package pl.com.razi.listy.przesuwanie.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pl.com.razi.listy.przesuwanie.model.PrzesuwanieIndeksyBlok;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan.PrzesuwanieOperacja;

public class PrzesuwanieFactory {

	private PrzesuwanieFactory() {
		throw new AssertionError();
	}

	public static PrzesuwanieIndeksyBlok blok(int start, int end) {
		return new PrzesuwanieIndeksyBlok(start, end);
	}

	/**
	 * Tworzy listę elementów od 1 do n.
	 */
	public static List<PrzesuwanieTestDane> listaPelna(int n) {
		List<PrzesuwanieTestDane> wynik = new ArrayList<>();
		for (int i = 1; i <= n; i++) {
			wynik.add(new PrzesuwanieTestDane(i));
		}
		return wynik;
	}

	/**
	 * Tworzy listę elementów na podstawie przekazanych identyfikatorów (ID/LP), w
	 * dokładnie takiej kolejności, jak podano.
	 */
	public static List<PrzesuwanieTestDane> elementy(int... lp) {
		List<PrzesuwanieTestDane> wynik = new ArrayList<>();
		for (int x : lp) {
			wynik.add(new PrzesuwanieTestDane(x));
		}
		return wynik;
	}

	/**
     * Tworzy jedną operację przesunięcia do użytku testowego.
     */
	public static PrzesuwanieOperacja operacja(int lpOd, int lpDo, int offset) {
		return new PrzesuwanieOperacja(lpOd, lpDo, offset);
    }

	/**
	 * Tworzy plan przesunięcia zawierający podane operacje. Jeśli lista operacji
	 * jest pusta, zwracany jest pusty plan.
	 */
	public static PrzesuwaniePlan plan(PrzesuwanieOperacja... operacje) {
		PrzesuwaniePlan plan = new PrzesuwaniePlan();
		Arrays.stream(operacje).forEach(o -> plan.dodaj(o.lpOd, o.lpDo, o.offset));
		return plan;
	}

}
