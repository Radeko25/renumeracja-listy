package pl.com.razi.listy.przesuwanie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pl.com.razi.listy.przesuwanie.PrzesuwanieObsluga.TrybPrzesuwania;
import pl.com.razi.listy.przesuwanie.model.PrzesuwanieIndeksyBlok;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan;

/**
 * Zestaw niskopoziomowych obliczeń wykonywanych podczas przesuwania bloków
 * elementów na liście. Klasa nie wykonuje żadnej walidacji – zakłada pełną
 * poprawność i spójność danych wejściowych.
 * <p>
 * Wszystkie metody operują wyłącznie na indeksach i strukturach pomocniczych,
 * przyjmując uprzednio zweryfikowane bloki oraz wartości przesunięć.
 * <p>
 * Klasa jest wewnętrzną częścią mechanizmu przesuwania i nie powinna być
 * używana bezpośrednio z zewnątrz – odpowiednia walidacja danych realizowana
 * jest w {@link PrzesuwanieWejscie}.
 */
class PrzesuwanieObliczenia {

	private static final String NIEOBSŁUGIWANY_TRYB_PRZESUWANIA = "Nieobsługiwany tryb przesuwania: ";

	private PrzesuwanieObliczenia() {
		throw new AssertionError();
	}

	enum Kierunek {
		GORA, DOL
	}

	private static Kierunek okreslKierunek(long przesuniecie) {
		return przesuniecie < 0 ? Kierunek.GORA : Kierunek.DOL;
	}

	/**
	 * Określa, czy przy danym trybie, kierunku i blokach dojdzie do przesunięcia
	 * choćby jednego elementu listy.
	 * <p>
	 * Zakłada pełną poprawność danych wejściowych.
	 */
	public static <T> boolean czyDoPrzesunieciaDojdzie(final TrybPrzesuwania trybPrzesuwania,
			final List<PrzesuwanieIndeksyBlok> blokiWybranych, final int rozmiarListy, final long przesuniecie) {

		if (blokiWybranych.isEmpty() || przesuniecie == 0) {
			return false;
		}

		final Kierunek kierunek = okreslKierunek(przesuniecie);

		final int startListy = 1;
		final int koniecListy = rozmiarListy;

		final PrzesuwanieIndeksyBlok blokGraniczny = (kierunek == Kierunek.GORA) ? blokiWybranych.get(0)
				: blokiWybranych.get(blokiWybranych.size() - 1);

		switch (trybPrzesuwania) {

		case CYKLICZNE:
			return true;

		case LINIOWE:
			return kierunek == Kierunek.GORA ? blokGraniczny.start + przesuniecie >= startListy
					: blokGraniczny.end + przesuniecie <= koniecListy;

		case DOCISKAJACE:
			if (blokiWybranych.size() > 1) {
				return true;
			} else {
				return kierunek == Kierunek.GORA ? blokGraniczny.start > startListy
						: blokGraniczny.end < koniecListy;
			}

		default:
			throw new AssertionError(NIEOBSŁUGIWANY_TRYB_PRZESUWANIA + trybPrzesuwania);
		}
	}

	/**
	 * Oblicza docelową permutację indeksów po dokonaniu przesunięcia bloków
	 * elementów.
	 * <p>
	 * Wynikowa tablica zawiera dla każdego starego indeksu nowy indeks, który dany
	 * element powinien przyjąć po przesunięciu.
	 * <p>
	 * Zakłada pełną poprawność danych wejściowych.
	 */
	static int[] obliczDocelowaPermutacje(final TrybPrzesuwania trybPrzesuwania,
			final List<PrzesuwanieIndeksyBlok> blokiWybranych, final int rozmiarListy, final long przesuniecie) {

		final int[] docelowaPermutacja = new int[rozmiarListy];
		final boolean[] zajeteLp = new boolean[rozmiarListy + 1];

		final Kierunek kierunek = okreslKierunek(przesuniecie);

		long limitGora = 1;
		long limitDol = rozmiarListy;

		// -----------------------------------------------------
		// 1. Przesuwanie bloków
		// -----------------------------------------------------

		boolean reverse = (trybPrzesuwania == TrybPrzesuwania.DOCISKAJACE && kierunek == Kierunek.DOL);
		int startIndex  = reverse ? blokiWybranych.size() - 1 : 0;
		int endIndex    = reverse ? -1 : blokiWybranych.size();
		int step        = reverse ? -1 : 1;

		for (int i = startIndex; i != endIndex; i += step) {

			PrzesuwanieIndeksyBlok blok = blokiWybranych.get(i);

			final int dlugosc = blok.end - blok.start + 1;
			long start = blok.start + przesuniecie;

			switch (trybPrzesuwania) {

			case CYKLICZNE:
				start = Math.floorMod(start - 1, rozmiarListy) + 1;
				break;

			case LINIOWE:
				start = Math.max(1, Math.min(start, rozmiarListy));
				break;

			case DOCISKAJACE:
				if (kierunek == Kierunek.GORA) {
					start = Math.max(start, limitGora);
					limitGora = start + dlugosc;
				} else {
					long maxStart = limitDol - dlugosc + 1;
					start = Math.min(start, maxStart);
					limitDol = start - 1;
				}

				start = Math.max(1, Math.min(start, rozmiarListy));
				break;

			default:
				throw new AssertionError(NIEOBSŁUGIWANY_TRYB_PRZESUWANIA + trybPrzesuwania);
			}

			long newLp = start;

			for (int lp = blok.start; lp <= blok.end; lp++) {

				final int destLp = (trybPrzesuwania == TrybPrzesuwania.CYKLICZNE)
						? Math.floorMod(newLp - 1, rozmiarListy) + 1
						: (int) newLp;

				docelowaPermutacja[lp - 1] = destLp;
				zajeteLp[destLp] = true;

				newLp++;
			}
		}

		// -----------------------------------------------------
		// 2. Uzupełnianie elementów niewybranych
		// -----------------------------------------------------
		int kandydat = 1;

		for (int i = 0; i < rozmiarListy; i++) {
			if (docelowaPermutacja[i] == 0) {

				while (zajeteLp[kandydat]) {
					kandydat++;
				}

				docelowaPermutacja[i] = kandydat;
				zajeteLp[kandydat] = true;
			}
		}

		return docelowaPermutacja;
	}

	/**
	 * Na podstawie docelowej permutacji tworzy plan przesunięć w postaci ciągów
	 * starych indeksów wraz z ich wspólnym offsetem.
	 * <p>
	 * Łączy kolejne pozycje o tym samym przesunięciu w jeden blok, co upraszcza
	 * aktualizację danych np. w bazie SQL.
	 */
	static PrzesuwaniePlan generujPlan(final int[] docelowaPermutacja, final int rozmiarListy) {
		return generujPlan(docelowaPermutacja, rozmiarListy, null);
	}

	static PrzesuwaniePlan generujPlan(int[] permutacja, int rozmiarListy, int[] widoczneLp) {
		PrzesuwaniePlan plan = new PrzesuwaniePlan();
		int poczatek = 1;
		int poprzednieLp = 0;
		int offset = 0;
		for (int i = 0; i < rozmiarListy; i++) {
			int stareLp = widoczneLp == null ? i + 1 : widoczneLp[i];
			int noweLp = widoczneLp == null ? permutacja[i] : widoczneLp[permutacja[i] - 1];
			int nowyOffset = noweLp - stareLp;
			if (nowyOffset != offset || stareLp - poprzednieLp != 1) {
				plan.dodaj(poczatek, poprzednieLp, offset);
				poczatek = stareLp;
				offset = nowyOffset;
			}
			poprzednieLp = stareLp;
		}
		plan.dodaj(poczatek, poprzednieLp, offset);
		return plan;
	}

	/**
	 * Buduje nową listę elementów zgodnie z wyliczoną permutacją docelową.
	 * <p>
	 * Dla każdego elementu pobiera jego nową pozycję z tablicy permutacji i
	 * umieszcza go we właściwym miejscu listy wynikowej.
	 */
	static <T> List<T> przebudujListe(final int[] docelowaPermutacja, final List<T> wszystkie) {

		final int rozmiarListy = wszystkie.size();
		final List<T> wynik = new ArrayList<>(Collections.nCopies(rozmiarListy, null));
		int i = 0;
		for (T element : wszystkie) {
			int destIndex = docelowaPermutacja[i++] - 1;
			wynik.set(destIndex, element);
		}

		return wynik;
	}

}
