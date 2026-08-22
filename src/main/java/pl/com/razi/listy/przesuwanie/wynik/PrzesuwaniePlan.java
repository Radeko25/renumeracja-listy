package pl.com.razi.listy.przesuwanie.wynik;

import java.util.ArrayList;
import java.util.List;

/**
 * Reprezentuje skompresowany plan zmian pozycji (LP).
 * <p>
 * Każda operacja opisuje spójny zakres starych LP przesuwany o jeden stały
 * offset. Plan generowany przez mechanizm przesuwania zawiera maksymalne takie
 * zakresy, a więc minimalną liczbę operacji w tym modelu reprezentacji.
 */
public class PrzesuwaniePlan {

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

	private final List<PrzesuwanieOperacja> operacje = new ArrayList<>();

	public void dodaj(int lpOd, int lpDo, int offset) {
		if (lpOd <= lpDo && offset != 0) {
			operacje.add(new PrzesuwanieOperacja(lpOd, lpDo, offset));
		}
	}

	/**
	 * Zwraca niemodyfikowalny snapshot operacji planu.
	 */
	public List<PrzesuwanieOperacja> getOperacje() {
		return List.copyOf(operacje);
	}

	public boolean isPusty() {
		return operacje.isEmpty();
	}

	/**
	 * Generuje SQL UPDATE z użyciem CASE WHEN.
	 * <p>
	 * Jeśli plan jest pusty, zwracany jest pusty String zamiast niepoprawnego SQL-a
	 * bez żadnej gałęzi WHEN.
	 * <p>
	 * Parametr {@code where} jest surowym fragmentem SQL i powinien pochodzić z
	 * zaufanego źródła. Metoda nie wykonuje escapowania identyfikatorów ani warunku.
	 */
	public String toSqlCaseWhenBloki(String tabela, String kolumnaLp, String where) {
		if (operacje.isEmpty()) {
			return "";
		}

		wymagajNiepustejNazwy(tabela, "tabela");
		wymagajNiepustejNazwy(kolumnaLp, "kolumnaLp");

		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE ").append(tabela).append("\n");
		sb.append("SET ").append(kolumnaLp).append(" = CASE\n");

		for (PrzesuwanieOperacja op : operacje) {
			sb.append("    WHEN ").append(kolumnaLp).append(" BETWEEN ").append(op.lpOd).append(" AND ").append(op.lpDo)
					.append(" THEN ").append(kolumnaLp);

			if (op.offset > 0) {
				sb.append(" + ").append(op.offset);
			} else {
				sb.append(" - ").append(-op.offset);
			}
			sb.append("\n");
		}

		sb.append("    ELSE ").append(kolumnaLp).append("\n");
		sb.append("END");

		if (where != null && !where.isBlank()) {
			sb.append("\nWHERE ").append(where);
		}
		sb.append(";");

		return sb.toString();
	}

	private static void wymagajNiepustejNazwy(String wartosc, String nazwaParametru) {
		if (wartosc == null || wartosc.isBlank()) {
			throw new IllegalArgumentException("Parametr '" + nazwaParametru + "' nie może być pusty.");
		}
	}
}
