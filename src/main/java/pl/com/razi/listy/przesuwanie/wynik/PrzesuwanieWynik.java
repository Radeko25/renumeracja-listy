package pl.com.razi.listy.przesuwanie.wynik;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ObjIntConsumer;

/**
 * Reprezentuje wynik operacji przesunięcia elementów listy.
 * <p>
 * Zawiera zarówno:
 * <ul>
 * <li>nową listę elementów po przemieszczeniu,</li>
 * <li>plan przesunięć opisujący zmiany pozycji (przydatny np. do aktualizacji
 * bazy danych).</li>
 * </ul>
 * <p>
 * Klasa jest niemutowalna — przechowuje finalne wyniki operacji przesuwania.
 */
public class PrzesuwanieWynik<T> {

    private final List<T> lista;
    private final PrzesuwaniePlan plan;

    public PrzesuwanieWynik(List<T> lista, PrzesuwaniePlan plan) {
        this.lista = lista;
        this.plan = plan;
    }

    public List<T> getLista() {
        return lista;
    }

    public PrzesuwaniePlan getPlan() {
        return plan;
    }

	/**
	 * Ustawia numery porządkowe (LP) zaczynając od 1.
	 * <p>
	 * Patrz: {@link #ustawLp(int, ObjIntConsumer)}.
	 */
	public void ustawLp(ObjIntConsumer<T> setter) {
		ustawLp(1, setter);
	}

	/**
	 * Ustawia numery porządkowe (LP) elementom listy wynikowej, zaczynając od
	 * przekazanej wartości początkowej.
	 * <p>
	 * Umożliwia to numerowanie również fragmentów listy lub sytuacji, gdy lista
	 * wynikowa odpowiada jedynie części większej tabeli.
	 * <p>
	 * Setter jest funkcją otrzymującą obiekt oraz numer porządkowy (1-based lub
	 * dowolnie wybrany przez użytkownika). Klasa {@link ObjIntConsumer} pozwala na
	 * stosowanie dowolnych klas danych — bez konieczności implementacji dodatkowych
	 * interfejsów.
	 */
	public void ustawLp(int lpStart, ObjIntConsumer<T> setter) {
		int lp = lpStart;
		for (T element : lista) {
			setter.accept(element, lp++);
		}
	}

	public static <T> PrzesuwanieWynik<T> getInstancePusty(List<T> lista) {
		return new PrzesuwanieWynik<T>(new ArrayList<>(lista), new PrzesuwaniePlan());
	}

}
