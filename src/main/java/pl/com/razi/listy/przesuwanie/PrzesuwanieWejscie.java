package pl.com.razi.listy.przesuwanie;

import java.util.ArrayList;
import java.util.HashMap;
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
 */
class PrzesuwanieWejscie {

	private PrzesuwanieWejscie() {
		throw new AssertionError();
	}

	/**
	 * Weryfikuje, czy wszystkie elementy wybrane występują na liście źródłowej
	 * w wymaganej liczbie wystąpień.
	 * <p>
	 * Jeżeli lista wybranych jest pusta lub {@code null}, walidacja przechodzi
	 * pozytywnie. Elementy są porównywane zgodnie z kontraktem {@link Object#equals(Object)}
	 * i {@link Object#hashCode()}.
	 */
	public static <T> void walidacjaDanych(List<T> wszystkie, List<T> wybrane)
			throws PrzesuwanieBrakElementuException {

		if (wybrane == null || wybrane.isEmpty()) {
			return;
		}

		if (wszystkie == null) {
			throw new PrzesuwanieBrakElementuException();
		}

		Map<T, Integer> dostepneWystapienia = new HashMap<>();
		for (T element : wszystkie) {
			dostepneWystapienia.merge(element, 1, Integer::sum);
		}

		for (T element : wybrane) {
			Integer liczba = dostepneWystapienia.get(element);
			if (liczba == null || liczba == 0) {
				throw new PrzesuwanieBrakElementuException();
			}

			if (liczba == 1) {
				dostepneWystapienia.remove(element);
			} else {
				dostepneWystapienia.put(element, liczba - 1);
			}
		}
	}

	/**
	 * Sprawdza minimalne warunki umożliwiające wykonanie jakiegokolwiek
	 * przesunięcia.
	 */
	public static <T> boolean czyParametryPrzesuwaniaPoprawne(List<T> wszystkie, List<T> wybrane, int przesuniecie) {
		if (wszystkie == null || wszystkie.isEmpty()) {
			return false;
		}
		if (wybrane == null || wybrane.isEmpty()) {
			return false;
		}
		return przesuniecie != 0;
	}

	/**
	 * Oblicza ostateczną wartość przesunięcia, uwzględniając tryb pracy.
	 * W trybie cyklicznym przesunięcie jest redukowane modulo rozmiar listy.
	 */
	public static int obliczRzeczywistePrzesuniecie(TrybPrzesuwania trybPrzesuwania, int przesuniecie,
			int rozmiarListy) {
		return trybPrzesuwania == TrybPrzesuwania.CYKLICZNE ? przesuniecie % rozmiarListy : przesuniecie;
	}

	/**
	 * Buduje ciągłe bloki indeksów elementów wybranych.
	 * <p>
	 * Gdy {@code normalizujKolejnoscWybranych == true}, kolejność listy
	 * {@code wybrane} nie ma znaczenia. Metoda odtwarza wybór w kolejności
	 * występowania na liście źródłowej bez modyfikowania przekazanej listy.
	 * <p>
	 * Gdy parametr ma wartość {@code false}, stosowana jest szybsza ścieżka dla
	 * callerów, którzy gwarantują, że {@code wybrane} jest już uporządkowane zgodnie
	 * z kolejnością w {@code wszystkie}. Naruszenie tego kontraktu kończy się
	 * {@link IllegalArgumentException} zamiast niejawnego uszkodzenia indeksów.
	 */
	public static <T> List<PrzesuwanieIndeksyBlok> zbudujBlokiWybranych(List<T> wszystkie, List<T> wybrane,
			boolean normalizujKolejnoscWybranych) throws PrzesuwanieBrakElementuException {

		if (wszystkie == null || wybrane == null || wszystkie.isEmpty() || wybrane.isEmpty()) {
			return new ArrayList<>();
		}

		return normalizujKolejnoscWybranych
				? zbudujBlokiDlaDowolnejKolejnosci(wszystkie, wybrane)
				: zbudujBlokiDlaKolejnosciZrodlowej(wszystkie, wybrane);
	}

	/**
	 * Zachowuje zgodność z dotychczasowym użyciem wewnętrznym i testowym:
	 * domyślnie wejście jest normalizowane do kolejności listy źródłowej.
	 */
	public static <T> List<PrzesuwanieIndeksyBlok> zbudujBlokiWybranych(List<T> wszystkie, List<T> wybrane) {
		try {
			return zbudujBlokiWybranych(wszystkie, wybrane, true);
		} catch (PrzesuwanieBrakElementuException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	private static <T> List<PrzesuwanieIndeksyBlok> zbudujBlokiDlaDowolnejKolejnosci(List<T> wszystkie,
			List<T> wybrane) throws PrzesuwanieBrakElementuException {

		Map<T, Integer> pozostaleWybrane = new HashMap<>();
		for (T element : wybrane) {
			pozostaleWybrane.merge(element, 1, Integer::sum);
		}

		List<Integer> lpWybranych = new ArrayList<>(wybrane.size());
		for (int i = 0; i < wszystkie.size() && !pozostaleWybrane.isEmpty(); i++) {
			T element = wszystkie.get(i);
			Integer pozostalo = pozostaleWybrane.get(element);
			if (pozostalo == null) {
				continue;
			}

			lpWybranych.add(i + 1);
			if (pozostalo == 1) {
				pozostaleWybrane.remove(element);
			} else {
				pozostaleWybrane.put(element, pozostalo - 1);
			}
		}

		if (!pozostaleWybrane.isEmpty()) {
			throw new PrzesuwanieBrakElementuException();
		}

		return zbudujBlokiZLp(lpWybranych);
	}

	private static <T> List<PrzesuwanieIndeksyBlok> zbudujBlokiDlaKolejnosciZrodlowej(List<T> wszystkie,
			List<T> wybrane) throws PrzesuwanieBrakElementuException {

		List<Integer> lpWybranych = new ArrayList<>(wybrane.size());
		int startWyszukiwania = 0;

		for (T elem : wybrane) {
			int idx = -1;
			for (int i = startWyszukiwania; i < wszystkie.size(); i++) {
				if (Objects.equals(wszystkie.get(i), elem)) {
					idx = i;
					startWyszukiwania = i + 1;
					break;
				}
			}

			if (idx < 0) {
				// Ścieżka błędna jest rzadka, więc dopiero tutaj wykonujemy pełną walidację.
				// Jeżeli elementu/wystąpienia faktycznie brakuje, zachowujemy checked exception.
				walidacjaDanych(wszystkie, wybrane);
				throw new IllegalArgumentException(
						"Lista 'wybrane' nie jest uporządkowana zgodnie z kolejnością listy źródłowej. "
								+ "Włącz normalizację kolejności wybranych albo przekaż elementy w kolejności źródłowej.");
			}

			lpWybranych.add(idx + 1);
		}

		return zbudujBlokiZLp(lpWybranych);
	}

	private static List<PrzesuwanieIndeksyBlok> zbudujBlokiZLp(List<Integer> lpWybranych) {
		List<PrzesuwanieIndeksyBlok> bloki = new ArrayList<>();
		if (lpWybranych.isEmpty()) {
			return bloki;
		}

		int startBloku = lpWybranych.get(0);
		int poprzedni = startBloku;

		for (int i = 1; i < lpWybranych.size(); i++) {
			int lp = lpWybranych.get(i);
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
