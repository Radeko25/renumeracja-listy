package pl.com.razi.listy.przesuwanie;

import java.util.List;
import java.util.Objects;

import pl.com.razi.listy.przesuwanie.model.PrzesuwanieIndeksyBlok;
import pl.com.razi.listy.przesuwanie.wyjatki.PrzesuwanieBrakElementuException;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwanieWynik;

/**
 * Wysokopoziomowe API do przesuwania pojedynczych elementów i wielu rozłącznych
 * bloków na liście.
 * <p>
 * Biblioteka operuje na porządku elementów, nie wymaga więc pola LP w samych
 * obiektach. Plan wynikowy może zostać później przełożony np. na aktualizację
 * kolumny porządkowej w bazie danych.
 *
 * @param <T> typ elementów listy
 */
public class PrzesuwanieObsluga<T> {

	public enum TrybPrzesuwania {
		LINIOWE, CYKLICZNE, DOCISKAJACE;
	}

	private TrybPrzesuwania trybPrzesuwania;
	private boolean normalizujKolejnoscWybranych;

	private PrzesuwanieObsluga(TrybPrzesuwania trybPrzesuwania, boolean normalizujKolejnoscWybranych) {
		this.trybPrzesuwania = Objects.requireNonNull(trybPrzesuwania, "Tryb przesuwania nie może być null.");
		this.normalizujKolejnoscWybranych = normalizujKolejnoscWybranych;
	}

	public static class Builder<T> {

		private TrybPrzesuwania trybPrzesuwania = TrybPrzesuwania.LINIOWE;
		private boolean normalizujKolejnoscWybranych = true;

		public Builder<T> trybPrzesuwania(TrybPrzesuwania trybPrzesuwania) {
			this.trybPrzesuwania = Objects.requireNonNull(trybPrzesuwania, "Tryb przesuwania nie może być null.");
			return this;
		}

		/**
		 * Określa, czy biblioteka ma sama odtworzyć kolejność elementów wybranych
		 * zgodnie z listą źródłową.
		 * <p>
		 * Domyślnie: {@code true} – bezpieczne API dla dowolnej kolejności wejścia.
		 * Ustawienie {@code false} wybiera szybszą ścieżkę dla GUI lub innego callera,
		 * który gwarantuje już kolejność zgodną z listą źródłową.
		 */
		public Builder<T> normalizujKolejnoscWybranych(boolean normalizujKolejnoscWybranych) {
			this.normalizujKolejnoscWybranych = normalizujKolejnoscWybranych;
			return this;
		}

		public PrzesuwanieObsluga<T> build() {
			return new PrzesuwanieObsluga<>(trybPrzesuwania, normalizujKolejnoscWybranych);
		}
	}

	/** Zmienia bieżący tryb przesuwania dla kolejnych operacji. */
	public void ustawTrybPrzesuwania(TrybPrzesuwania trybPrzesuwania) {
		this.trybPrzesuwania = Objects.requireNonNull(trybPrzesuwania, "Tryb przesuwania nie może być null.");
	}

	/**
	 * Włącza lub wyłącza automatyczną normalizację kolejności listy wybranych.
	 * Zobacz {@link Builder#normalizujKolejnoscWybranych(boolean)}.
	 */
	public void ustawNormalizacjeKolejnosciWybranych(boolean normalizujKolejnoscWybranych) {
		this.normalizujKolejnoscWybranych = normalizujKolejnoscWybranych;
	}

	/**
	 * Sprawdza, czy dla podanych parametrów dojdzie do przesunięcia co najmniej
	 * jednego elementu.
	 */
	public boolean czyDoPrzesunieciaDojdzie(List<T> wszystkie, List<T> wybrane, int przesuniecie)
			throws PrzesuwanieBrakElementuException {

		if (!PrzesuwanieWejscie.czyParametryPrzesuwaniaPoprawne(wszystkie, wybrane, przesuniecie)) {
			PrzesuwanieWejscie.walidacjaDanych(wszystkie, wybrane);
			return false;
		}

		List<PrzesuwanieIndeksyBlok> blokiWybranych = PrzesuwanieWejscie.zbudujBlokiWybranych(wszystkie, wybrane,
				normalizujKolejnoscWybranych);

		int rozmiarListy = wszystkie.size();
		int rzeczywistePrzesuniecie = PrzesuwanieWejscie.obliczRzeczywistePrzesuniecie(trybPrzesuwania, przesuniecie,
				rozmiarListy);

		return PrzesuwanieObliczenia.czyDoPrzesunieciaDojdzie(trybPrzesuwania, blokiWybranych, rozmiarListy,
				rzeczywistePrzesuniecie);
	}

	public List<T> przesunWGoreOJeden(List<T> wszystkie, List<T> wybrane) throws PrzesuwanieBrakElementuException {
		return przesunListe(wszystkie, wybrane, -1);
	}

	public List<T> przesunWDolOJeden(List<T> wszystkie, List<T> wybrane) throws PrzesuwanieBrakElementuException {
		return przesunListe(wszystkie, wybrane, 1);
	}

	/**
	 * Przesuwa elementy w górę o nieujemną liczbę pozycji.
	 * Dla signed displacement użyj {@link #przesunListe(List, List, int)}.
	 */
	public List<T> przesunWGore(List<T> wszystkie, List<T> wybrane, int przesuniecie)
			throws PrzesuwanieBrakElementuException {
		wymagajNieujemnejWartosciKierunkowej(przesuniecie);
		return przesunListe(wszystkie, wybrane, -przesuniecie);
	}

	/**
	 * Przesuwa elementy w dół o nieujemną liczbę pozycji.
	 * Dla signed displacement użyj {@link #przesunListe(List, List, int)}.
	 */
	public List<T> przesunWDol(List<T> wszystkie, List<T> wybrane, int przesuniecie)
			throws PrzesuwanieBrakElementuException {
		wymagajNieujemnejWartosciKierunkowej(przesuniecie);
		return przesunListe(wszystkie, wybrane, przesuniecie);
	}

	/**
	 * Przesuwa elementy o signed displacement: wartość ujemna oznacza ruch w górę,
	 * dodatnia – w dół.
	 */
	public List<T> przesunListe(List<T> wszystkie, List<T> wybrane, int przesuniecie)
			throws PrzesuwanieBrakElementuException {
		return przesunPelny(wszystkie, wybrane, przesuniecie).getLista();
	}

	/**
	 * Wykonuje pełną operację i zwraca zarówno nową listę, jak i skompresowany plan
	 * zmiany LP.
	 */
	public PrzesuwanieWynik<T> przesunPelny(List<T> wszystkie, List<T> wybrane, int przesuniecie)
			throws PrzesuwanieBrakElementuException {

		if (!PrzesuwanieWejscie.czyParametryPrzesuwaniaPoprawne(wszystkie, wybrane, przesuniecie)) {
			PrzesuwanieWejscie.walidacjaDanych(wszystkie, wybrane);
			return PrzesuwanieWynik.getInstancePusty(wszystkie);
		}

		List<PrzesuwanieIndeksyBlok> blokiWybranych = PrzesuwanieWejscie.zbudujBlokiWybranych(wszystkie, wybrane,
				normalizujKolejnoscWybranych);

		int rozmiarListy = wszystkie.size();
		int rzeczywistePrzesuniecie = PrzesuwanieWejscie.obliczRzeczywistePrzesuniecie(trybPrzesuwania, przesuniecie,
				rozmiarListy);

		return PrzesuwanieObliczenia.wykonaj(trybPrzesuwania, wszystkie, blokiWybranych, rzeczywistePrzesuniecie);
	}

	private static void wymagajNieujemnejWartosciKierunkowej(int przesuniecie) {
		if (przesuniecie < 0) {
			throw new IllegalArgumentException(
					"Metody kierunkowe przesunWGore/przesunWDol oczekują nieujemnej wartości przesunięcia.");
		}
	}
}
