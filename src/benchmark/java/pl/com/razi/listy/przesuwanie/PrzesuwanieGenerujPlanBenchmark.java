package pl.com.razi.listy.przesuwanie;

import java.util.Random;

import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan;

/**
 * Benchmark wydajnościowy operacji generowania planu przesunięcia.
 * 
 * Zakres pomiarów: - 1 000 000 elementów, - trzy zestawy danych o różnym
 * charakterze, - wielokrotne powtórzenia i pełen warm-up JIT.
 */
public class PrzesuwanieGenerujPlanBenchmark {

	// =====================================================================
	// ==================== KONFIGURACJA BENCHMARKU ========================
	// =====================================================================

	private static final int LICZBA_ELEMENTOW = 1_000_000;
	private static final int POWTORZENIA_POMIARU = 20;
	private static final int POWTORZENIA_WARMUP = 8;

	private final Random random = new Random(777);

	// =====================================================================
	// ======================= URUCHOMIENIE BENCHMARKU =====================
	// =====================================================================

	public static void main(String[] args) {
		new PrzesuwanieGenerujPlanBenchmark().uruchomBenchmark();
	}

	private void uruchomBenchmark() {

		System.out.println("=== BENCHMARK generujPlan() ===");

		// ------------------------------------------------------------
		// ZESTAW 1 – rzadkie przesunięcia
		// ------------------------------------------------------------
		wykonajBenchmarkDlaZestawu("RZADKIE PRZESUNIĘCIA (offsety sporadyczne)", zbudujRzadkieOffsety());

		// ------------------------------------------------------------
		// ZESTAW 2 – bardzo chaotyczne przesunięcia
		// ------------------------------------------------------------
		wykonajBenchmarkDlaZestawu("CHAOTYCZNE PRZESUNIĘCIA (częste zmiany kierunku)", zbudujChaotyczneOffsety());

		// ------------------------------------------------------------
		// ZESTAW 3 – duże grupy stabilnych przesunięć
		// ------------------------------------------------------------
		wykonajBenchmarkDlaZestawu("GRUPOWE PRZESUNIĘCIA (stabilne duże segmenty)", zbudujOffsetyZgrupowane());
	}

	// =====================================================================
	// ======================= GENERATORY DANYCH ===========================
	// =====================================================================

	/**
	 * Bardzo mało przesunięć – większość elementów pozostaje na swoich miejscach.
	 */
	private int[] zbudujRzadkieOffsety() {
		int[] arr = new int[LICZBA_ELEMENTOW];

		for (int lp = 1; lp <= LICZBA_ELEMENTOW; lp++) {
			arr[lp - 1] = (random.nextInt(200) == 0) ? lp + 1 : lp;
		}

		return arr;
	}

	/**
	 * Chaotyczne, częste zmiany offsetów — utrudniają kompresję planu.
	 */
	private int[] zbudujChaotyczneOffsety() {
		int[] arr = new int[LICZBA_ELEMENTOW];

		for (int lp = 1; lp <= LICZBA_ELEMENTOW; lp++) {
			int los = random.nextInt(5);

			if (los == 0) {
				arr[lp - 1] = lp + 3;
			} else if (los == 1) {
				arr[lp - 1] = lp - 3;
			} else {
				arr[lp - 1] = lp;
			}
		}

		return arr;
	}

	/**
	 * Stabilne przesunięcia w dużych blokach — idealne dla optymalizacji.
	 */
	private int[] zbudujOffsetyZgrupowane() {
		int[] arr = new int[LICZBA_ELEMENTOW];

		int aktualnyOffset = 0;

		for (int lp = 1; lp <= LICZBA_ELEMENTOW; lp++) {

			// co 50 000 elementów offset się zmienia
			if (lp % 50_000 == 0) {
				aktualnyOffset = random.nextInt(10) - 5;
			}

			arr[lp - 1] = lp + aktualnyOffset;
		}

		return arr;
	}

	// =====================================================================
	// =========================== BENCHMARK ===============================
	// =====================================================================

	private void wykonajBenchmarkDlaZestawu(String opis, int[] finalLp) {
		System.out.println("\nZESTAW DANYCH: " + opis);

		// Warm-up – rozgrzanie JVM i JIT
		for (int i = 0; i < POWTORZENIA_WARMUP; i++) {
			PrzesuwanieObliczenia.generujPlan(finalLp, LICZBA_ELEMENTOW);
		}

		long sumaCzasowNanos = 0;

		for (int i = 0; i < POWTORZENIA_POMIARU; i++) {
			long tStart = System.nanoTime();

			PrzesuwaniePlan plan = PrzesuwanieObliczenia.generujPlan(finalLp, LICZBA_ELEMENTOW);

			long tEnd = System.nanoTime();
			sumaCzasowNanos += (tEnd - tStart);

			if (plan.getOperacje().isEmpty()) {
				throw new IllegalStateException("Wygenerowano pusty plan — nie powinno się zdarzyć.");
			}
		}

		double sredniMs = (sumaCzasowNanos / (double) POWTORZENIA_POMIARU) / 1_000_000.0;

		System.out.println("Średni czas wykonania: " + sredniMs + " ms");
	}

}
