package pl.com.razi.listy.przesuwanie;

import java.sql.Connection;
import java.sql.SQLException;

import pl.com.razi.listy.przesuwanie.wynik.TabelaSql;

/** Przykład odczytu definicji listy z katalogu SQL Server przed wygenerowaniem planu SQL. */
public final class PrzykladKolumnSqlServer {
	private PrzykladKolumnSqlServer() {}

	public record Definicja(TabelaSql tabela, String kolumnaGrupy) {}

	/** idTabeli to sys.tables.object_id tabeli dopuszczonej przez aplikację w bieżącej bazie. */
	public static Definicja zKatalogu(Connection baza, int idTabeli, String nazwaLp, String nazwaGrupy)
			throws SQLException {
		String sql = """
				SELECT s.name AS schemat, t.name AS tabela, p.name AS lp, g.name AS grupa,
				       p.column_id AS id_lp, g.column_id AS id_grupy
				FROM sys.tables AS t
				JOIN sys.schemas AS s ON s.schema_id = t.schema_id
				JOIN sys.columns AS p ON p.object_id = t.object_id
				JOIN sys.columns AS g ON g.object_id = t.object_id
				WHERE t.object_id = ? AND p.name = ? AND g.name = ?
				""";
		try (var pytanie = baza.prepareStatement(sql)) {
			pytanie.setInt(1, idTabeli);
			pytanie.setNString(2, nazwaLp);
			pytanie.setNString(3, nazwaGrupy);
			try (var wynik = pytanie.executeQuery()) {
				if (!wynik.next()) {
					throw new IllegalArgumentException("Tabela lub kolumna jest niedostępna w katalogu.");
				}
				if (wynik.getInt("id_lp") == wynik.getInt("id_grupy")) {
					throw new IllegalArgumentException("LP i grupa wskazują tę samą kolumnę.");
				}
				var tabela = new TabelaSql(wynik.getString("schemat"), wynik.getString("tabela"), wynik.getString("lp"));
				return new Definicja(tabela, wynik.getString("grupa"));
			}
		}
	}
}
