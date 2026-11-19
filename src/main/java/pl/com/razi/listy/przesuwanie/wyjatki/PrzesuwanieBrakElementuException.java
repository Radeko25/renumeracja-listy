package pl.com.razi.listy.przesuwanie.wyjatki;

public class PrzesuwanieBrakElementuException extends Exception {

	public PrzesuwanieBrakElementuException() {
		super("Wybrany element nie występuje na liście wszystkich elementów w wymaganej liczbie.");
	}

}
