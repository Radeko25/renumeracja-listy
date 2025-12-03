package pl.com.razi.listy.przesuwanie;

import java.util.ArrayList;
import java.util.List;

/**
 * Benchmark wydajnościowy operacji:
 * PrzesuwanieWejscie.zbudujBlokiWybranych(...)
 *
 * Test ma sprawdzić, - jak szybko budowane są bloki przy dużej liście, - jak
 * reaguje algorytm przy 50 000 wybranych elementów, - stabilność i
 * powtarzalność czasu po warm-up JIT.
 */
public class PrzesuwanieZbudujBlokiWybranychBenchmark {

	// ============================================================
	// ======================= KONFIGURACJA =======================
	// ============================================================

	private static final int ROZMIAR_LISTY = 1_000_000;
	private static final int ILOSC_WYBRANYCH = 50_000;

	private static final int POWTORZENIA_WARMUP = 10;
	private static final int POWTORZENIA_POMIARU = 50;

	// ============================================================
	// ======================= START TESTU =========================
	// ============================================================

	public static void main(String[] args) {
		new PrzesuwanieZbudujBlokiWybranychBenchmark().uruchomBenchmark();
	}

	private void uruchomBenchmark() {

		System.out.println("=== BENCHMARK zbudujBlokiWybranych() ===");

		// ---------------------------------------------------------
		// Przygotowanie danych wejściowych
		// ---------------------------------------------------------
		List<Integer> wszystkie = new ArrayList<>(ROZMIAR_LISTY);
		for (int i = 0; i < ROZMIAR_LISTY; i++) {
			wszystkie.add(i);
		}

		List<Integer> wybrane = new ArrayList<>(ILOSC_WYBRANYCH);
		for (int i = 0; i < ILOSC_WYBRANYCH; i++) {
			wybrane.add(i * 2); // elementy gwarantowanie istniejące na liście „wszystkie”
		}

		// ---------------------------------------------------------
		// Warm-up JVM (JIT optymalizuje kod)
		// ---------------------------------------------------------
		for (int i = 0; i < POWTORZENIA_WARMUP; i++) {
			PrzesuwanieWejscie.zbudujBlokiWybranych(wszystkie, wybrane);
		}

		long sumaNanos = 0;

		// ---------------------------------------------------------
		// Właściwy benchmark
		// ---------------------------------------------------------
		for (int i = 0; i < POWTORZENIA_POMIARU; i++) {

			long start = System.nanoTime();
			PrzesuwanieWejscie.zbudujBlokiWybranych(wszystkie, wybrane);
			long end = System.nanoTime();

			sumaNanos += (end - start);
		}

		double avgMs = (sumaNanos / (double) POWTORZENIA_POMIARU) / 1_000_000.0;

		System.out.println("Średni czas wykonania: " + avgMs + " ms");
	}

}
