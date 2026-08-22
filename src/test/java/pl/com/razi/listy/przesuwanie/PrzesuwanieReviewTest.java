package pl.com.razi.listy.przesuwanie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pl.com.razi.listy.przesuwanie.PrzesuwanieObsluga.TrybPrzesuwania;
import pl.com.razi.listy.przesuwanie.wyjatki.PrzesuwanieBrakElementuException;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan.PrzesuwanieOperacja;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwanieWynik;

class PrzesuwanieReviewTest {

	@Test
	void domyslneApiNormalizujeNieuporzadkowaneWybrane() throws Exception {
		PrzesuwanieObsluga<Integer> obsluga = new PrzesuwanieObsluga.Builder<Integer>().build();
		assertEquals(Arrays.asList(1, 3, 2, 4, 6, 5),
				obsluga.przesunWDolOJeden(Arrays.asList(1, 2, 3, 4, 5, 6), Arrays.asList(5, 2)));
	}

	@Test
	void szybkaSciezkaWymagaKolejnosciZrodlowej() throws Exception {
		PrzesuwanieObsluga<Integer> obsluga = new PrzesuwanieObsluga.Builder<Integer>()
				.normalizujKolejnoscWybranych(false).build();
		List<Integer> wszystkie = Arrays.asList(1, 2, 3, 4, 5, 6);

		assertEquals(Arrays.asList(1, 3, 2, 4, 6, 5),
				obsluga.przesunWDolOJeden(wszystkie, Arrays.asList(2, 5)));
		assertThrows(IllegalArgumentException.class,
				() -> obsluga.przesunWDolOJeden(wszystkie, Arrays.asList(5, 2)));
	}

	@Test
	void walidacjaUwzgledniaLiczbeWystapien() throws Exception {
		PrzesuwanieWejscie.walidacjaDanych(Arrays.asList(1, 2, 2, 3), Arrays.asList(2, 2));
		assertThrows(PrzesuwanieBrakElementuException.class,
				() -> PrzesuwanieWejscie.walidacjaDanych(Arrays.asList(1, 2, 3), Arrays.asList(2, 2)));
	}

	@Test
	void skrajnePrzesunieciaNiePrzepelniajaInt() throws Exception {
		List<Integer> wszystkie = Arrays.asList(1, 2, 3, 4, 5);
		List<Integer> wybrane = Arrays.asList(3);

		PrzesuwanieObsluga<Integer> liniowe = obsluga(TrybPrzesuwania.LINIOWE);
		assertEquals(wszystkie, liniowe.przesunListe(wszystkie, wybrane, Integer.MAX_VALUE));
		assertEquals(wszystkie, liniowe.przesunListe(wszystkie, wybrane, Integer.MIN_VALUE));

		PrzesuwanieObsluga<Integer> dociskajace = obsluga(TrybPrzesuwania.DOCISKAJACE);
		assertEquals(Arrays.asList(1, 2, 4, 5, 3),
				dociskajace.przesunListe(wszystkie, wybrane, Integer.MAX_VALUE));
		assertEquals(Arrays.asList(3, 1, 2, 4, 5),
				dociskajace.przesunListe(wszystkie, wybrane, Integer.MIN_VALUE));
	}

	@Test
	void metodyKierunkoweNieAkceptujaUjemnejOdleglosci() {
		PrzesuwanieObsluga<Integer> obsluga = new PrzesuwanieObsluga.Builder<Integer>().build();
		assertThrows(IllegalArgumentException.class,
				() -> obsluga.przesunWGore(Arrays.asList(1, 2, 3), Arrays.asList(2), -1));
		assertThrows(IllegalArgumentException.class,
				() -> obsluga.przesunWDol(Arrays.asList(1, 2, 3), Arrays.asList(2), -1));
	}

	@Test
	void nullowyNoOpZwracaPustyWynik() throws Exception {
		PrzesuwanieWynik<Integer> wynik = new PrzesuwanieObsluga.Builder<Integer>().build()
				.przesunPelny(null, null, 1);
		assertTrue(wynik.getLista().isEmpty());
		assertTrue(wynik.getPlan().isPusty());
	}

	@Test
	void planScalaMaksymalneCiagiStalegoOffsetu() {
		int[] permutacja = { 3, 4, 1, 2, 5, 8, 6, 7 };
		List<PrzesuwanieOperacja> operacje = PrzesuwanieObliczenia.generujPlan(permutacja, permutacja.length)
				.getOperacje();
		assertEquals(4, operacje.size());
		assertOperacja(operacje.get(0), 1, 2, 2);
		assertOperacja(operacje.get(1), 3, 4, -2);
		assertOperacja(operacje.get(2), 6, 6, 2);
		assertOperacja(operacje.get(3), 7, 8, -1);
	}

	@Test
	void pustyPlanNieGenerujeNiepoprawnegoSql() {
		assertEquals("", new PrzesuwaniePlan().toSqlCaseWhenBloki("tabela", "lp", null));
	}

	@Test
	void listaOperacjiPlanuNieJestModyfikowalnaZZewnatrz() {
		PrzesuwaniePlan plan = new PrzesuwaniePlan();
		plan.dodaj(1, 1, 1);
		assertThrows(UnsupportedOperationException.class, () -> plan.getOperacje().clear());
	}

	@Test
	void testWlasnosciowyWszystkichTrybow() throws Exception {
		for (int n = 1; n <= 8; n++) {
			List<Integer> wszystkie = lista1DoN(n);
			for (int maska = 1; maska < (1 << n); maska++) {
				List<Integer> wybrane = wybraneZMaski(n, maska);
				for (int przesuniecie = -2 * n; przesuniecie <= 2 * n; przesuniecie++) {
					if (przesuniecie == 0) continue;
					for (TrybPrzesuwania tryb : TrybPrzesuwania.values()) {
						PrzesuwanieWynik<Integer> wynik = new PrzesuwanieObsluga.Builder<Integer>()
								.trybPrzesuwania(tryb).normalizujKolejnoscWybranych(false).build()
								.przesunPelny(wszystkie, wybrane, przesuniecie);
						sprawdzPermutacje(wszystkie, wynik.getLista());
						sprawdzPlanMinimalny(wynik);
					}
				}
			}
		}
	}

	private static void sprawdzPermutacje(List<Integer> wszystkie, List<Integer> wynik) {
		assertEquals(wszystkie.size(), wynik.size());
		Set<Integer> zbior = new HashSet<>(wynik);
		assertEquals(wszystkie.size(), zbior.size());
		assertTrue(zbior.containsAll(wszystkie));
	}

	private static void sprawdzPlanMinimalny(PrzesuwanieWynik<Integer> wynik) {
		List<Integer> lista = wynik.getLista();
		int n = lista.size();
		int[] nowaPozycja = new int[n + 1];
		for (int i = 0; i < n; i++) nowaPozycja[lista.get(i)] = i + 1;

		int[] delta = new int[n + 1];
		int runs = 0;
		boolean active = false;
		int prev = 0;
		for (int oldLp = 1; oldLp <= n; oldLp++) {
			delta[oldLp] = nowaPozycja[oldLp] - oldLp;
			if (delta[oldLp] == 0) { active = false; prev = 0; continue; }
			if (!active || delta[oldLp] != prev) runs++;
			active = true;
			prev = delta[oldLp];
		}

		List<PrzesuwanieOperacja> operacje = wynik.getPlan().getOperacje();
		assertEquals(runs, operacje.size());
		boolean[] pokryte = new boolean[n + 1];
		int poprzedniKoniec = 0;
		for (PrzesuwanieOperacja op : operacje) {
			assertTrue(op.lpOd > poprzedniKoniec);
			poprzedniKoniec = op.lpDo;
			for (int oldLp = op.lpOd; oldLp <= op.lpDo; oldLp++) {
				assertFalse(pokryte[oldLp]);
				pokryte[oldLp] = true;
				assertEquals(delta[oldLp], op.offset);
			}
		}
		for (int oldLp = 1; oldLp <= n; oldLp++) assertEquals(delta[oldLp] != 0, pokryte[oldLp]);
	}

	private static PrzesuwanieObsluga<Integer> obsluga(TrybPrzesuwania tryb) {
		return new PrzesuwanieObsluga.Builder<Integer>().trybPrzesuwania(tryb).build();
	}

	private static List<Integer> lista1DoN(int n) {
		List<Integer> wynik = new ArrayList<>(n);
		for (int i = 1; i <= n; i++) wynik.add(i);
		return wynik;
	}

	private static List<Integer> wybraneZMaski(int n, int maska) {
		List<Integer> wybrane = new ArrayList<>();
		for (int i = 0; i < n; i++) if ((maska & (1 << i)) != 0) wybrane.add(i + 1);
		return wybrane;
	}

	private static void assertOperacja(PrzesuwanieOperacja op, int lpOd, int lpDo, int offset) {
		assertEquals(lpOd, op.lpOd);
		assertEquals(lpDo, op.lpDo);
		assertEquals(offset, op.offset);
	}
}
