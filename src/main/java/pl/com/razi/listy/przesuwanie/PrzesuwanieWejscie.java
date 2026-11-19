package pl.com.razi.listy.przesuwanie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pl.com.razi.listy.przesuwanie.PrzesuwanieObsluga.TrybPrzesuwania;
import pl.com.razi.listy.przesuwanie.model.PrzesuwanieIndeksyBlok;
import pl.com.razi.listy.przesuwanie.wyjatki.PrzesuwanieBrakElementuException;

/**
 * Klasa pomocnicza odpowiedzialna za walidację danych wejściowych oraz
 * przetwarzanie listy źródłowej przed przekazaniem jej do właściwych obliczeń
 * przesunięcia.
 * <p>
 * Metody dostarczają:
 * <ul>
 * <li>weryfikację poprawności list wejściowych,</li>
 * <li>sprawdzenie istnienia elementów wybranych w liście źródłowej,</li>
 * <li>obliczenie rzeczywistej wartości przesunięcia (np. w trybie
 * cyklicznym),</li>
 * <li>zbudowanie ciągłych bloków indeksów na podstawie kolejności występowania
 * elementów na liście.</li>
 * </ul>
 * Klasa jest wyłącznie pomocnicza i nie powinna być używana bezpośrednio poza
 * mechanizmem przesuwania.
 */
class PrzesuwanieWejscie {

	private PrzesuwanieWejscie() {
		throw new AssertionError();
	}

	/**
	 * Weryfikuje, czy wszystkie elementy wybrane występują na liście źródłowej.
	 * <p>
	 * Jeśli lista wybranych jest pusta lub null – walidacja przechodzi pozytywnie.
	 * W przeciwnym wypadku metoda sprawdza, czy każdy element wybrany występuje
	 * na liście źródłowej w wymaganej liczbie. Jeśli nie – rzucany jest wyjątek.
	 */
	public static <T> void walidacjaDanych(List<T> wszystkie, List<T> wybrane) throws PrzesuwanieBrakElementuException {

		if (wybrane == null || wybrane.isEmpty()) {
			return;
		}

		if (wszystkie == null) {
			throw new PrzesuwanieBrakElementuException();
		}

		pozycjeWybranych(wszystkie, wybrane);
	}

	/**
	 * Odtwarza pozycje zaznaczenia w kolejności listy źródłowej. Dla równych
	 * elementów wybiera pierwsze wystąpienia w liczbie podanej w zaznaczeniu.
	 */
	private static <T> int[] pozycjeWybranych(List<T> wszystkie, List<T> wybrane)
			throws PrzesuwanieBrakElementuException {
		Map<T, Integer> pozostale = new HashMap<>();
		for (T element : wybrane) {
			pozostale.merge(element, 1, Integer::sum);
		}

		int[] pozycje = new int[wybrane.size()];
		int znalezione = 0;
		int lp = 0;
		for (T element : wszystkie) {
			lp++;
			Integer ile = pozostale.get(element);
			if (ile == null) {
				continue;
			}
			pozycje[znalezione++] = lp;
			if (znalezione == pozycje.length) {
				return pozycje;
			}
			if (ile == 1) {
				pozostale.remove(element);
			} else {
				pozostale.put(element, ile - 1);
			}
		}
		throw new PrzesuwanieBrakElementuException();
	}

	/**
	 * Odczytuje pozycje uporządkowanego zaznaczenia w jednym przejściu.
	 */
	private static <T> int[] pozycjeWybranychWKolejnosciListy(List<T> wszystkie, List<T> wybrane)
			throws PrzesuwanieBrakElementuException {
		int[] pozycje = new int[wybrane.size()];
		int znalezione = 0;
		Iterator<T> zaznaczenie = wybrane.iterator();
		T szukany = zaznaczenie.next();
		int lp = 0;
		for (T element : wszystkie) {
			lp++;
			if (Objects.equals(element, szukany)) {
				pozycje[znalezione++] = lp;
				if (!zaznaczenie.hasNext()) {
					return pozycje;
				}
				szukany = zaznaczenie.next();
			}
		}

		// Przy niepełnym dopasowaniu rozróżniamy brak wystąpienia od błędnej kolejności.
		walidacjaDanych(wszystkie, wybrane);
		throw new IllegalArgumentException("Wybrane elementy muszą być w kolejności listy źródłowej.");
	}

	/**
	 * Sprawdza minimalne warunki umożliwiające wykonanie jakiegokolwiek
	 * przesunięcia.
	 * <p>
	 * Zwraca {@code true}, jeśli:
	 * <ul>
	 * <li>lista źródłowa nie jest pusta,</li>
	 * <li>lista wybranych elementów nie jest pusta,</li>
	 * <li>wartość przesunięcia jest różna od zera.</li>
	 * </ul>
	 */
	public static <T> boolean czyParametryPrzesuwaniaPoprawne(List<T> wszystkie, List<T> wybrane, int przesuniecie) {
		if (wszystkie == null || wszystkie.isEmpty()) {
			return false;
		}
		if (wybrane == null || wybrane.isEmpty()) {
			return false;
		}
		if (przesuniecie == 0) {
			return false;
		}
		return true;
	}

	/**
	 * Oblicza ostateczną wartość przesunięcia, uwzględniając tryb pracy.
	 * <p>
	 * W trybie <b>CYKLICZNYM</b> przesunięcie jest redukowane modulo rozmiar listy,
	 * aby każdorazowo mieściło się w zakresie. W pozostałych trybach zwracana jest
	 * wartość wejściowa bez zmian.
	 */
	public static int obliczRzeczywistePrzesuniecie(TrybPrzesuwania trybPrzesuwania, int przesuniecie,
			int rozmiarListy) {
		return trybPrzesuwania == TrybPrzesuwania.CYKLICZNE ? przesuniecie % rozmiarListy : przesuniecie;
	}

	/**
	 * Buduje listę ciągłych bloków indeksów elementów wybranych na podstawie
	 * kolejności ich występowania w liście źródłowej.
	 * <p>
	 * Przeszukiwana jest kolejno lista źródłowa, a każdy odnaleziony element
	 * dodawany jest do bieżącego bloku, jeśli jego indeks bezpośrednio sąsiaduje z
	 * poprzednim. W przeciwnym przypadku rozpoczynany jest nowy blok.
	 * <p>
	 * Metoda zakłada, że elementy z listy wybranych znajdują się w liście źródłowej
	 * – ewentualne błędy istnienia powinny być wychwycone wcześniej przez
	 * walidację.
	 */
	public static <T> List<PrzesuwanieIndeksyBlok> zbudujBlokiWybranych(List<T> wszystkie, List<T> wybrane) {
		if (wszystkie == null || wszystkie.isEmpty()) {
			return new ArrayList<>();
		}
		try {
			return zbudujBlokiWybranych(wszystkie, wybrane, true);
		} catch (PrzesuwanieBrakElementuException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	/**
	 * Buduje ciągłe bloki zaznaczenia. Domyślnie kolejność elementów wybranych
	 * jest odtwarzana na podstawie listy źródłowej.
	 */
	public static <T> List<PrzesuwanieIndeksyBlok> zbudujBlokiWybranych(List<T> wszystkie, List<T> wybrane,
			boolean normalizujKolejnoscWybranych) throws PrzesuwanieBrakElementuException {

		List<PrzesuwanieIndeksyBlok> bloki = new ArrayList<>();
		if (wybrane == null || wybrane.isEmpty()) {
			return bloki;
		}
		if (wszystkie == null) {
			throw new PrzesuwanieBrakElementuException();
		}

		int[] pozycje = normalizujKolejnoscWybranych ? pozycjeWybranych(wszystkie, wybrane)
				: pozycjeWybranychWKolejnosciListy(wszystkie, wybrane);
		int startBloku = pozycje[0];
		int poprzedni = startBloku;
		for (int i = 1; i < pozycje.length; i++) {
			int lp = pozycje[i];
			if (lp != poprzedni + 1) {
				bloki.add(new PrzesuwanieIndeksyBlok(startBloku, poprzedni));
				startBloku = lp;
			}
			poprzedni = lp;
		}
		bloki.add(new PrzesuwanieIndeksyBlok(startBloku, poprzedni));
		return bloki;
	}

}
