package pl.com.razi.listy.przesuwanie;

import java.util.ArrayList;
import java.util.List;

import pl.com.razi.listy.przesuwanie.PrzesuwanieObsluga.TrybPrzesuwania;
import pl.com.razi.listy.przesuwanie.model.PrzesuwanieIndeksyBlok;

/**
 * Benchmark wydajnościowy metody permutacjaDocelowa().
 *
 * Zakres testu: - lista 1 000 000 pozycji LP, - 50 000 elementów wybranych w
 * blokach po 10, - trzy tryby przesuwania, - pełny warm-up + wielokrotne
 * powtórzenia pomiaru.
 */
public class PrzesuwaniePermutacjaBenchmark {

	// =====================================================================
	// ======================= KONFIGURACJA TESTU ===========================
	// =====================================================================

	private static final int ROZMIAR_LISTY = 1_000_000;
	private static final int ILOSC_WYBRANE = 50_000;
	private static final int POWTORZENIA_POMIARU = 30;
	private static final int POWTORZENIA_WARMUP = 10;

	private static final int WARTOSC_PRZESUNIECIA = 5;

	// =====================================================================
	// ========================= METODA GŁÓWNA ==============================
	// =====================================================================

	public static void main(String[] args) {
		new PrzesuwaniePermutacjaBenchmark().uruchomBenchmark();
	}

	private void uruchomBenchmark() {

		System.out.println("=== BENCHMARK permutacjaDocelowa() ===");

		// --------------------------------------------------------
		// Przygotowanie bloków elementów wybranych
		// --------------------------------------------------------
		List<PrzesuwanieIndeksyBlok> bloki = przygotujBlokiWybranych();

		// --------------------------------------------------------
		// Benchmark dla trzech trybów przesuwania
		// --------------------------------------------------------
		benchmarkTryb(TrybPrzesuwania.LINIOWE, bloki);
		benchmarkTryb(TrybPrzesuwania.CYKLICZNE, bloki);
		benchmarkTryb(TrybPrzesuwania.DOCISKAJACE, bloki);
	}

	// =====================================================================
	// =================== PRZYGOTOWANIE BLOKÓW WYBRANYCH ===================
	// =====================================================================

	/**
	 * Buduje listę 50 000 wybranych elementów w blokach po 10, równomiernie
	 * rozmieszczonych na milionowej liście.
	 */
	private List<PrzesuwanieIndeksyBlok> przygotujBlokiWybranych() {

		List<PrzesuwanieIndeksyBlok> bloki = new ArrayList<>();

		int krok = ROZMIAR_LISTY / ILOSC_WYBRANE;
		int start = 1;

		for (int i = 0; i < ILOSC_WYBRANE; i += 10) {

			int blockStart = start;
			int blockEnd = blockStart + 9;

			if (blockEnd > ROZMIAR_LISTY)
				break;

			bloki.add(new PrzesuwanieIndeksyBlok(blockStart, blockEnd));

			start += krok;
		}

		return bloki;
	}

	// =====================================================================
	// ========================== BENCHMARK TRYBU ===========================
	// =====================================================================

	private void benchmarkTryb(TrybPrzesuwania tryb, List<PrzesuwanieIndeksyBlok> bloki) {

		System.out.println("\nTRYB: " + tryb);

		// -------------------------
		// Warm-up (JIT optymalizuje)
		// -------------------------
		for (int i = 0; i < POWTORZENIA_WARMUP; i++) {
			PrzesuwanieObliczenia.obliczDocelowaPermutacje(tryb, bloki, ROZMIAR_LISTY, WARTOSC_PRZESUNIECIA);
		}

		long sumaNanos = 0;

		// -------------------------
		// Faktyczny pomiar czasu
		// -------------------------
		for (int i = 0; i < POWTORZENIA_POMIARU; i++) {

			long tStart = System.nanoTime();

			int[] wynik = PrzesuwanieObliczenia.obliczDocelowaPermutacje(tryb, bloki, ROZMIAR_LISTY, WARTOSC_PRZESUNIECIA);

			long tEnd = System.nanoTime();
			sumaNanos += (tEnd - tStart);

			// check przeciwko optymalizacji JIT
			if (wynik.length != ROZMIAR_LISTY) {
				throw new IllegalStateException("Niepoprawna długość tablicy wynikowej!");
			}
		}

		double avgMs = (sumaNanos / (double) POWTORZENIA_POMIARU) / 1_000_000.0;

		System.out.println("Średni czas: " + avgMs + " ms");
	}

}
