package pl.com.razi.listy.przesuwanie;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
	 * W przeciwnym wypadku metoda sprawdza, czy każdy element wybrany znajduje się
	 * w liście źródłowej, jeśli nie – rzucany jest wyjątek.
	 */
	public static <T> void walidacjaDanych(List<T> wszystkie, List<T> wybrane) throws PrzesuwanieBrakElementuException {

		if (wybrane == null || wybrane.isEmpty()) {
			return;
		}

		if (wszystkie == null) {
			throw new PrzesuwanieBrakElementuException();
		}

		Set<T> zbior = new HashSet<>(wszystkie);

		for (T element : wybrane) {
			if (!zbior.contains(element)) {
				throw new PrzesuwanieBrakElementuException();
			}
		}
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

		List<PrzesuwanieIndeksyBlok> bloki = new ArrayList<>();

		if (wszystkie == null || wybrane == null || wszystkie.isEmpty() || wybrane.isEmpty()) {
			return bloki;
		}

		int startWyszukiwania = 0;

		int startBloku = -1;
		int poprzedni = -1;

		for (T elem : wybrane) {

			int idx = -1;
			for (int i = startWyszukiwania; i < wszystkie.size(); i++) {
				if (Objects.equals(wszystkie.get(i), elem)) {
					idx = i;
					startWyszukiwania = i + 1;
					break;
				}
			}

			int lp = idx + 1;

			if (startBloku == -1) {
				startBloku = lp;
				poprzedni = lp;
				continue;
			}

			if (lp == poprzedni + 1) {
				poprzedni = lp;
				continue;
			}

			bloki.add(new PrzesuwanieIndeksyBlok(startBloku, poprzedni));
			startBloku = poprzedni = lp;
		}

		bloki.add(new PrzesuwanieIndeksyBlok(startBloku, poprzedni));

		return bloki;
	}

}
