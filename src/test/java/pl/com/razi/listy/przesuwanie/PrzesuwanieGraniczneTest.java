package pl.com.razi.listy.przesuwanie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import pl.com.razi.listy.przesuwanie.PrzesuwanieObsluga.TrybPrzesuwania;
import pl.com.razi.listy.przesuwanie.wyjatki.PrzesuwanieBrakElementuException;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan;

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
				+ "    WHEN lp BETWEEN 1 AND 1 THEN lp - 2147483648\n    ELSE lp\nEND;",
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
}
