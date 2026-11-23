package pl.com.razi.listy.przesuwanie.wynik;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pl.com.razi.listy.przesuwanie.wynik.TabelaSql.Cel;

/**
 * Reprezentuje plan przesunięć powstały w wyniku operacji przemieszczenia
 * bloków elementów listy.
 * <p>
 * Plan składa się z jednej lub wielu operacji, z których każda opisuje spójny
 * zakres pozycji (LP) przesunięty o stały offset. Taka struktura jest
 * zoptymalizowana do późniejszej aktualizacji danych, np. w bazie SQL przy
 * użyciu instrukcji {@code CASE WHEN}.
 * <p>
 * Plan można zbudować ręcznie przez {@link #dodaj(int, int, int)}. Gotowy
 * {@link PrzesuwanieWynik} zachowuje jego kopię tylko do odczytu.
 */
public class PrzesuwaniePlan {

	public enum DialektSql {
		POSTGRESQL, SQL_SERVER, ORACLE, SQLITE
	}

	/**
	 * Pojedyncza operacja przesunięcia bloku.
	 * <p>
	 * Zawiera informacje:
	 * <ul>
	 * <li>{@code lpOd} – początek zakresu (włącznie),</li>
	 * <li>{@code lpDo} – koniec zakresu (włącznie),</li>
	 * <li>{@code offset} – przesunięcie o stałą wartość (ujemne lub dodatnie).</li>
	 * </ul>
	 */
	public static class PrzesuwanieOperacja {

		public final int lpOd;
		public final int lpDo;
		public final int offset;

		public PrzesuwanieOperacja(int lpOd, int lpDo, int offset) {
			this.lpOd = lpOd;
			this.lpDo = lpDo;
			this.offset = offset;
		}

	}

	private final List<PrzesuwanieOperacja> operacje;

	public PrzesuwaniePlan() {
		this.operacje = new ArrayList<>();
	}

	private PrzesuwaniePlan(List<PrzesuwanieOperacja> operacje) {
		this.operacje = List.copyOf(operacje);
	}

	PrzesuwaniePlan kopiaDoOdczytu() {
		return new PrzesuwaniePlan(operacje);
	}

	public void dodaj(int lpOd, int lpDo, int offset) {
		if (lpOd <= lpDo && offset != 0) {
			operacje.add(new PrzesuwanieOperacja(lpOd, lpDo, offset));
		}
	}

	public List<PrzesuwanieOperacja> getOperacje() {
		return operacje;
	}

	/**
	 * Generuje SQL w postaci instrukcji UPDATE z użyciem konstrukcji
	 * {@code CASE WHEN}, opisującej wszystkie przesunięcia bloków.
	 * Dla pustego planu zwraca pusty tekst. Ta sygnatura przyjmuje proste nazwy
	 * i warunek grupy opisany w {@link #toSql(DialektSql, String, String, String)}.
	 * <p>
	 * Przykład:
	 *
	 * <pre>
	 * UPDATE Dokumenty
	 * SET lp = CASE
	 *     WHEN lp BETWEEN 5 AND 7 THEN lp + 1
	 *     WHEN lp BETWEEN 10 AND 12 THEN lp - 2
	 *     ELSE lp
	 * END
	 * WHERE (Dokumenty.id_kategorii = 3) AND ((lp BETWEEN 5 AND 7) OR (lp BETWEEN 10 AND 12));
	 * </pre>
	 */
	public String toSqlCaseWhenBloki(String tabela, String kolumnaLp, String where) {
		return pojedynczyCase(TabelaSql.zProstegoZapisu(tabela, kolumnaLp, where));
	}

	/**
	 * Pojedynczy CASE dla schematów pozwalających na taki zapis. Zasady nazw i
	 * parametrów są takie jak w {@link #toSql(DialektSql, TabelaSql, String...)}.
	 */
	public String toSqlCaseWhenBloki(DialektSql dialekt, TabelaSql tabela, String... kolumnyGrupy) {
		return pojedynczyCase(tabela.celGrupy(dialekt, kolumnyGrupy));
	}

	/** Pojedynczy CASE dla tabeli, która w całości stanowi jedną listę. */
	public String toSqlCaseWhenBloki(DialektSql dialekt, TabelaSql tabela) {
		return pojedynczyCase(tabela.cel(dialekt));
	}

	private String pojedynczyCase(Cel cel) {
		return operacje.isEmpty() ? "" : sqlDocelowy(cel, false) + ";";
	}

	/**
	 * Wariant zgodności dla prostych niecytowanych nazw i filtra aplikacji.
	 * Obsługuje pusty filtr, równość kolumny z liczbą całkowitą oraz równości
	 * z parametrami JDBC połączone przez AND lub OR. Odrzuca pozostałe wyrażenia.
	 * Dla nazw z definicji użytkownika użyj przeciążenia z {@link TabelaSql}.
	 */
	public List<String> toSql(DialektSql dialekt, String tabela, String kolumnaLp, String where) {
		return sqlDlaDialektu(dialekt, TabelaSql.zProstegoZapisu(tabela, kolumnaLp, where));
	}

	/**
	 * Instrukcje do wykonania po kolei w jednej transakcji JDBC. Każda kolumna
	 * grupy tworzy równość z jednym parametrem JDBC. Równości łączy AND.
	 * W każdej instrukcji powiąż te same wartości w kolejności podanych kolumn.
	 * Pusta tablica kolumn grupy jest błędem.
	 * W PostgreSQL i SQLite tymczasowe ujemne LP muszą być dostępne do zapisu.
	 */
	public List<String> toSql(DialektSql dialekt, TabelaSql tabela, String... kolumnyGrupy) {
		return sqlDlaDialektu(dialekt, tabela.celGrupy(dialekt, kolumnyGrupy));
	}

	/** Zapis dla tabeli, która w całości stanowi jedną listę. */
	public List<String> toSql(DialektSql dialekt, TabelaSql tabela) {
		return sqlDlaDialektu(dialekt, tabela.cel(dialekt));
	}

	private List<String> sqlDlaDialektu(DialektSql dialekt, Cel cel) {
		Objects.requireNonNull(dialekt, "dialekt");
		if (operacje.isEmpty()) {
			return List.of();
		}
		for (PrzesuwanieOperacja op : operacje) {
			if (op.lpOd <= 0 || (long) op.lpOd + op.offset <= 0 || (long) op.lpDo + op.offset > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("Źródłowe i docelowe LP muszą być dodatnimi liczbami typu int.");
			}
		}
		return switch (dialekt) {
		case ORACLE, SQL_SERVER -> List.of(sqlDocelowy(cel, false));
		case POSTGRESQL, SQLITE -> List.of(
				"UPDATE " + cel.tabela() + "\nSET " + cel.lp() + " = -" + cel.lp() + filtr(cel, false),
				sqlDocelowy(cel, true));
		};
	}

	private String sqlDocelowy(Cel cel, boolean zUjemnych) {
		String lp = cel.lp();
		String wartosc = zUjemnych ? "-" + lp : lp;
		StringBuilder sql = new StringBuilder("UPDATE ").append(cel.tabela()).append("\nSET ").append(lp).append(" = CASE\n");
		for (PrzesuwanieOperacja op : operacje) {
			sql.append("    WHEN ").append(zakres(lp, op, zUjemnych)).append(" THEN ").append(wartosc)
					.append(op.offset > 0 ? " + " : " - ").append(Math.abs((long) op.offset)).append("\n");
		}
		return sql.append("    ELSE ").append(lp).append("\nEND").append(filtr(cel, zUjemnych)).toString();
	}

	private String filtr(Cel cel, boolean zUjemnych) {
		StringBuilder sql = new StringBuilder("\nWHERE ");
		if (!cel.warunek().isEmpty()) {
			sql.append('(').append(cel.warunek()).append(") AND ");
		}
		dopiszZakresy(sql, cel.lp(), zUjemnych, 0, operacje.size());
		return sql.toString();
	}

	// Równoważenie OR zapobiega przekroczeniu głębokości wyrażeń SQLite dla rzadkich widoków.
	private void dopiszZakresy(StringBuilder sql, String lp, boolean zUjemnych, int od, int koniec) {
		sql.append('(');
		if (koniec - od == 1) {
			sql.append(zakres(lp, operacje.get(od), zUjemnych));
		} else {
			int srodek = od + (koniec - od) / 2;
			dopiszZakresy(sql, lp, zUjemnych, od, srodek);
			sql.append(" OR ");
			dopiszZakresy(sql, lp, zUjemnych, srodek, koniec);
		}
		sql.append(')');
	}

	private static String zakres(String lp, PrzesuwanieOperacja op, boolean zUjemnych) {
		long od = zUjemnych ? -(long) op.lpDo : op.lpOd;
		long doLp = zUjemnych ? -(long) op.lpOd : op.lpDo;
		return lp + " BETWEEN " + od + " AND " + doLp;
	}
}
