package pl.com.razi.listy.przesuwanie.wynik;

import java.util.ArrayList;
import java.util.Collections;
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
 * Lista i plan są migawką kolejności. Obiekty na liście zachowują swoją
 * tożsamość i mogą otrzymać numery przez {@link #ustawLp(ObjIntConsumer)}.
 */
public class PrzesuwanieWynik<T> {

	private final List<T> lista;
	private final PrzesuwaniePlan plan;
	private final int[] widoczneLp;

	public PrzesuwanieWynik(List<T> lista, PrzesuwaniePlan plan) {
		this(lista, plan, null);
	}

	public PrzesuwanieWynik(List<T> lista, PrzesuwaniePlan plan, int[] widoczneLp) {
		this.widoczneLp = widoczneLp == null ? null : widoczneLp.clone();
		if (this.widoczneLp != null) {
			if (this.widoczneLp.length != lista.size()) {
				throw new IllegalArgumentException("Każdy widoczny rekord musi mieć własne LP.");
			}
			int poprzednie = 0;
			for (int lp : this.widoczneLp) {
				if (lp <= poprzednie) {
					throw new IllegalArgumentException("Widoczne LP muszą być dodatnie i ściśle rosnące.");
				}
				poprzednie = lp;
			}
		}
		this.lista = Collections.unmodifiableList(new ArrayList<>(lista));
		this.plan = plan.kopiaDoOdczytu();
	}

	public List<T> getLista() {
		return lista;
	}

	public PrzesuwaniePlan getPlan() {
		return plan;
	}

	/**
	 * Przypisuje wynikowym elementom oryginalne widoczne LP. Przy zwykłym
	 * przesunięciu pełnej listy numeruje od 1.
	 * <p>
	 * Patrz: {@link #ustawLp(int, ObjIntConsumer)}.
	 */
	public void ustawLp(ObjIntConsumer<T> setter) {
		if (widoczneLp == null) {
			ustawLp(1, setter);
			return;
		}
		for (int i = 0; i < widoczneLp.length; i++) {
			setter.accept(lista.get(i), widoczneLp[i]);
		}
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
		return new PrzesuwanieWynik<>(lista == null ? List.of() : lista, new PrzesuwaniePlan());
	}

}
