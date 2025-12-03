package pl.com.razi.listy.przesuwanie.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;
import java.util.List;

import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan;

public class PrzesuwanieAssertions {

	private final PrzesuwanieTestLogger logger;

	public PrzesuwanieAssertions(PrzesuwanieTestLogger testLogger) {
		this.logger = testLogger;
	}

	/**
	 * Sprawdza, czy kolejność elementów w liście wynikowej jest zgodna z
	 * oczekiwaną. W trybie debug wypisuje opis oraz obie listy.
	 */
	public void assertLista(List<PrzesuwanieTestDane> wynik, List<PrzesuwanieTestDane> oczekiwania) {

		logger.logLista("Oczekiwane:  ", oczekiwania);
		logger.logLista("Rzeczywiste: ", wynik);

		if (wynik == null && oczekiwania == null) {
			return;
		}

		if (wynik == null) {
			throw new AssertionError("Lista wynikowa jest null, a oczekiwana nie jest.");
		}

		if (oczekiwania == null) {
			throw new AssertionError("Lista oczekiwana jest null, a wynikowa nie jest.");
		}

		if (wynik.size() != oczekiwania.size()) {
			throw new AssertionError("Rozmiary list się różnią.");
		}

		List<Integer> idWynik = wynik.stream().map(PrzesuwanieTestDane::getId).toList();

		List<Integer> idOczekiwane = oczekiwania.stream().map(PrzesuwanieTestDane::getId).toList();

		assertEquals(idOczekiwane, idWynik, "Niepoprawna kolejność.");
	}

	public void assertDanePrzesuniecia(PrzesuwaniePlan wynik, PrzesuwaniePlan oczekiwania) {

		logger.log("Plan oczekiwany:");
		logger.logDanePrzesuniecia(oczekiwania);

		logger.log("Plan rzeczywisty:");
		logger.logDanePrzesuniecia(wynik);

		if (wynik == null && oczekiwania == null) {
			return;
		}

		if (wynik == null) {
			throw new AssertionError("Plan wynikowy jest null, oczekiwany nie.");
		}

		if (oczekiwania == null) {
			throw new AssertionError("Plan oczekiwany jest null, wynikowy nie.");
		}

		List<PrzesuwaniePlan.PrzesuwanieOperacja> opO = oczekiwania.getOperacje();
		List<PrzesuwaniePlan.PrzesuwanieOperacja> opW = wynik.getOperacje();

		Comparator<PrzesuwaniePlan.PrzesuwanieOperacja> cmp = Comparator.comparingInt(o -> o.lpOd);

		opO = opO.stream().sorted(cmp).toList();
		opW = opW.stream().sorted(cmp).toList();

		assertEquals(opO.size(), opW.size(), "Niezgodna liczba operacji przesunięcia.");

		for (int i = 0; i < opO.size(); i++) {

			PrzesuwaniePlan.PrzesuwanieOperacja ocz = opO.get(i);
			PrzesuwaniePlan.PrzesuwanieOperacja win = opW.get(i);

			assertEquals(ocz.lpOd, win.lpOd, "Operacja " + i + ": lpOd niezgodne");

			assertEquals(ocz.lpDo, win.lpDo, "Operacja " + i + ": lpDo niezgodne");

			assertEquals(ocz.offset, win.offset, "Operacja " + i + ": offset niezgodny");
		}
	}

}
