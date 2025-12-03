package pl.com.razi.listy.przesuwanie;

import java.util.ArrayList;
import java.util.List;

import pl.com.razi.listy.przesuwanie.PrzesuwanieObsluga.TrybPrzesuwania;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwanieWynik;

/**
 * Benchmark wydajnościowy pełnej operacji przesunięcia na danych typu Integer.
 */
public class PrzesuwaniePelnaOperacjaBenchmark {

	// ================================================================
	// ================== KONFIGURACJA BENCHMARKU =====================
	// ================================================================

	private static final int ROZMIAR_LISTY = 1_000_000;
	private static final int ILOSC_WYBRANE = 50_000;

	private static final int WARMUP = 4;
	private static final int POWTORZENIA_POMIARU = 12;

	private static final int PRZESUNIECIE = 5;

	// ================================================================
	// ======================= START BENCHMARKU =======================
	// ================================================================

	public static void main(String[] args) throws Exception {
		new PrzesuwaniePelnaOperacjaBenchmark().uruchomBenchmark();
	}

	private void uruchomBenchmark() throws Exception {
		System.out.println("=== BENCHMARK przesunPelny() — Integer ===");

		// ---------------------------
		// lista "wszystkie": 1..N
		// ---------------------------
		List<Integer> wszystkie = new ArrayList<>(ROZMIAR_LISTY);
		for (int i = 1; i <= ROZMIAR_LISTY; i++) {
			wszystkie.add(i);
		}

		// ---------------------------
		// lista "wybrane": co 20
		// ---------------------------
		List<Integer> wybrane = new ArrayList<>(ILOSC_WYBRANE);
		for (int i = 1; i <= ILOSC_WYBRANE; i++) {
			wybrane.add(i * 20);
		}

		// ---------------------------
		// benchmark dla trybów
		// ---------------------------
		benchmarkTryb(TrybPrzesuwania.LINIOWE, wszystkie, wybrane);
		benchmarkTryb(TrybPrzesuwania.CYKLICZNE, wszystkie, wybrane);
		benchmarkTryb(TrybPrzesuwania.DOCISKAJACE, wszystkie, wybrane);
	}

	// ================================================================
	// ===================== BENCHMARK TRYBU =========================
	// ================================================================

	private void benchmarkTryb(TrybPrzesuwania tryb, List<Integer> wszystkie, List<Integer> wybrane) throws Exception {

		System.out.println("\nTRYB: " + tryb);

		PrzesuwanieObsluga<Integer> obsluga = new PrzesuwanieObsluga.Builder<Integer>().trybPrzesuwania(tryb).build();

		// ---------------------------
		// 1. WARM-UP
		// ---------------------------
		for (int i = 0; i < WARMUP; i++) {
			obsluga.przesunPelny(wszystkie, wybrane, PRZESUNIECIE);
		}

		// ---------------------------
		// 2. POMIAR
		// ---------------------------
		long sumaNanos = 0;

		for (int i = 0; i < POWTORZENIA_POMIARU; i++) {

			long start = System.nanoTime();
			PrzesuwanieWynik<Integer> wynik = obsluga.przesunPelny(wszystkie, wybrane, PRZESUNIECIE);
			long end = System.nanoTime();

			sumaNanos += (end - start);

			// check poprawności
			if (wynik.getLista().size() != wszystkie.size()) {
				throw new IllegalStateException("Zły rozmiar listy wynikowej!");
			}
		}

		double avgMs = (sumaNanos / (double) POWTORZENIA_POMIARU) / 1_000_000.0;

		System.out.println("Średni czas pełnej operacji: " + avgMs + " ms");
	}

}
