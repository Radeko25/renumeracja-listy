package pl.com.razi.listy.przesuwanie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Porównuje normalizację dowolnej kolejności z zaufaną ścieżką wejścia. */
public class PrzesuwanieKolejnoscWybranychBenchmark {

	private static final int ROZMIAR_LISTY = 1_000_000;
	private static final int ILOSC_WYBRANYCH = 50_000;
	private static final int WARMUP = 10;
	private static final int POMIARY = 50;

	public static void main(String[] args) throws Exception {
		List<Integer> wszystkie = new ArrayList<>(ROZMIAR_LISTY);
		for (int i = 0; i < ROZMIAR_LISTY; i++) wszystkie.add(i);

		List<Integer> uporzadkowane = new ArrayList<>(ILOSC_WYBRANYCH);
		for (int i = 0; i < ILOSC_WYBRANYCH; i++) uporzadkowane.add(i * 2);

		List<Integer> odwrocone = new ArrayList<>(uporzadkowane);
		Collections.reverse(odwrocone);

		benchmark("normalizacja dowolnej kolejności", wszystkie, odwrocone, true);
		benchmark("zaufana kolejność źródłowa", wszystkie, uporzadkowane, false);
	}

	private static void benchmark(String nazwa, List<Integer> wszystkie, List<Integer> wybrane, boolean normalizuj)
			throws Exception {
		for (int i = 0; i < WARMUP; i++) PrzesuwanieWejscie.zbudujBlokiWybranych(wszystkie, wybrane, normalizuj);
		long suma = 0;
		for (int i = 0; i < POMIARY; i++) {
			long start = System.nanoTime();
			PrzesuwanieWejscie.zbudujBlokiWybranych(wszystkie, wybrane, normalizuj);
			suma += System.nanoTime() - start;
		}
		System.out.printf("%-32s: %.3f ms%n", nazwa, (suma / (double) POMIARY) / 1_000_000.0);
	}
}
