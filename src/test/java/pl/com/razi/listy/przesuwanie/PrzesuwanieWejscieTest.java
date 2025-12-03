package pl.com.razi.listy.przesuwanie;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.com.razi.listy.przesuwanie.util.PrzesuwanieFactory.blok;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import pl.com.razi.listy.przesuwanie.model.PrzesuwanieIndeksyBlok;
import pl.com.razi.listy.przesuwanie.wyjatki.PrzesuwanieBrakElementuException;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class PrzesuwanieWejscieTest {

	// ============================================================
	// walidacjaDanych
	// ============================================================

	@Test
	void walidacjaDanych_wybraneNaLiscie() {
		List<Integer> wszystkie = Arrays.asList(1, 2, 3, 4);
		List<Integer> wybrane = Arrays.asList(2, 4);

		assertDoesNotThrow(() -> PrzesuwanieWejscie.walidacjaDanych(wszystkie, wybrane));
	}

	@Test
	void walidacjaDanych_wybranePuste() {
		List<Integer> wszystkie = Arrays.asList(1, 2, 3);

		assertDoesNotThrow(() -> PrzesuwanieWejscie.walidacjaDanych(wszystkie, Arrays.asList()));
	}

	@Test
	void walidacjaDanych_wybraneNull() {
		List<Integer> wszystkie = Arrays.asList(1, 2, 3);

		assertDoesNotThrow(() -> PrzesuwanieWejscie.walidacjaDanych(wszystkie, null));
	}

	@Test
	void walidacjaDanych_wszystkieNullWybraneNull() {
		assertDoesNotThrow(() -> PrzesuwanieWejscie.walidacjaDanych(null, null));
	}

	@Test
	void walidacjaDanych_wszystkieNullWybranePuste() {
		assertDoesNotThrow(() -> PrzesuwanieWejscie.walidacjaDanych(null, Arrays.asList()));
	}

	@Test
	void walidacjaDanych_wszystkieNullIWybraneNiepuste_rzucaWyjatek() {
		List<Integer> wybrane = Arrays.asList(1);

		assertThrows(PrzesuwanieBrakElementuException.class, () -> PrzesuwanieWejscie.walidacjaDanych(null, wybrane));
	}

	@Test
	void walidacjaDanych_brakWybranegoNaLiscie_wyjatek() {
		List<Integer> wszystkie = Arrays.asList(1, 2, 3);
		List<Integer> wybrane = Arrays.asList(3, 99);

		assertThrows(PrzesuwanieBrakElementuException.class, () -> PrzesuwanieWejscie.walidacjaDanych(wszystkie, wybrane));
	}

	// ============================================================
	// czyParametryPrzesunieciaPoprawne
	// ============================================================

	@Test
	void czyParametryPrzesunieciaPoprawne_poprawneDane_true() {
		assertTrue(PrzesuwanieWejscie.czyParametryPrzesuwaniaPoprawne(Arrays.asList(1, 2, 3), Arrays.asList(2), 1));
	}

	@Test
	void czyParametryPrzesunieciaPoprawne_wszystkieNull_false() {
		assertFalse(PrzesuwanieWejscie.czyParametryPrzesuwaniaPoprawne(null, Arrays.asList(1), 1));
	}

	@Test
	void czyParametryPrzesunieciaPoprawne_wszystkiePuste_false() {
		assertFalse(PrzesuwanieWejscie.czyParametryPrzesuwaniaPoprawne(Arrays.asList(), Arrays.asList(1), 1));
	}

	@Test
	void czyParametryPrzesunieciaPoprawne_wybraneNull_false() {
		assertFalse(PrzesuwanieWejscie.czyParametryPrzesuwaniaPoprawne(Arrays.asList(1, 2), null, 1));
	}

	@Test
	void czyParametryPrzesunieciaPoprawne_wybranePuste_false() {
		assertFalse(PrzesuwanieWejscie.czyParametryPrzesuwaniaPoprawne(Arrays.asList(1, 2), Arrays.asList(), 1));
	}

	@Test
	void czyParametryPrzesunieciaPoprawne_przesuniecieZero_false() {
		assertFalse(PrzesuwanieWejscie.czyParametryPrzesuwaniaPoprawne(Arrays.asList(1, 2), Arrays.asList(1), 0));
	}

	// ============================================================
	// zbudujBlokiWybranych
	// ============================================================

	@Test
	void zbudujBlokiWybranych_prosteBloki() {
		List<Integer> wszystkie = Arrays.asList(10, 20, 30, 40, 50, 60);
		List<Integer> wybrane = Arrays.asList(20, 30, 50);

		List<PrzesuwanieIndeksyBlok> bloki = PrzesuwanieWejscie.zbudujBlokiWybranych(wszystkie, wybrane);

		assertEquals(2, bloki.size());
		assertEquals(blok(2, 3), bloki.get(0));
		assertEquals(blok(5, 5), bloki.get(1));
	}

	@Test
	void zbudujBlokiWybranych_jedenCiągłyBlok() {
		List<Integer> wszystkie = Arrays.asList(1, 2, 3, 4, 5);
		List<Integer> wybrane = Arrays.asList(2, 3, 4);

		List<PrzesuwanieIndeksyBlok> bloki = PrzesuwanieWejscie.zbudujBlokiWybranych(wszystkie, wybrane);

		assertEquals(1, bloki.size());
		assertEquals(blok(2, 4), bloki.get(0));
	}

	@Test
	void zbudujBlokiWybranych_wybranePuste() {
		List<PrzesuwanieIndeksyBlok> bloki = PrzesuwanieWejscie.zbudujBlokiWybranych(Arrays.asList(1, 2, 3),
				Arrays.asList());

		assertTrue(bloki.isEmpty());
	}

	@Test
	void zbudujBlokiWybranych_wybraneNull() {
		List<PrzesuwanieIndeksyBlok> bloki = PrzesuwanieWejscie.zbudujBlokiWybranych(Arrays.asList(1, 2, 3), null);

		assertTrue(bloki.isEmpty());
	}

	@Test
	void zbudujBlokiWybranych_wszystkieNull() {
		List<PrzesuwanieIndeksyBlok> bloki = PrzesuwanieWejscie.zbudujBlokiWybranych(null, Arrays.asList(1, 2));

		assertTrue(bloki.isEmpty());
	}

	@Test
	void zbudujBlokiWybranych_wszystkieNullWybraneNull() {
		List<PrzesuwanieIndeksyBlok> bloki = PrzesuwanieWejscie.zbudujBlokiWybranych(null, null);

		assertTrue(bloki.isEmpty());
	}

}
