package pl.com.razi.listy.przesuwanie;

import java.util.List;

import pl.com.razi.listy.przesuwanie.model.PrzesuwanieIndeksyBlok;
import pl.com.razi.listy.przesuwanie.wyjatki.PrzesuwanieBrakElementuException;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwanieWynik;

/**
 * Klasa udostępniająca wysokopoziomowe operacje przesuwania elementów listy.
 * <p>
 * Przesunięcie działa wyłącznie na podstawie porządku elementów na liście
 * (indeksów), bez wymogu posiadania pól przechowujących pozycje.
 * </p>
 *
 * <h3>Tryby przesuwania</h3>
 * <ul>
 * <li><b>LINIOWE</b> – przesunięcie w górę/dół tylko w ramach listy. Element
 * nie może wyjść poza 0 lub ostatni indeks.</li>
 *
 * <li><b>CYKLICZNE</b> – przejście modulo długość listy. Element wychodzący
 * poza granice trafia z drugiej strony.</li>
 *
 * <li><b>DOCISKAJACE</b> – elementy lub bloki są przesuwane maksymalnie w daną
 * stronę, aż „dociśnięte” do granicy listy. Jeśli pierwszy element nie może się
 * ruszyć, sprawdzany jest kolejny (analogicznie przy przesuwaniu w dół).</li>
 * </ul>
 *
 * <h3>Wyjątki</h3> Wszystkie metody mogą rzucać:
 * <ul>
 * <li>{@link PrzesuwanieBrakElementuException} – gdy którykolwiek element z listy
 * wybranych nie istnieje w liście wszystkich.</li>
 * </ul>
 *
 * @param <T> typ elementów listy
 */
public class PrzesuwanieObsluga<T> {

	public enum TrybPrzesuwania {
		LINIOWE, CYKLICZNE, DOCISKAJACE;
	}

	private TrybPrzesuwania trybPrzesuwania;

	private PrzesuwanieObsluga(TrybPrzesuwania trybPrzesuwania) {
		this.trybPrzesuwania = trybPrzesuwania;
	}

	// ====================================================================
	// ========================= BUILDER ==================================
	// ====================================================================

	public static class Builder<T> {

		private TrybPrzesuwania trybPrzesuwania = TrybPrzesuwania.LINIOWE;

		public Builder<T> trybPrzesuwania(TrybPrzesuwania trybPrzesuwania) {
			this.trybPrzesuwania = trybPrzesuwania;
			return this;
		}

		public PrzesuwanieObsluga<T> build() {
			return new PrzesuwanieObsluga<>(trybPrzesuwania);
		}

	}

	// ====================================================================
	// ========================= SETTERY ==================================
	// ====================================================================

	/**
	 * Zmienia bieżący tryb przesuwania dla wszystkich kolejnych operacji.
	 */
	public void ustawTrybPrzesuwania(TrybPrzesuwania trybPrzesuwania) {
		this.trybPrzesuwania = trybPrzesuwania;
	}

	// ====================================================================
	// ========================= API ======================================
	// ====================================================================

	/**
	 * Sprawdza, czy przesunięcie jest możliwe dla zestawu parametrów samego
	 * przesunięcia. Metoda przydatna do sterowania aktywnością przycisków. W razie
	 * błędnych danych rzuca wyjątek.
	 *
	 * @param wszystkie    Lista źródłowa, na której wykonywana jest operacja.
	 * @param wybrane      Elementy, które podlegają przesunięciu.
	 * @param przesuniecie Wartość przesunięcia (ujemna - w górę, dodatnia - w dół).
	 * @return Zwraca <b>true</b> w przypadku, gdy aktualnym trybie działania
	 *         możliwe będzie wykonanie przesunięcia.
	 * @throws PrzesuwanieBrakElementuException Jeżeli któregokolwiek elementu wybranego nie ma
	 *                               na liście wszystkich.
	 */
	public boolean czyDoPrzesunieciaDojdzie(List<T> wszystkie, List<T> wybrane, int przesuniecie)
			throws PrzesuwanieBrakElementuException {

		PrzesuwanieWejscie.walidacjaDanych(wszystkie, wybrane);

		if (!PrzesuwanieWejscie.czyParametryPrzesuwaniaPoprawne(wszystkie, wybrane, przesuniecie)) {
			return false;
		}

		List<PrzesuwanieIndeksyBlok> blokiWybranych = PrzesuwanieWejscie.zbudujBlokiWybranych(wszystkie, wybrane);

		int rozmiarListy = wszystkie.size();
		int rzeczywistePrzesuniecie = PrzesuwanieWejscie.obliczRzeczywistePrzesuniecie(trybPrzesuwania, przesuniecie,
				rozmiarListy);

		return PrzesuwanieObliczenia.czyDoPrzesunieciaDojdzie(trybPrzesuwania, blokiWybranych, rozmiarListy,
				rzeczywistePrzesuniecie);
	}

	/**
	 * Przesuwa elementy o jeden w górę w obrębie listy.
	 *
	 * @param wszystkie Lista źródłowa, na której wykonywana jest operacja.
	 * @param wybrane   Elementy, które podlegają przesunięciu.
	 * @return Nowa lista po przesunięciu.
	 * @throws PrzesuwanieBrakElementuException Jeżeli któregokolwiek elementu wybranego nie ma
	 *                               na liście wszystkich.
	 */
	public List<T> przesunWGoreOJeden(List<T> wszystkie, List<T> wybrane) throws PrzesuwanieBrakElementuException {
		return przesunListe(wszystkie, wybrane, -1);
	}

	/**
	 * Przesuwa elementy o jeden w dół w obrębie listy.
	 *
	 * @param wszystkie Lista źródłowa, na której wykonywana jest operacja.
	 * @param wybrane   Elementy, które podlegają przesunięciu.
	 * @return Nowa lista po przesunięciu.
	 * @throws PrzesuwanieBrakElementuException Jeżeli któregokolwiek elementu wybranego nie ma
	 *                               na liście wszystkich.
	 */
	public List<T> przesunWDolOJeden(List<T> wszystkie, List<T> wybrane) throws PrzesuwanieBrakElementuException {
		return przesunListe(wszystkie, wybrane, 1);
	}

	/**
	 * Przesuwa elementy w górę o przekazaną wartość w obrębie listy.
	 *
	 * @param wszystkie Lista źródłowa, na której wykonywana jest operacja.
	 * @param wybrane   Elementy, które podlegają przesunięciu.
	 * @return Nowa lista po przesunięciu.
	 * @throws PrzesuwanieBrakElementuException Jeżeli któregokolwiek elementu wybranego nie ma
	 *                               na liście wszystkich.
	 */
	public List<T> przesunWGore(List<T> wszystkie, List<T> wybrane, int przesuniecie) throws PrzesuwanieBrakElementuException {
		return przesunListe(wszystkie, wybrane, -przesuniecie);
	}

	/**
	 * Przesuwa elementy w dół o przekazaną wartość w obrębie listy.
	 *
	 * @param wszystkie Lista źródłowa, na której wykonywana jest operacja.
	 * @param wybrane   Elementy, które podlegają przesunięciu.
	 * @return Nowa lista po przesunięciu.
	 * @throws PrzesuwanieBrakElementuException Jeżeli któregokolwiek elementu wybranego nie ma
	 *                               na liście wszystkich.
	 */
	public List<T> przesunWDol(List<T> wszystkie, List<T> wybrane, int przesuniecie) throws PrzesuwanieBrakElementuException {
		return przesunListe(wszystkie, wybrane, przesuniecie);
	}

	/**
	 * Przesuwa elementy o przekazaną wartość przesunięcia w obrębie listy. Ujemna
	 * wartość przesunięcia oznacza kierunek w górę. Zwraca nową listę.
	 *
	 * @param wszystkie    Lista źródłowa, na której wykonywana jest operacja.
	 * @param wybrane      Elementy, które podlegają przesunięciu.
	 * @param przesuniecie Wartość przesunięcia. Znak oznacza kierunek.
	 * @return Nowa lista po przesunięciu.
	 * @throws PrzesuwanieBrakElementuException Jeżeli któregokolwiek elementu wybranego nie ma
	 *                               na liście wszystkich.
	 */
	public List<T> przesunListe(List<T> wszystkie, List<T> wybrane, int przesuniecie) throws PrzesuwanieBrakElementuException {
		return przesunPelny(wszystkie, wybrane, przesuniecie).getLista();
	}

	/**
	 * Przesuwa elementy o przekazaną wartość przesunięcia w obrębie listy. Ujemna
	 * wartość przesunięcia oznacza kierunek w górę. Zwraca w wyniku nową listę, a
	 * także metadane przesunięcia bloków.
	 * <p>
	 * Użyteczne przy aktualizacji bazy danych.
	 * </p>
	 *
	 * @param wszystkie    Lista źródłowa, na której wykonywana jest operacja.
	 * @param wybrane      Elementy, które podlegają przesunięciu.
	 * @param przesuniecie Wartość przesunięcia. Znak oznacza kierunek.
	 * @return Nowa lista po przesunięciu wraz z metadanymi przesunięcia.
	 * @throws PrzesuwanieBrakElementuException Jeżeli któregokolwiek elementu wybranego nie ma
	 *                               na liście wszystkich.
	 */
	public PrzesuwanieWynik<T> przesunPelny(List<T> wszystkie, List<T> wybrane, int przesuniecie)
			throws PrzesuwanieBrakElementuException {

		PrzesuwanieWejscie.walidacjaDanych(wszystkie, wybrane);

		if (!PrzesuwanieWejscie.czyParametryPrzesuwaniaPoprawne(wszystkie, wybrane, przesuniecie)) {
			return PrzesuwanieWynik.getInstancePusty(wszystkie);
		}

		List<PrzesuwanieIndeksyBlok> blokiWybranych = PrzesuwanieWejscie.zbudujBlokiWybranych(wszystkie, wybrane);

		int rozmiarListy = wszystkie.size();
		int rzeczywistePrzesuniecie = PrzesuwanieWejscie.obliczRzeczywistePrzesuniecie(trybPrzesuwania, przesuniecie,
				rozmiarListy);

		return PrzesuwanieObliczenia.wykonaj(trybPrzesuwania, wszystkie, blokiWybranych, rzeczywistePrzesuniecie);
	}

}
