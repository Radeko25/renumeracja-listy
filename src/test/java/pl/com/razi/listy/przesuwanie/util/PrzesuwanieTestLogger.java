package pl.com.razi.listy.przesuwanie.util;

import java.util.List;

import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan;

public class PrzesuwanieTestLogger {

	private final boolean debug;

	public PrzesuwanieTestLogger(boolean debug) {
		this.debug = debug;
	}

	public boolean isDebug() {
		return debug;
	}

	/** Jeśli tryb <b>debug</b> - wypisuje pojedynczą linię tekstu. */
	public void log(String msg) {
		if (debug) {
			System.out.println(msg);
		}
	}

	/** Wypisuje pustą linię, jeśli debug=true */
	/** Jeśli tryb <b>debug</b> - wypisuje pustą linię. */
	public void logEmpty() {
		if (debug) {
			System.out.println();
		}
	}

	/**
	 * Jeśli tryb <b>debug</b> - wypisuje poprzedzoną <i>prefiksem</i> listę
	 * elementów wg kolejności na liście w formacie:
	 * 
	 * <pre>
	 * "<i>prefiks</i>[1, 4, 5, 9, 10]".
	 * </pre>
	 */
	public void logLista(String prefiks, List<PrzesuwanieTestDane> lista) {
		if (!isDebug()) {
			return;
		}

		prefiks = prefiks == null ? "" : prefiks;

		if (lista == null) {
			System.out.println(prefiks + "[null]");
			return;
		}

		String wynik = lista.stream().map(e -> String.valueOf(e.getId())).reduce((a, b) -> a + ", " + b).orElse("");

		System.out.println(prefiks + "[" + wynik + "]");
	}

	/**
	 * Jeśli tryb <b>debug</b> – wypisuje dane przesunięcia (plan operacji) w
	 * czytelnej formie. Każda operacja wyświetlana jest jako:
	 * 
	 * <pre>
	 *   • [lpOd–lpDo] offset = ±X
	 * </pre>
	 * 
	 * Gdy plan jest pusty, wypisywana jest informacja:
	 * <code>(brak operacji)</code>.
	 */
    public void logDanePrzesuniecia(PrzesuwaniePlan plan) {
        if (!isDebug()) {
            return;
        }

        if (plan == null) {
            System.out.println("[null]");
            return;
        }

        List<PrzesuwaniePlan.PrzesuwanieOperacja> operacje = plan.getOperacje();

        if (operacje.isEmpty()) {
            System.out.println("(brak operacji)");
            return;
        }

        for (PrzesuwaniePlan.PrzesuwanieOperacja o : operacje) {

            String offsetStr = (o.offset > 0 ? "+" : "") + o.offset;

			System.out.println("  • [" + o.lpOd + "–" + o.lpDo + "] offset = " + offsetStr);
		}
	}

}
