package pl.com.razi.listy.przesuwanie.wynik;

import java.util.ArrayList;
import java.util.List;

/**
 * Reprezentuje plan przesunięć powstały w wyniku operacji przemieszczenia
 * bloków elementów listy.
 * <p>
 * Plan składa się z jednej lub wielu operacji, z których każda opisuje spójny
 * zakres pozycji (LP) przesunięty o stały offset. Taka struktura jest
 * zoptymalizowana do późniejszej aktualizacji danych, np. w bazie SQL przy
 * użyciu instrukcji {@code CASE WHEN}.
 * <p>
 * Klasa jest niemutowalna w części — lista operacji jest finalna, ale może
 * zostać uzupełniona przez wywołania metody {@link #dodaj(int, int, int)}.
 */
public class PrzesuwaniePlan {

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

	private final List<PrzesuwanieOperacja> operacje = new ArrayList<>();

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
	 * WHERE id_kategorii = 3;
	 * </pre>
	 */
	public String toSqlCaseWhenBloki(String tabela, String kolumnaLp, String where) {
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

}
