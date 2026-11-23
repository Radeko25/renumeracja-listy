package pl.com.razi.listy.przesuwanie;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan.DialektSql;
import pl.com.razi.listy.przesuwanie.wynik.TabelaSql;

/** Przykład dla aplikacji, w której elementy listy łączy kolumna „id_listy”. */
public final class PrzykladZapisuJdbc {
	private PrzykladZapisuJdbc() {}

	/**
	 * Na osobnym połączeniu z wyłączonym autocommitem zatwierdza lub wycofuje cały ruch.
	 * Polecenie wersji ma dwa parametry: ID listy i wersję odczytaną z widokiem.
	 * Zwraca false, gdy lista z tą wersją już nie istnieje. Pusty plan pomija zapis.
	 */
	public static boolean zapisz(PreparedStatement podniesWersje, long idListy, long wersjaWidoku,
			PrzesuwaniePlan plan, DialektSql dialekt, TabelaSql tabela) throws SQLException {
		var baza = podniesWersje.getConnection();
		if (baza.getAutoCommit()) {
			throw new IllegalStateException("Zapis kolejności wymaga wyłączonego autocommitu.");
		}
		try {
			var polecenia = plan.toSql(dialekt, tabela, "id_listy");
			if (polecenia.isEmpty()) {
				return true;
			}
			podniesWersje.setLong(1, idListy);
			podniesWersje.setLong(2, wersjaWidoku);
			int liczbaList = podniesWersje.executeUpdate();
			if (liczbaList == 0) {
				baza.rollback();
				return false;
			}
			if (liczbaList != 1) {
				throw new SQLException("Identyfikator musi wskazywać jedną listę.");
			}
			long oczekiwane = plan.getOperacje().stream().mapToLong(op -> (long) op.lpDo - op.lpOd + 1).sum();
			for (String sql : polecenia) {
				try (var zapis = baza.prepareStatement(sql)) {
					zapis.setLong(1, idListy);
					if (zapis.executeUpdate() != oczekiwane) {
						throw new SQLException("Liczba zmienionych pozycji odbiega od planu.");
					}
				}
			}
			baza.commit();
			return true;
		} catch (SQLException | RuntimeException blad) {
			try {
				baza.rollback();
			} catch (SQLException bladWycofania) {
				blad.addSuppressed(bladWycofania);
			}
			throw blad;
		}
	}
}
