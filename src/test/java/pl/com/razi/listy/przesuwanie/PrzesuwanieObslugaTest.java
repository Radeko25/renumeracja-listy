package pl.com.razi.listy.przesuwanie;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.com.razi.listy.przesuwanie.PrzesuwanieObliczenia.Kierunek;
import pl.com.razi.listy.przesuwanie.PrzesuwanieObsluga.TrybPrzesuwania;
import pl.com.razi.listy.przesuwanie.util.PrzesuwanieAssertions;
import pl.com.razi.listy.przesuwanie.util.PrzesuwanieFactory;
import pl.com.razi.listy.przesuwanie.util.PrzesuwanieTestDane;
import pl.com.razi.listy.przesuwanie.util.PrzesuwanieTestLogger;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwanieWynik;

abstract class PrzesuwanieObslugaTest {

	private static final boolean DEBUG = true;

	private PrzesuwanieTestLogger logger;
	private PrzesuwanieAssertions assertions;

	private PrzesuwanieObsluga<PrzesuwanieTestDane> obsluga;

	private final List<PrzesuwanieTestDane> listaTestowa = PrzesuwanieFactory.listaPelna(10);

	protected abstract TrybPrzesuwania pobierzTryb();

	/**
	 * Każda klasa dziedzicząca zwraca oczekiwaną kolejność dla wybranego
	 * scenariusza testowego.
	 */
	protected abstract List<PrzesuwanieTestDane> getOczekiwaneLista(Scenariusz scenariusz);

	/**
	 * Każda klasa dziedzicząca zwraca oczekiwany plan aktualizacji dla wybranego
	 * scenariusza testowego.
	 */
	protected abstract PrzesuwaniePlan getOczekiwanePlan(Scenariusz scenariusz);

	@BeforeEach
	void setup() {
		this.logger = new PrzesuwanieTestLogger(DEBUG);
		this.assertions = new PrzesuwanieAssertions(logger);
		this.obsluga = new PrzesuwanieObsluga.Builder<PrzesuwanieTestDane>().trybPrzesuwania(pobierzTryb()).build();
	}

	// ====================================================================
	// SCENARIUSZE TESTOWE
	// ====================================================================

	protected enum Scenariusz {

		// W górę o 1
		GORA_O_JEDEN_INDEKS_1(1, Kierunek.GORA, 1),
		GORA_O_JEDEN_INDEKS_5(1, Kierunek.GORA, 5),
		GORA_O_JEDEN_INDEKSY_4_5(1, Kierunek.GORA, 4, 5),
		GORA_O_JEDEN_INDEKSY_1_3(1, Kierunek.GORA, 1, 3),
		GORA_O_JEDEN_INDEKSY_1_2(1, Kierunek.GORA, 1, 2),
		GORA_O_JEDEN_INDEKSY_1_3_4_7_8(1, Kierunek.GORA, 1, 3, 4, 7, 8),

		// W górę o 3
		GORA_O_TRZY_INDEKSY_1_2(3, Kierunek.GORA, 1, 2),
		GORA_O_TRZY_INDEKSY_1_3(3, Kierunek.GORA, 1, 3),
		GORA_O_TRZY_INDEKSY_5_6(3, Kierunek.GORA, 5, 6),
		GORA_O_TRZY_INDEKSY_1_3_4_7_8(3, Kierunek.GORA, 1, 3, 4, 7, 8),

		// W dół o 1
		DOL_O_JEDEN_INDEKS_10(1, Kierunek.DOL, 10),
		DOL_O_JEDEN_INDEKS_5(1, Kierunek.DOL, 5),
		DOL_O_JEDEN_INDEKSY_4_5(1, Kierunek.DOL, 4, 5),
		DOL_O_JEDEN_INDEKSY_8_10(1, Kierunek.DOL, 8, 10),
		DOL_O_JEDEN_INDEKSY_9_10(1, Kierunek.DOL, 9, 10),
		DOL_O_JEDEN_INDEKSY_1_3_4_7_8(1, Kierunek.DOL, 1, 3, 4, 7, 8),

		// W dół o 3
		DOL_O_TRZY_INDEKSY_9_10(3, Kierunek.DOL, 9, 10),
		DOL_O_TRZY_INDEKSY_8_10(3, Kierunek.DOL, 8, 10),
		DOL_O_TRZY_INDEKSY_5_6(3, Kierunek.DOL, 5, 6),
		DOL_O_TRZY_INDEKSY_1_3_4_7_8(3, Kierunek.DOL, 1, 3, 4, 7, 8);
		
		private final int przesuniecie;
		private final Kierunek kierunek;
		private final int[] lpWybranych;

		private Scenariusz(int przesuniecie, Kierunek kierunek, int... lpWybranych) {
			this.przesuniecie = przesuniecie;
			this.kierunek = kierunek;
			this.lpWybranych = lpWybranych;
		}

		public int getPrzesuniecie() {
			return kierunek == Kierunek.GORA ? -przesuniecie : przesuniecie;
		}

		public List<PrzesuwanieTestDane> wybrane() {
			return PrzesuwanieFactory.elementy(lpWybranych);
		}

		public String opis() {
			String lp = Arrays.toString(lpWybranych);
			return kierunek.name() + " — o " + przesuniecie + " pozycje, wybrane " + lp;
		}

	}

	// ====================================================================
	// W GÓRĘ O 1
	// ====================================================================

	@Test
	final void test_gora_o_jeden_indeks_1() throws Exception {
		wykonaj(Scenariusz.GORA_O_JEDEN_INDEKS_1);
	}

	@Test
	final void test_gora_o_jeden_indeks_5() throws Exception {
		wykonaj(Scenariusz.GORA_O_JEDEN_INDEKS_5);
	}

	@Test
	final void test_gora_o_jeden_indeksy_4_5() throws Exception {
		wykonaj(Scenariusz.GORA_O_JEDEN_INDEKSY_4_5);
	}

	@Test
	final void test_gora_o_jeden_indeksy_1_3() throws Exception {
		wykonaj(Scenariusz.GORA_O_JEDEN_INDEKSY_1_3);
	}

	@Test
	final void test_gora_o_jeden_indeksy_1_2() throws Exception {
		wykonaj(Scenariusz.GORA_O_JEDEN_INDEKSY_1_2);
	}

	@Test
	final void test_gora_o_jeden_indeksy_1_3_4_7_8() throws Exception {
		wykonaj(Scenariusz.GORA_O_JEDEN_INDEKSY_1_3_4_7_8);
	}

	// ====================================================================
	// W GÓRĘ O 3
	// ====================================================================

	@Test
	final void test_gora_o_trzy_indeksy_1_2() throws Exception {
		wykonaj(Scenariusz.GORA_O_TRZY_INDEKSY_1_2);
	}

	@Test
	final void test_gora_o_trzy_indeksy_1_3() throws Exception {
		wykonaj(Scenariusz.GORA_O_TRZY_INDEKSY_1_3);
	}

	@Test
	final void test_gora_o_trzy_indeksy_5_6() throws Exception {
		wykonaj(Scenariusz.GORA_O_TRZY_INDEKSY_5_6);
	}

	@Test
	final void test_gora_o_trzy_indeksy_1_3_4_7_8() throws Exception {
		wykonaj(Scenariusz.GORA_O_TRZY_INDEKSY_1_3_4_7_8);
	}

	// ====================================================================
	// W DÓŁ O 1
	// ====================================================================

	@Test
	final void test_dol_o_jeden_indeks_10() throws Exception {
		wykonaj(Scenariusz.DOL_O_JEDEN_INDEKS_10);
	}

	@Test
	final void test_dol_o_jeden_indeks_5() throws Exception {
		wykonaj(Scenariusz.DOL_O_JEDEN_INDEKS_5);
	}

	@Test
	final void test_dol_o_jeden_indeksy_4_5() throws Exception {
		wykonaj(Scenariusz.DOL_O_JEDEN_INDEKSY_4_5);
	}

	@Test
	final void test_dol_o_jeden_indeksy_8_10() throws Exception {
		wykonaj(Scenariusz.DOL_O_JEDEN_INDEKSY_8_10);
	}

	@Test
	final void test_dol_o_jeden_indeksy_9_10() throws Exception {
		wykonaj(Scenariusz.DOL_O_JEDEN_INDEKSY_9_10);
	}

	@Test
	final void test_dol_o_jeden_indeksy_1_3_4_7_8() throws Exception {
		wykonaj(Scenariusz.DOL_O_JEDEN_INDEKSY_1_3_4_7_8);
	}

	// ====================================================================
	// W DÓŁ O 3
	// ====================================================================

	@Test
	final void test_dol_o_trzy_indeksy_9_10() throws Exception {
		wykonaj(Scenariusz.DOL_O_TRZY_INDEKSY_9_10);
	}

	@Test
	final void test_dol_o_trzy_indeksy_8_10() throws Exception {
		wykonaj(Scenariusz.DOL_O_TRZY_INDEKSY_8_10);
	}

	@Test
	final void test_dol_o_trzy_indeksy_5_6() throws Exception {
		wykonaj(Scenariusz.DOL_O_TRZY_INDEKSY_5_6);
	}

	@Test
	final void test_dol_o_trzy_indeksy_1_3_4_7_8() throws Exception {
		wykonaj(Scenariusz.DOL_O_TRZY_INDEKSY_1_3_4_7_8);
	}

	// ====================================================================
	// TEST
	// ====================================================================

	private void wykonaj(Scenariusz scenariusz) throws Exception {

		List<PrzesuwanieTestDane> wybrane = scenariusz.wybrane();
		List<PrzesuwanieTestDane> wszystkie = listaTestowa;

		logger.log(scenariusz.opis());
		logger.logLista("Początkowa: ", wszystkie);

		PrzesuwanieWynik<PrzesuwanieTestDane> wynik = obsluga.przesunPelny(wszystkie, wybrane,
				scenariusz.getPrzesuniecie());

		logger.logEmpty();
		logger.log("Sprawdzenie wynikowej listy:");
		assertions.assertLista(wynik.getLista(), getOczekiwaneLista(scenariusz));

		logger.logEmpty();
		logger.log("Sprawdzenie danych przesunięcia:");
		assertions.assertDanePrzesuniecia(wynik.getPlan(), getOczekiwanePlan(scenariusz));
	}

}
