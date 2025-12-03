package pl.com.razi.listy.przesuwanie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Benchmark wydajnościowy operacji przebudowy listy (sortowanie po finalLp).
 *
 * Testowane scenariusze: - IDENTITY — brak zmian LP, minimalne sortowanie -
 * PERM_RANDOM — pełna losowa permutacja (ciężki przypadek) - SHIFT_BLOCKS —
 * przesunięcia blokowe (średni przypadek)
 */
public class PrzesuwaniePrzebudujListeBenchmark {

	// ============================================================
	// ===================== KONFIGURACJA =========================
	// ============================================================

	private static final int ROZMIAR_LISTY = 1_000_000;
	private static final int POWTORZENIA_POMIARU = 20;
	private static final int POWTORZENIA_WARMUP = 8;

	private final Random random = new Random(777);

	// ============================================================
	// ====================== START BENCHMARKU ====================
	// ============================================================

	public static void main(String[] args) {
		new PrzesuwaniePrzebudujListeBenchmark().uruchomBenchmark();
	}

	private void uruchomBenchmark() {

		System.out.println("=== BENCHMARK przebudujListe() ===");

		// źródłowa lista 1..N
		List<Integer> wszystkie = new ArrayList<>(ROZMIAR_LISTY);
		for (int i = 1; i <= ROZMIAR_LISTY; i++) {
			wszystkie.add(i);
		}

		// trzy scenariusze danych
		benchmarkDlaScenariusza("IDENTITY — minimalna praca sortowania", wszystkie, zbudujIdentity());

		benchmarkDlaScenariusza("PERM_RANDOM — pełna losowa permutacja", wszystkie, zbudujPermRandom());

		benchmarkDlaScenariusza("SHIFT_BLOCKS — długie przesunięcia blokowe", wszystkie, zbudujShiftBlocks());
	}

	// ============================================================
	// ===================== GENERATORY DANYCH ====================
	// ============================================================

	/** Scenariusz najlżejszy — LP bez zmian. */
	private int[] zbudujIdentity() {
		int[] arr = new int[ROZMIAR_LISTY];
		for (int i = 1; i <= ROZMIAR_LISTY; i++) {
			arr[i - 1] = i;
		}
		return arr;
	}

	/** Trudny przypadek — pełna permutacja losowa. */
	private int[] zbudujPermRandom() {
		List<Integer> tmp = new ArrayList<>(ROZMIAR_LISTY);
		for (int i = 1; i <= ROZMIAR_LISTY; i++) {
			tmp.add(i);
		}

		Collections.shuffle(tmp, random);

		int[] arr = new int[ROZMIAR_LISTY];
		for (int i = 0; i < ROZMIAR_LISTY; i++) {
			arr[i] = tmp.get(i);
		}
		return arr;
	}

	/** Przesunięcia blokowe — średni przypadek. */
	private int[] zbudujShiftBlocks() {
		int[] arr = new int[ROZMIAR_LISTY];
		int blockSize = 20_000;

		int lp = 1;
		while (lp <= ROZMIAR_LISTY) {

			int offset = ((lp / blockSize) % 2 == 0) ? 5 : -7;

			for (int i = 0; i < blockSize && lp <= ROZMIAR_LISTY; i++, lp++) {
				int newLp = lp + offset;

				if (newLp < 1)
					newLp = 1;
				if (newLp > ROZMIAR_LISTY)
					newLp = ROZMIAR_LISTY;

				arr[lp - 1] = newLp;
			}
		}
		return arr;
	}

	// ============================================================
	// ========================= BENCHMARK =========================
	// ============================================================

	private void benchmarkDlaScenariusza(String opis, List<Integer> wszystkie, int[] finalLp) {

		System.out.println("\nScenariusz: " + opis);

		// --- Warm-up (JIT optymalizuje kod) ---
		for (int i = 0; i < POWTORZENIA_WARMUP; i++) {
			PrzesuwanieObliczenia.przebudujListe(finalLp, wszystkie);
		}

		long sumaNanos = 0;

		// --- właściwy pomiar ---
		for (int i = 0; i < POWTORZENIA_POMIARU; i++) {

			long start = System.nanoTime();
			List<Integer> wynik = PrzesuwanieObliczenia.przebudujListe(finalLp, wszystkie);
			long end = System.nanoTime();

			sumaNanos += (end - start);

			// minimalna walidacja — uniemożliwia optymalizację dead code
			if (wynik.isEmpty()) {
				throw new IllegalStateException("Wynik nie może być pusty.");
			}
		}

		double avgMs = (sumaNanos / (double) POWTORZENIA_POMIARU) / 1_000_000.0;
		System.out.println("Średni czas: " + avgMs + " ms");
	}

}
