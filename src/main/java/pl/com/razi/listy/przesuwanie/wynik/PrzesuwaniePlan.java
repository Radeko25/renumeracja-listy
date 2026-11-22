package pl.com.razi.listy.przesuwanie.wynik;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
	 * Dla pustego planu zwraca pusty tekst.
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
	 * WHERE (id_kategorii = 3) AND ((lp BETWEEN 5 AND 7) OR (lp BETWEEN 10 AND 12));
	 * </pre>
	 */
	public String toSqlCaseWhenBloki(String tabela, String kolumnaLp, String where) {
		return operacje.isEmpty() ? "" : sqlDocelowy(tabela, kolumnaLp, where, false) + ";";
	}

	/**
	 * Instrukcje do wykonania po kolei w jednej transakcji JDBC. Nazwy i warunek
	 * grupy są fragmentami SQL aplikacji. Warunek musi wskazywać tę samą listę
	 * również w trakcie zmiany LP. PostgreSQL i SQLite potrzebują wolnych
	 * ujemnych LP dostępnych do chwilowego zapisu przy zwykłym UNIQUE.
	 */
	public List<String> toSql(DialektSql dialekt, String tabela, String kolumnaLp, String where) {
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
		case ORACLE, SQL_SERVER -> List.of(sqlDocelowy(tabela, kolumnaLp, where, false));
		case POSTGRESQL, SQLITE -> List.of(
				"UPDATE " + tabela + "\nSET " + kolumnaLp + " = -" + kolumnaLp + filtr(kolumnaLp, where, false),
				sqlDocelowy(tabela, kolumnaLp, where, true));
		};
	}

	private String sqlDocelowy(String tabela, String lp, String where, boolean zUjemnych) {
		String wartosc = zUjemnych ? "-" + lp : lp;
		StringBuilder sql = new StringBuilder("UPDATE ").append(tabela).append("\nSET ").append(lp).append(" = CASE\n");
		for (PrzesuwanieOperacja op : operacje) {
			sql.append("    WHEN ").append(zakres(lp, op, zUjemnych)).append(" THEN ").append(wartosc)
					.append(op.offset > 0 ? " + " : " - ").append(Math.abs((long) op.offset)).append("\n");
		}
		return sql.append("    ELSE ").append(lp).append("\nEND").append(filtr(lp, where, zUjemnych)).toString();
	}

	private String filtr(String lp, String where, boolean zUjemnych) {
		StringBuilder sql = new StringBuilder("\nWHERE ");
		if (where != null && !where.isBlank()) {
			sql.append('(').append(where).append(") AND ");
		}
		dopiszZakresy(sql, lp, zUjemnych, 0, operacje.size());
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
