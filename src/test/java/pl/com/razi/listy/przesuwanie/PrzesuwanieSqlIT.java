package pl.com.razi.listy.przesuwanie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import pl.com.razi.listy.przesuwanie.PrzesuwanieObsluga.TrybPrzesuwania;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan.DialektSql;
import pl.com.razi.listy.przesuwanie.wynik.TabelaSql;

/** Profil Maven testy-baz uruchamia te same operacje na czterech silnikach. */
class PrzesuwanieSqlIT {
	private record Wiersz(int id, int lp) {}
	private record Widok(long wersja, List<Wiersz> wiersze) {}

	@ParameterizedTest(name = "{0} – pełna lista, widok, UNIQUE i wycofanie transakcji")
	@EnumSource(DialektSql.class)
	void zapisujeTylkoPrzesunieteRekordy(DialektSql dialekt) throws Exception {
		String tabela = "rn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		try (var baza = polacz(dialekt); var ddl = baza.createStatement()) {
			System.out.println(dialekt + " | " + baza.getMetaData().getDatabaseProductVersion());
			ddl.executeUpdate("CREATE TABLE " + tabela + " (id INTEGER PRIMARY KEY, grupa INTEGER NOT NULL,"
					+ " lp INTEGER NOT NULL, widoczny INTEGER NOT NULL, UNIQUE (grupa, lp), CHECK (id <> 1 OR lp <> 2))");
			try {
				try (var insert = baza.prepareStatement("INSERT INTO " + tabela + " VALUES (?, ?, ?, ?)")) {
					for (int grupa : List.of(3, 9)) {
						for (int lp = 1; lp <= 5; lp++) {
							insert.setInt(1, grupa == 3 ? lp : lp + 10);
							insert.setInt(2, grupa);
							insert.setInt(3, lp);
							insert.setInt(4, lp % 2);
							insert.executeUpdate();
						}
					}
				}
				baza.setAutoCommit(false);
				var pelna = odczytaj(baza, tabela, 3, false);
				var widok = odczytaj(baza, tabela, 3, true);
				var obcaGrupa = odczytaj(baza, tabela, 9, false);
				var api = new PrzesuwanieObsluga.Builder<Wiersz>().build();

				var plan = api.przesunPelny(pelna, pelna.subList(1, 3), 1).getPlan();
				sprawdzRuch(baza, dialekt, tabela, plan, List.of(1, 4, 2, 3, 5), 3, obcaGrupa);

				plan = api.przesunPelny(widok, List.of(widok.get(2)), -1, Wiersz::lp).getPlan();
				sprawdzRuch(baza, dialekt, tabela, plan, List.of(1, 2, 5, 4, 3), 2, obcaGrupa);

				api.ustawTrybPrzesuwania(TrybPrzesuwania.DOCISKAJACE);
				plan = api.przesunPelny(widok, List.of(widok.get(0), widok.get(2)), 20, Wiersz::lp).getPlan();
				sprawdzRuch(baza, dialekt, tabela, plan, List.of(3, 2, 1, 4, 5), 2, obcaGrupa);

				api.ustawTrybPrzesuwania(TrybPrzesuwania.CYKLICZNE);
				plan = api.przesunPelny(widok, List.of(widok.get(2)), 1, Wiersz::lp).getPlan();
				sprawdzRuch(baza, dialekt, tabela, plan, List.of(5, 2, 1, 4, 3), 3, obcaGrupa);

				var pusty = api.przesunPelny(widok, widok, 0, Wiersz::lp).getPlan();
				assertEquals(List.of(), pusty.toSql(dialekt, tabela, "lp", "grupa = ?"));

				// CHECK odrzuci dopiero docelową zmianę rekordu 1 na LP 2.
				var odrzucony = api.przesunPelny(pelna, List.of(pelna.get(0)), 1).getPlan();
				var polecenia = odrzucony.toSql(dialekt, tabela, "lp", "grupa = ?");
				for (int i = 0; i < polecenia.size() - 1; i++) {
					assertEquals(2, wykonaj(baza, polecenia.get(i), 3));
				}
				assertThrows(SQLException.class, () -> wykonaj(baza, polecenia.get(polecenia.size() - 1), 3));
				baza.rollback();
				assertEquals(pelna, odczytaj(baza, tabela, 3, false));
				assertEquals(obcaGrupa, odczytaj(baza, tabela, 9, false));
			} finally {
				if (!baza.getAutoCommit()) {
					baza.rollback();
					baza.setAutoCommit(true);
				}
				ddl.executeUpdate("DROP TABLE " + tabela);
			}
		}
	}

	@ParameterizedTest(name = "{0} – fizyczne nazwy użytkownika i wartości grupy w JDBC")
	@EnumSource(DialektSql.class)
	void renumerujeWskazanaListeWTabeliDefiniowanej(DialektSql dialekt) throws SQLException {
		String nazwa = "Spis." + UUID.randomUUID().toString().substring(0, 8) + " A?";
		boolean sqlServer = dialekt == DialektSql.SQL_SERVER;
		String tabela = sqlServer ? "[" + nazwa + "]" : "\"" + nazwa + "\"";
		String nazwaLp = switch (dialekt) {
		case SQL_SERVER -> "Numer] pozycji";
		case ORACLE -> "Numer pozycji";
		default -> "Numer \"pozycji\"";
		};
		String lp = switch (dialekt) {
		case SQL_SERVER -> "[Numer]] pozycji]";
		case ORACLE -> "\"Numer pozycji\"";
		default -> "\"Numer \"\"pozycji\"\"\"";
		};
		String grupa = sqlServer ? "[Order]" : "\"Order\"";
		String projekt = sqlServer ? "[Id projektu]" : "\"Id projektu\"";
		String mojZespol = "O'Connor";
		try (var baza = polacz(dialekt); var ddl = baza.createStatement()) {
			String schemat = dialekt == DialektSql.SQLITE ? "main" : baza.getSchema();
			var definicja = new TabelaSql(schemat, nazwa, nazwaLp);
			ddl.executeUpdate("CREATE TABLE " + tabela + " (id INTEGER PRIMARY KEY, " + grupa + " VARCHAR(40) NOT NULL, "
					+ projekt + " INTEGER NOT NULL, " + lp + " INTEGER NOT NULL, UNIQUE (" + grupa + ", " + projekt + ", " + lp + "))");
			try {
				try (var dodaj = baza.prepareStatement("INSERT INTO " + tabela + " VALUES (?, ?, ?, ?)")) {
					for (int id = 1; id <= 6; id++) {
						dodaj.setInt(1, id);
						dodaj.setString(2, id <= 4 ? mojZespol : "Inny zespół");
						dodaj.setInt(3, id == 3 || id == 4 ? 2 : 1);
						dodaj.setInt(4, id % 2 == 1 ? 1 : 2);
						dodaj.executeUpdate();
					}
				}
				baza.setAutoCommit(false);
				var plan = new PrzesuwaniePlan();
				plan.dodaj(1, 1, 1);
				plan.dodaj(2, 2, -1);
				for (String sql : plan.toSql(dialekt, definicja, "Order", "Id projektu")) {
					try (var zapis = baza.prepareStatement(sql)) {
						zapis.setString(1, mojZespol);
						zapis.setInt(2, 1);
						assertEquals(2, zapis.executeUpdate());
					}
				}
				try (var odczyt = ddl.executeQuery("SELECT " + lp + " FROM " + tabela + " ORDER BY id")) {
					var pozycje = new ArrayList<Integer>();
					while (odczyt.next()) {
						pozycje.add(odczyt.getInt(1));
					}
					assertEquals(List.of(2, 1, 1, 2, 1, 2), pozycje);
				}
				baza.rollback();
				// Nieaktualna definicja kolumny grupy musi zakończyć zapis błędem.
				String brakKolumny = plan.toSql(dialekt, definicja, "Dawna nazwa").get(0);
				assertThrows(SQLException.class, () -> {
					try (var zapis = baza.prepareStatement(brakKolumny)) {
						zapis.setString(1, "Dawna nazwa");
						zapis.executeUpdate();
					}
				});
			} finally {
				if (!baza.getAutoCommit()) {
					baza.rollback();
					baza.setAutoCommit(true);
				}
				ddl.executeUpdate("DROP TABLE " + tabela);
			}
		}
	}

	@Test
	void sqlServerRozpoznajeKolumnyWedlugKatalogu() throws SQLException {
		String nazwa = "Spis." + UUID.randomUUID().toString().substring(0, 8);
		String cel = "[" + nazwa + "]";
		try (var baza = polacz(DialektSql.SQL_SERVER); var ddl = baza.createStatement()) {
			ddl.executeUpdate("CREATE TABLE " + cel + " (id int PRIMARY KEY, [lp] int NOT NULL)");
			try {
				int idTabeli;
				boolean rozrozniaWielkoscLiter;
				try (var pytanie = baza.prepareStatement("""
						SELECT id, COLUMNPROPERTY(id, N'LP', 'ColumnId'),
						       CAST(DATABASEPROPERTYEX(DB_NAME(), 'Collation') AS nvarchar(128))
						FROM (SELECT OBJECT_ID(?, N'U') AS id) AS t
						""")) {
					pytanie.setNString(1, cel);
					try (var meta = pytanie.executeQuery()) {
						assertTrue(meta.next());
						idTabeli = meta.getInt(1);
						assertFalse(meta.wasNull());
						rozrozniaWielkoscLiter = meta.getObject(2) == null;
						System.out.println("SQL Server | kolacja: " + meta.getString(3) + " | rozróżnia lp i LP: " + rozrozniaWielkoscLiter);
					}
				}
				var blad = assertThrows(IllegalArgumentException.class,
						() -> PrzykladKolumnSqlServer.zKatalogu(baza, idTabeli, "lp", "LP"));
				assertEquals(rozrozniaWielkoscLiter ? "Tabela lub kolumna jest niedostępna w katalogu."
						: "LP i grupa wskazują tę samą kolumnę.", blad.getMessage());

				String grupa = rozrozniaWielkoscLiter ? "LP" : "Id listy";
				ddl.executeUpdate("ALTER TABLE " + cel + " ADD [" + grupa + "] int NOT NULL");
				ddl.executeUpdate("ALTER TABLE " + cel + " ADD UNIQUE ([" + grupa + "], [lp])");
				ddl.executeUpdate("INSERT INTO " + cel + " VALUES (1,1,10), (2,2,10), (3,1,20), (4,2,20)");
				var definicja = PrzykladKolumnSqlServer.zKatalogu(baza, idTabeli,
						rozrozniaWielkoscLiter ? "lp" : "LP", rozrozniaWielkoscLiter ? "LP" : "ID LISTY");
				assertEquals(new TabelaSql(baza.getSchema(), nazwa, "lp"), definicja.tabela());
				assertEquals(grupa, definicja.kolumnaGrupy());
				assertThrows(IllegalArgumentException.class, () -> PrzykladKolumnSqlServer.zKatalogu(baza, idTabeli, "lp", "lp"));
				assertThrows(IllegalArgumentException.class, () -> PrzykladKolumnSqlServer.zKatalogu(baza, idTabeli, "lp", "Brak kolumny"));

				var plan = new PrzesuwaniePlan();
				plan.dodaj(1, 1, 1);
				plan.dodaj(2, 2, -1);
				baza.setAutoCommit(false);
				for (String sql : plan.toSql(DialektSql.SQL_SERVER, definicja.tabela(), definicja.kolumnaGrupy())) {
					assertEquals(2, wykonaj(baza, sql, 10));
				}
				try (var wynik = ddl.executeQuery("SELECT [lp] FROM " + cel + " ORDER BY id")) {
					var pozycje = new ArrayList<Integer>();
					while (wynik.next()) {
						pozycje.add(wynik.getInt(1));
					}
					assertEquals(List.of(2, 1, 1, 2), pozycje);
				}
			} finally {
				if (!baza.getAutoCommit()) {
					baza.rollback();
					baza.setAutoCommit(true);
				}
				ddl.executeUpdate("DROP TABLE " + cel);
			}
		}
	}

	@ParameterizedTest(name = "{0} – dwie sesje przeciągają ten sam rekord z wersji 7")
	@EnumSource(DialektSql.class)
	void tylkoJednaSesjaZatwierdzaRuch(DialektSql dialekt, @TempDir Path katalog) throws Exception {
		String plik = katalog.resolve("listy.db").toString();
		String nazwa = "rn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		var tabela = new TabelaSql(nazwa, "lp");
		String elementy = "\"" + nazwa + "\"";
		String listy = nazwa + "_l";
		String sqlWersji = "UPDATE " + listy + " SET wersja = wersja + 1 WHERE id = ? AND wersja = ?";
		try (var kontrola = polacz(dialekt, plik); var ddl = kontrola.createStatement()) {
			ddl.executeUpdate("CREATE TABLE " + listy + " (id INTEGER PRIMARY KEY, wersja INTEGER NOT NULL)");
			try {
				ddl.executeUpdate("CREATE TABLE " + elementy + " (id INTEGER PRIMARY KEY, \"id_listy\" INTEGER NOT NULL,"
						+ " \"lp\" INTEGER NOT NULL, widoczny INTEGER NOT NULL, UNIQUE (\"id_listy\", \"lp\"),"
						+ " CHECK (id <> 1 OR \"lp\" <> 2))");
				try {
					ddl.executeUpdate("INSERT INTO " + listy + " VALUES (1, 7)");
					ddl.executeUpdate("INSERT INTO " + listy + " VALUES (2, 11)");
					try (var dodaj = kontrola.prepareStatement("INSERT INTO " + elementy + " VALUES (?, ?, ?, ?)")) {
						for (int id = 1; id <= 6; id++) {
							dodaj.setInt(1, id);
							dodaj.setInt(2, id <= 4 ? 1 : 2);
							dodaj.setInt(3, id <= 4 ? id : id - 4);
							dodaj.setInt(4, id == 3 ? 0 : 1);
							dodaj.executeUpdate();
						}
					}
					String widok = "SELECT l.wersja, e.id, e.\"lp\" FROM " + listy + " l JOIN " + elementy
							+ " e ON e.\"id_listy\" = l.id WHERE l.id = ?";
					var start = new CyclicBarrier(2);
					var pula = Executors.newFixedThreadPool(2);
					try {
						var pierwsza = pula.submit(() -> ruchUzytkownika(dialekt, plik, tabela, widok, sqlWersji, start));
						var druga = pula.submit(() -> ruchUzytkownika(dialekt, plik, tabela, widok, sqlWersji, start));
						assertNotEquals(pierwsza.get(40, TimeUnit.SECONDS), druga.get(40, TimeUnit.SECONDS));
					} finally {
						pula.shutdownNow();
						assertTrue(pula.awaitTermination(15, TimeUnit.SECONDS));
					}
					var poRuchu = new Widok(8, List.of(new Wiersz(1, 1), new Wiersz(4, 2), new Wiersz(3, 3), new Wiersz(2, 4)));
					assertEquals(poRuchu, odczytajWidok(kontrola, widok, 1));
					assertEquals(new Widok(11, List.of(new Wiersz(5, 1), new Wiersz(6, 2))), odczytajWidok(kontrola, widok, 2));

					// CHECK odrzuca ruch po zwiększeniu wersji, a na PostgreSQL i SQLite po pierwszym UPDATE pozycji.
					var odrzucony = new PrzesuwaniePlan();
					odrzucony.dodaj(1, 1, 1);
					odrzucony.dodaj(2, 2, -1);
					try (var zapis = polacz(dialekt, plik); var wersja = zapis.prepareStatement(sqlWersji)) {
						zapis.setAutoCommit(false);
						assertThrows(SQLException.class, () -> PrzykladZapisuJdbc.zapisz(wersja, 1, 8, odrzucony, dialekt, tabela));
						assertEquals(poRuchu, odczytajWidok(zapis, widok, 1));
						zapis.rollback();
					}
				} finally {
					ddl.executeUpdate("DROP TABLE " + elementy);
				}
			} finally {
				ddl.executeUpdate("DROP TABLE " + listy);
			}
		}
	}

	private boolean ruchUzytkownika(DialektSql dialekt, String plik, TabelaSql tabela, String widok, String sqlWersji,
			CyclicBarrier start) throws Exception {
		try (var baza = polacz(dialekt, plik); var wersja = baza.prepareStatement(sqlWersji)) {
			var odczyt = odczytajWidok(baza, widok + " AND e.widoczny = 1", 1);
			assertEquals(new Widok(7, List.of(new Wiersz(1, 1), new Wiersz(2, 2), new Wiersz(4, 4))), odczyt);
			var obsluga = new PrzesuwanieObsluga.Builder<Wiersz>().build();
			var plan = obsluga.przesunPelny(odczyt.wiersze(), List.of(odczyt.wiersze().get(2)), -1, Wiersz::lp).getPlan();
			start.await(20, TimeUnit.SECONDS);
			baza.setAutoCommit(false);
			return PrzykladZapisuJdbc.zapisz(wersja, 1, odczyt.wersja(), plan, dialekt, tabela);
		}
	}

	private Widok odczytajWidok(Connection baza, String sql, int idListy) throws SQLException {
		try (var zapytanie = baza.prepareStatement(sql + " ORDER BY e.\"lp\"")) {
			zapytanie.setInt(1, idListy);
			try (var dane = zapytanie.executeQuery()) {
				assertTrue(dane.next());
				long wersja = dane.getLong(1);
				var wiersze = new ArrayList<Wiersz>();
				do {
					wiersze.add(new Wiersz(dane.getInt(2), dane.getInt(3)));
				} while (dane.next());
				return new Widok(wersja, wiersze);
			}
		}
	}

	@Test
	void sqlitePrzesuwaPonadTysiacRozdzielonychPozycji() throws Exception {
		try (var baza = polacz(DialektSql.SQLITE); var polecenie = baza.createStatement()) {
			baza.setAutoCommit(false);
			polecenie.executeUpdate("CREATE TABLE lista (id INTEGER PRIMARY KEY, grupa INTEGER, lp INTEGER UNIQUE, widoczny INTEGER)");
			polecenie.executeUpdate("WITH RECURSIVE numery(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM numery WHERE n < 2200)"
					+ " INSERT INTO lista SELECT n, 1, n, n % 2 FROM numery");
			var widoczne = odczytaj(baza, "lista", 1, true);
			var oczekiwane = odczytaj(baza, "lista", 1, false);
			var api = new PrzesuwanieObsluga.Builder<Wiersz>().trybPrzesuwania(TrybPrzesuwania.CYKLICZNE).build();
			var ostatni = widoczne.get(widoczne.size() - 1);
			var plan = api.przesunPelny(widoczne, List.of(ostatni), 1, Wiersz::lp).getPlan();
			for (String sql : plan.toSql(DialektSql.SQLITE, "lista", "lp", "grupa = ?")) {
				assertEquals(widoczne.size(), wykonaj(baza, sql, 1));
			}
			// Ostatni widoczny wraca na początek, a każdy kolejny zajmuje następne widoczne LP.
			var poprzedni = ostatni;
			for (Wiersz miejsce : widoczne) {
				oczekiwane.set(miejsce.lp() - 1, new Wiersz(poprzedni.id(), miejsce.lp()));
				poprzedni = miejsce;
			}
			assertEquals(oczekiwane, odczytaj(baza, "lista", 1, false));
			baza.rollback();
		}
	}

	private void sprawdzRuch(Connection baza, DialektSql dialekt, String tabela, PrzesuwaniePlan plan,
			List<Integer> ids, int liczbaZmienionych, List<Wiersz> obcaGrupa) throws SQLException {
		var sql = plan.toSql(dialekt, tabela, "lp", "grupa = ? OR grupa = ?");
		int liczbaInstrukcji = switch (dialekt) {
		case POSTGRESQL, SQLITE -> 2;
		case ORACLE, SQL_SERVER -> 1;
		};
		assertEquals(liczbaInstrukcji, sql.size());
		for (String polecenie : sql) {
			assertEquals(liczbaZmienionych, wykonaj(baza, polecenie, 3, 99));
		}
		var oczekiwane = new ArrayList<Wiersz>();
		for (int i = 0; i < ids.size(); i++) {
			oczekiwane.add(new Wiersz(ids.get(i), i + 1));
		}
		assertEquals(oczekiwane, odczytaj(baza, tabela, 3, false));
		assertEquals(obcaGrupa, odczytaj(baza, tabela, 9, false));
		baza.rollback();
	}

	private int wykonaj(Connection baza, String sql, int... parametry) throws SQLException {
		try (var zapytanie = baza.prepareStatement(sql)) {
			for (int i = 0; i < parametry.length; i++) {
				zapytanie.setInt(i + 1, parametry[i]);
			}
			return zapytanie.executeUpdate();
		}
	}

	private List<Wiersz> odczytaj(Connection baza, String tabela, int grupa, boolean tylkoWidoczne) throws SQLException {
		String sql = "SELECT id, lp FROM " + tabela + " WHERE grupa = ?" + (tylkoWidoczne ? " AND widoczny = 1" : "")
				+ " ORDER BY lp";
		try (var zapytanie = baza.prepareStatement(sql)) {
			zapytanie.setInt(1, grupa);
			try (var dane = zapytanie.executeQuery()) {
				var lista = new ArrayList<Wiersz>();
				while (dane.next()) {
					lista.add(new Wiersz(dane.getInt(1), dane.getInt(2)));
				}
				return lista;
			}
		}
	}

	private Connection polacz(DialektSql dialekt) throws SQLException {
		return polacz(dialekt, ":memory:");
	}

	private Connection polacz(DialektSql dialekt, String plikSqlite) throws SQLException {
		if (dialekt == DialektSql.SQLITE) {
			return DriverManager.getConnection("jdbc:sqlite:" + plikSqlite);
		}
		String prefix = "RENUM_" + dialekt.name() + "_";
		return DriverManager.getConnection(wymagana(prefix + "URL"), wymagana(prefix + "USER"), wymagana(prefix + "PASSWORD"));
	}

	private String wymagana(String nazwa) {
		String wartosc = System.getenv(nazwa);
		if (wartosc == null || wartosc.isBlank()) {
			throw new IllegalStateException("Ustaw zmienną testową " + nazwa);
		}
		return wartosc;
	}
}
