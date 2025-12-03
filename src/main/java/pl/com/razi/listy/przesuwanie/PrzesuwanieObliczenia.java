package pl.com.razi.listy.przesuwanie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import pl.com.razi.listy.przesuwanie.PrzesuwanieObsluga.TrybPrzesuwania;
import pl.com.razi.listy.przesuwanie.model.PrzesuwanieIndeksyBlok;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwanieWynik;

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

	private static Kierunek okreslKierunek(int przesuniecie) {
		return przesuniecie < 0 ? Kierunek.GORA : Kierunek.DOL;
	}

	/**
	 * Określa, czy przy danym trybie, kierunku i blokach dojdzie do przesunięcia
	 * choćby jednego elementu listy.
	 * <p>
	 * Zakłada pełną poprawność danych wejściowych.
	 */
	public static <T> boolean czyDoPrzesunieciaDojdzie(final TrybPrzesuwania trybPrzesuwania,
			final List<PrzesuwanieIndeksyBlok> blokiWybranych, final int rozmiarListy, final int przesuniecie) {

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
	 * Wykonuje pełną operację przesunięcia bloków elementów zgodnie z zadanym
	 * trybem oraz zwraca zarówno finalną listę, jak i plan przesunięć.
	 * <p>
	 * Zakłada pełną poprawność danych wejściowych.
	 */
	public static <T> PrzesuwanieWynik<T> wykonaj(final TrybPrzesuwania trybPrzesuwania, final List<T> wszystkie,
			final List<PrzesuwanieIndeksyBlok> blokiWybranych, final int przesuniecie) {

		final int rozmiarListy = wszystkie.size();

		if (!czyDoPrzesunieciaDojdzie(trybPrzesuwania, blokiWybranych, rozmiarListy, przesuniecie)) {
			return PrzesuwanieWynik.getInstancePusty(wszystkie);
		}

		final int[] docelowaPermutacja = obliczDocelowaPermutacje(trybPrzesuwania, blokiWybranych, rozmiarListy,
				przesuniecie);

		final PrzesuwaniePlan plan = generujPlan(docelowaPermutacja, rozmiarListy);
		final List<T> wynikowaLista = przebudujListe(docelowaPermutacja, wszystkie);

		return new PrzesuwanieWynik<>(wynikowaLista, plan);
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
			final List<PrzesuwanieIndeksyBlok> blokiWybranych, final int rozmiarListy, final int przesuniecie) {

		final int[] docelowaPermutacja = new int[rozmiarListy];
		final boolean[] zajeteLp = new boolean[rozmiarListy + 1];

		final Kierunek kierunek = okreslKierunek(przesuniecie);

		int limitGora = 1;
		int limitDol = rozmiarListy;

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
			int start = blok.start + przesuniecie;

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
					int maxStart = limitDol - dlugosc + 1;
					start = Math.min(start, maxStart);
					limitDol = start - 1;
				}

				start = Math.max(1, Math.min(start, rozmiarListy));
				break;

			default:
				throw new AssertionError(NIEOBSŁUGIWANY_TRYB_PRZESUWANIA + trybPrzesuwania);
			}

			int newLp = start;

			for (int lp = blok.start; lp <= blok.end; lp++) {

				final int destLp = (trybPrzesuwania == TrybPrzesuwania.CYKLICZNE)
						? Math.floorMod(newLp - 1, rozmiarListy) + 1
						: newLp;

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

		class Move {
			final int oldLp;
			final int offset;

			private Move(int oldLp, int offset) {
				this.oldLp = oldLp;
				this.offset = offset;
			}
		}

		final PrzesuwaniePlan plan = new PrzesuwaniePlan();
		final List<Move> lista = new ArrayList<>();

		for (int oldLp = 1; oldLp <= rozmiarListy; oldLp++) {

			final int newLp = docelowaPermutacja[oldLp - 1];
			final int offset = newLp - oldLp;

			if (offset != 0) {
				final Move mv = new Move(oldLp, offset);
				lista.add(mv);
			}
		}

		if (lista.isEmpty()) {
			return plan;
		}

		lista.sort(Comparator.comparingInt(a -> a.oldLp));

		int start = lista.get(0).oldLp;
		int prev = start;
		int offset = lista.get(0).offset;

		for (int i = 1; i < lista.size(); i++) {
			final Move mv = lista.get(i);

			if (mv.oldLp == prev + 1 && mv.offset == offset) {
				prev = mv.oldLp;
			} else {
				plan.dodaj(start, prev, offset);
				start = prev = mv.oldLp;
				offset = mv.offset;
			}
		}

		plan.dodaj(start, prev, offset);

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
		final List<T> wynik = new ArrayList<>(Collections.nCopies(rozmiarListy, (T) null));

		for (int i = 0; i < rozmiarListy; i++) {
			int destIndex = docelowaPermutacja[i] - 1;
			wynik.set(destIndex, wszystkie.get(i));
		}

		return wynik;
	}

}
