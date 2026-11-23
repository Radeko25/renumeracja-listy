package pl.com.razi.listy.przesuwanie;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan.DialektSql;
import pl.com.razi.listy.przesuwanie.wynik.TabelaSql;

class TabelaSqlTest {
	private final PrzesuwaniePlan plan = new PrzesuwaniePlan();

	TabelaSqlTest() {
		plan.dodaj(1, 1, 1);
	}

	@ParameterizedTest
	@EnumSource(DialektSql.class)
	void nazwyZKataloguIParametryWystepujaWTychSamychMiejscach(DialektSql dialekt) {
		var tabela = new TabelaSql("Schemat A", "Spis.1", "Numer pozycji");
		String cel = dialekt == DialektSql.SQL_SERVER ? "[Schemat A].[Spis.1]" : "\"Schemat A\".\"Spis.1\"";
		String filtr = dialekt == DialektSql.SQL_SERVER
				? "[Spis.1].[Order] = ? AND [Spis.1].[Id listy] = ?"
				: "\"Spis.1\".\"Order\" = ? AND \"Spis.1\".\"Id listy\" = ?";
		for (String sql : plan.toSql(dialekt, tabela, "Order", "Id listy")) {
			assertTrue(sql.startsWith("UPDATE " + cel + "\n"));
			assertTrue(sql.contains("WHERE (" + filtr + ") AND "));
			assertEquals(2L, sql.chars().filter(c -> c == '?').count());
		}
		assertTrue(plan.toSqlCaseWhenBloki(dialekt, tabela, "Order", "Id listy").contains(filtr));
		assertFalse(plan.toSqlCaseWhenBloki(dialekt, tabela).contains("?"));
		var pusty = new PrzesuwaniePlan();
		assertEquals(List.of(), pusty.toSql(dialekt, tabela, "Order"));
		assertEquals("", pusty.toSqlCaseWhenBloki(dialekt, tabela));
	}

	@Test
	void cytujeZnakiIdentyfikatoraZgodnieZDialektem() {
		var tabela = new TabelaSql("Arkusz \"A\"", "Nr] pozycji");
		for (var dialekt : List.of(DialektSql.POSTGRESQL, DialektSql.SQLITE)) {
			assertTrue(plan.toSql(dialekt, tabela).get(0).startsWith("UPDATE \"Arkusz \"\"A\"\"\"\nSET \"Nr] pozycji\""));
		}
		assertTrue(plan.toSql(DialektSql.SQL_SERVER, tabela).get(0).startsWith("UPDATE [Arkusz \"A\"]\nSET [Nr]] pozycji]"));
		assertThrows(IllegalArgumentException.class, () -> plan.toSql(DialektSql.ORACLE, tabela));
		assertThrows(IllegalArgumentException.class, () -> plan.toSql(DialektSql.ORACLE, new TabelaSql("Tabela", "ROWID")));
	}

	@ParameterizedTest
	@EnumSource(DialektSql.class)
	void odrzucaBlednaNazweTakzeWKolumnieGrupy(DialektSql dialekt) {
		for (String nazwa : List.of("", " ", "spis\0A", "\uD800")) {
			assertThrows(IllegalArgumentException.class, () -> plan.toSql(dialekt, new TabelaSql(nazwa, "lp")));
			assertThrows(IllegalArgumentException.class, () -> plan.toSql(dialekt, new TabelaSql(nazwa, "Spis", "lp")));
			assertThrows(IllegalArgumentException.class, () -> plan.toSql(dialekt, new TabelaSql("Spis", "lp"), nazwa));
		}
		assertThrows(IllegalArgumentException.class, () -> plan.toSql(dialekt, new TabelaSql("Spis", "lp"), (String) null));
		var tabela = new TabelaSql("Spis", "lp");
		assertThrows(IllegalArgumentException.class, () -> plan.toSql(dialekt, tabela, new String[0]));
		assertThrows(IllegalArgumentException.class, () -> plan.toSqlCaseWhenBloki(dialekt, tabela, new String[0]));
	}

	@Test
	void odrzucaZbytDlugieNazwyZamiastRyzykowacIchSkrocenie() {
		String pg = "ą".repeat(31) + "a";
		assertDoesNotThrow(() -> plan.toSql(DialektSql.POSTGRESQL, new TabelaSql(pg, "lp")));
		assertThrows(IllegalArgumentException.class, () -> plan.toSql(DialektSql.POSTGRESQL, new TabelaSql(pg + "a", "lp")));
		assertDoesNotThrow(() -> plan.toSql(DialektSql.ORACLE, new TabelaSql("ą".repeat(64), "lp")));
		assertThrows(IllegalArgumentException.class, () -> plan.toSql(DialektSql.ORACLE, new TabelaSql("ą".repeat(65), "lp")));
		assertDoesNotThrow(() -> plan.toSql(DialektSql.SQL_SERVER, new TabelaSql("a".repeat(128), "lp")));
		assertThrows(IllegalArgumentException.class, () -> plan.toSql(DialektSql.SQL_SERVER, new TabelaSql("a".repeat(129), "lp")));
	}

	@Test
	void starszeSygnaturyZachowujaProsteWywolaniaIOdrzucajaInneFragmentySql() {
		for (String filtr : List.of("grupa = 3", "grupa = ?", "grupa = ? OR grupa = ?", "grupa = ? AND typ = ?")) {
			String sql = plan.toSqlCaseWhenBloki("dbo.Dokumenty", "lp", filtr);
			assertTrue(sql.startsWith("UPDATE dbo.Dokumenty\nSET lp"));
			String kwalifikowany = filtr.replace("grupa", "Dokumenty.grupa").replace("typ", "Dokumenty.typ");
			assertTrue(sql.contains("WHERE (" + kwalifikowany + ") AND "));
			assertEquals(sql, plan.toSql(DialektSql.SQL_SERVER, "dbo.Dokumenty", "lp", filtr).get(0) + ";");
		}
		for (String filtr : List.of("UPPER(grupa) = ?", "grupa = ?;", "grupa = 3 AND typ = 2", "grupa =")) {
			assertThrows(IllegalArgumentException.class, () -> plan.toSqlCaseWhenBloki("Dokumenty", "lp", filtr));
			assertThrows(IllegalArgumentException.class, () -> plan.toSql(DialektSql.SQLITE, "Dokumenty", "lp", filtr));
		}
		assertThrows(IllegalArgumentException.class, () -> plan.toSqlCaseWhenBloki("Dokumenty d", "lp", null));
		assertThrows(IllegalArgumentException.class, () -> plan.toSql(DialektSql.ORACLE, "Dokumenty", "lp + 1", null));
	}
}
