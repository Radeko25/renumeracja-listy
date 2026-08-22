package pl.com.razi.listy.przesuwanie.wyjatki;

public class PrzesuwanieBrakElementuException extends Exception {

	public PrzesuwanieBrakElementuException() {
		super("Co najmniej jednego wybranego elementu (lub wymaganej liczby jego wystąpień) nie ma na liście źródłowej.");
	}
}
