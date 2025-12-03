package pl.com.razi.listy.przesuwanie.wyjatki;

public class PrzesuwanieBrakElementuException extends Exception {

	public PrzesuwanieBrakElementuException() {
		super("Wybrany element nie istnieje na liście wszystkich elementów.");
	}

}
