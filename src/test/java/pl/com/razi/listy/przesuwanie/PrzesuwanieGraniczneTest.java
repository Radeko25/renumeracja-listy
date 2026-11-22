package pl.com.razi.listy.przesuwanie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import pl.com.razi.listy.przesuwanie.PrzesuwanieObsluga.TrybPrzesuwania;
import pl.com.razi.listy.przesuwanie.wyjatki.PrzesuwanieBrakElementuException;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwanieWynik;

class PrzesuwanieGraniczneTest {

	private final List<Integer> wszystkie = List.of(1, 2, 3, 4, 5);
	private final List<Integer> wybrane = List.of(2, 4);

	private PrzesuwanieObsluga<Integer> obsluga(TrybPrzesuwania tryb) {
		return new PrzesuwanieObsluga.Builder<Integer>().trybPrzesuwania(tryb).build();
	}

	@Test
	void zablokowanyRuchDajePustySql() throws PrzesuwanieBrakElementuException {
		var wynik = obsluga(TrybPrzesuwania.LINIOWE).przesunPelny(wszystkie, List.of(5), 1);

		assertEquals("", wynik.getPlan().toSqlCaseWhenBloki("Dokumenty", "lp", null));
		assertEquals("", wynik.getPlan().toSqlCaseWhenBloki("Dokumenty", "lp", "id_kategorii = 3"));
	}

	@Test
	void sqlZachowujeZnakNajmniejszegoPrzesuniecia() {
		var plan = new PrzesuwaniePlan();
		plan.dodaj(1, 1, Integer.MIN_VALUE);

		assertEquals("UPDATE Dokumenty\nSET lp = CASE\n"
				+ "    WHEN lp BETWEEN 1 AND 1 THEN lp - 2147483648\n    ELSE lp\nEND\nWHERE (lp BETWEEN 1 AND 1);",
				plan.toSqlCaseWhenBloki("Dokumenty", "lp", null));
	}

	@Test
	void ruchLiniowyPozaListaJestZablokowany() throws PrzesuwanieBrakElementuException {
		var liniowe = obsluga(TrybPrzesuwania.LINIOWE);
		for (int przesuniecie : List.of(Integer.MIN_VALUE, Integer.MAX_VALUE)) {
			assertFalse(liniowe.czyDoPrzesunieciaDojdzie(wszystkie, wybrane, przesuniecie));
			var wynik = liniowe.przesunPelny(wszystkie, wybrane, przesuniecie);
			assertEquals(wszystkie, wynik.getLista());
			assertTrue(wynik.getPlan().getOperacje().isEmpty());
		}
	}

	@Test
	void skrajnePrzesuniecieDociskaObaBlokiDoWlasciwegoKonca() throws PrzesuwanieBrakElementuException {
		var dociskajace = obsluga(TrybPrzesuwania.DOCISKAJACE);

		assertEquals(List.of(1, 3, 5, 2, 4), dociskajace.przesunListe(wszystkie, wybrane, Integer.MAX_VALUE));
		assertEquals(List.of(2, 4, 1, 3, 5), dociskajace.przesunListe(wszystkie, wybrane, Integer.MIN_VALUE));
	}

	@Test
	void ruchCyklicznyZawijaSkrajnePrzesuniecia() throws PrzesuwanieBrakElementuException {
		var cykliczne = obsluga(TrybPrzesuwania.CYKLICZNE);

		var lista = List.of(1, 2, 3, 4, 5, 6);
		assertEquals(List.of(1, 3, 2, 5, 4, 6), cykliczne.przesunListe(lista, wybrane, Integer.MAX_VALUE));
		assertEquals(List.of(1, 4, 3, 5, 6, 2), cykliczne.przesunListe(lista, wybrane, Integer.MIN_VALUE));
	}

	@Test
	void przesunWGoreOdwracaKierunekTakzeDlaNajmniejszegoInt() throws PrzesuwanieBrakElementuException {
		var cykliczne = obsluga(TrybPrzesuwania.CYKLICZNE);

		assertEquals(List.of(1, 4, 3, 5, 2), cykliczne.przesunWGore(wszystkie, wybrane, Integer.MIN_VALUE));
	}

	@Test
	void brakZrodlaOdpowiadaPustejLiscie() throws Exception {
		var api = obsluga(TrybPrzesuwania.CYKLICZNE);
		assertFalse(api.czyDoPrzesunieciaDojdzie(null, null, 1));
		assertEquals(List.of(), api.przesunListe(null, null, 1));
		assertTrue(api.przesunPelny(null, List.of(), 1).getPlan().getOperacje().isEmpty());
		assertThrows(PrzesuwanieBrakElementuException.class, () -> api.przesunPelny(null, List.of(1), 0));
	}

	@Test
	void wynikChroniKolejnoscIPlanPrzedPozniejszaZmiana() {
		var lista = new ArrayList<>(Arrays.asList(1, null));
		var plan = new PrzesuwaniePlan();
		plan.dodaj(1, 1, 1);
		var wynik = new PrzesuwanieWynik<>(lista, plan);
		lista.clear();
		plan.getOperacje().clear();
		assertEquals(Arrays.asList(1, null), wynik.getLista());
		assertEquals(1, wynik.getPlan().getOperacje().size());
		assertThrows(UnsupportedOperationException.class, () -> wynik.getLista().clear());
		assertThrows(UnsupportedOperationException.class, () -> wynik.getPlan().getOperacje().clear());
		assertThrows(UnsupportedOperationException.class, () -> wynik.getPlan().dodaj(1, 1, 2));
	}

	@Test
	void samaListaJestEdytowalnaICzytaLinkedListSekwencyjnie() throws Exception {
		var zrodlo = new LinkedList<Integer>(wszystkie) {
			@Override public Integer get(int i) { throw new AssertionError("Odczyt przez indeks " + i); }
		};
		var lista = obsluga(TrybPrzesuwania.LINIOWE).przesunListe(zrodlo, List.of(2), 1);
		assertEquals(List.of(1, 3, 2, 4, 5), lista);
		lista.clear();
		assertEquals(wszystkie, zrodlo);
	}

	@Test
	void widoczneLpMuszaBycDodatnieUnikalneIPosortowaneTakzePrzyBrakuRuchu() {
		var api = obsluga(TrybPrzesuwania.LINIOWE);
		for (var lp : List.of(List.of(0, 3), List.of(3, 3), List.of(5, 3))) {
			assertThrows(IllegalArgumentException.class, () -> api.przesunPelny(lp, List.of(), 0, i -> i));
		}
	}

	@Test
	void ustawiaOryginalneLpWidokuTakzePrzyGranicyInt() throws Exception {
		var lp = List.of(1, 3, Integer.MAX_VALUE);
		var wynik = obsluga(TrybPrzesuwania.LINIOWE).przesunPelny(lp, List.of(3), 1, i -> i);
		var przypisania = new ArrayList<List<Integer>>();
		wynik.ustawLp((id, numer) -> przypisania.add(List.of(id, numer)));
		assertEquals(List.of(List.of(1, 1), List.of(Integer.MAX_VALUE, 3), List.of(3, Integer.MAX_VALUE)), przypisania);
	}
}
