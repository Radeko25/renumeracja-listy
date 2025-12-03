package pl.com.razi.listy.przesuwanie;

import static pl.com.razi.listy.przesuwanie.util.PrzesuwanieFactory.elementy;
import static pl.com.razi.listy.przesuwanie.util.PrzesuwanieFactory.operacja;
import static pl.com.razi.listy.przesuwanie.util.PrzesuwanieFactory.plan;

import java.util.List;

import pl.com.razi.listy.przesuwanie.PrzesuwanieObsluga.TrybPrzesuwania;
import pl.com.razi.listy.przesuwanie.util.PrzesuwanieTestDane;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan;

public class PrzesuwanieDociskajaceTest extends PrzesuwanieObslugaTest {

	@Override
	protected TrybPrzesuwania pobierzTryb() {
		return TrybPrzesuwania.DOCISKAJACE;
	}

	@Override
	protected List<PrzesuwanieTestDane> getOczekiwaneLista(Scenariusz scenariusz) {
		switch (scenariusz) {

		// W górę o 1
		case GORA_O_JEDEN_INDEKS_1:
			return elementy(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

		case GORA_O_JEDEN_INDEKS_5:
			return elementy(1, 2, 3, 5, 4, 6, 7, 8, 9, 10);

		case GORA_O_JEDEN_INDEKSY_4_5:
			return elementy(1, 2, 4, 5, 3, 6, 7, 8, 9, 10);

		case GORA_O_JEDEN_INDEKSY_1_3:
			return elementy(1, 3, 2, 4, 5, 6, 7, 8, 9, 10);

		case GORA_O_JEDEN_INDEKSY_1_2:
			return elementy(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

		case GORA_O_JEDEN_INDEKSY_1_3_4_7_8:
			return elementy(1, 3, 4, 2, 5, 7, 8, 6, 9, 10);

		// W górę o 3
		case GORA_O_TRZY_INDEKSY_1_2:
			return elementy(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

		case GORA_O_TRZY_INDEKSY_1_3:
			return elementy(1, 3, 2, 4, 5, 6, 7, 8, 9, 10);

		case GORA_O_TRZY_INDEKSY_5_6:
			return elementy(1, 5, 6, 2, 3, 4, 7, 8, 9, 10);

		case GORA_O_TRZY_INDEKSY_1_3_4_7_8:
			return elementy(1, 3, 4, 7, 8, 2, 5, 6, 9, 10);

		// W dół o 1
		case DOL_O_JEDEN_INDEKS_10:
			return elementy(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

		case DOL_O_JEDEN_INDEKS_5:
			return elementy(1, 2, 3, 4, 6, 5, 7, 8, 9, 10);

		case DOL_O_JEDEN_INDEKSY_4_5:
			return elementy(1, 2, 3, 6, 4, 5, 7, 8, 9, 10);

		case DOL_O_JEDEN_INDEKSY_8_10:
			return elementy(1, 2, 3, 4, 5, 6, 7, 9, 8, 10);

		case DOL_O_JEDEN_INDEKSY_9_10:
			return elementy(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

		case DOL_O_JEDEN_INDEKSY_1_3_4_7_8:
			return elementy(2, 1, 5, 3, 4, 6, 9, 7, 8, 10);

		// W dół o 3
		case DOL_O_TRZY_INDEKSY_9_10:
			return elementy(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

		case DOL_O_TRZY_INDEKSY_8_10:
			return elementy(1, 2, 3, 4, 5, 6, 7, 9, 8, 10);

		case DOL_O_TRZY_INDEKSY_5_6:
			return elementy(1, 2, 3, 4, 7, 8, 9, 5, 6, 10);

		case DOL_O_TRZY_INDEKSY_1_3_4_7_8:
			return elementy(2, 5, 6, 1, 9, 3, 4, 10, 7, 8);

		default:
			throw new IllegalArgumentException("Brak oczekiwań dla scenariusza: " + scenariusz);
		}
	}

	@Override
	protected PrzesuwaniePlan getOczekiwanePlan(Scenariusz scenariusz) {
		switch (scenariusz) {

		// W górę o 1
		case GORA_O_JEDEN_INDEKS_1:
			return plan();

		case GORA_O_JEDEN_INDEKS_5:
			return plan(operacja(4, 4, 1), operacja(5, 5, -1));

		case GORA_O_JEDEN_INDEKSY_4_5:
			return plan(operacja(3, 3, 2), operacja(4, 5, -1));

		case GORA_O_JEDEN_INDEKSY_1_3:
			return plan(operacja(2, 2, 1), operacja(3, 3, -1));

		case GORA_O_JEDEN_INDEKSY_1_2:
			return plan();

		case GORA_O_JEDEN_INDEKSY_1_3_4_7_8:
			return plan(operacja(2, 2, 2), operacja(3, 4, -1), operacja(6, 6, 2), operacja(7, 8, -1));

		// W górę o 3
		case GORA_O_TRZY_INDEKSY_1_2:
			return plan();

		case GORA_O_TRZY_INDEKSY_1_3:
			return plan(operacja(2, 2, 1), operacja(3, 3, -1));

		case GORA_O_TRZY_INDEKSY_5_6:
			return plan(operacja(2, 4, 2), operacja(5, 6, -3));

		case GORA_O_TRZY_INDEKSY_1_3_4_7_8:
			return plan(operacja(2, 2, 4), operacja(3, 4, -1), operacja(5, 6, 2), operacja(7, 8, -3));

		// W dół o 1
		case DOL_O_JEDEN_INDEKS_10:
			return plan();

		case DOL_O_JEDEN_INDEKS_5:
			return plan(operacja(5, 5, 1), operacja(6, 6, -1));

		case DOL_O_JEDEN_INDEKSY_4_5:
			return plan(operacja(4, 5, 1), operacja(6, 6, -2));

		case DOL_O_JEDEN_INDEKSY_8_10:
			return plan(operacja(8, 8, 1), operacja(9, 9, -1));

		case DOL_O_JEDEN_INDEKSY_9_10:
			return plan();

		case DOL_O_JEDEN_INDEKSY_1_3_4_7_8:
			return plan(operacja(1, 1, 1), operacja(2, 2, -1), operacja(3, 4, 1), operacja(5, 5, -2), operacja(7, 8, 1),
					operacja(9, 9, -2));

		// W dół o 3
		case DOL_O_TRZY_INDEKSY_9_10:
			return plan();

		case DOL_O_TRZY_INDEKSY_8_10:
			return plan(operacja(8, 8, 1), operacja(9, 9, -1));

		case DOL_O_TRZY_INDEKSY_5_6:
			return plan(operacja(5, 6, 3), operacja(7, 9, -2));

		case DOL_O_TRZY_INDEKSY_1_3_4_7_8:
			return plan(operacja(1, 1, 3), operacja(2, 2, -1), operacja(3, 4, 3), operacja(5, 6, -3), operacja(7, 8, 2),
					operacja(9, 9, -4), operacja(10, 10, -2));

		default:
			throw new IllegalArgumentException("Brak oczekiwań dla scenariusza: " + scenariusz);
		}
	}

}
