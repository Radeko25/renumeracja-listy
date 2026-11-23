package pl.com.razi.listy.przesuwanie.wynik;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import pl.com.razi.listy.przesuwanie.wynik.PrzesuwaniePlan.DialektSql;

/**
 * Fizyczne nazwy obiektów w katalogu bazy, w ich dokładnej pisowni, bez
 * cudzysłowów SQL. Schemat jest opcjonalny. Kropka w nazwie jest jej częścią.
 * Aplikacja wybiera tabelę i kolumny, do których użytkownik ma prawo zapisu.
 */
public record TabelaSql(String schemat, String nazwa, String kolumnaLp) {

	public TabelaSql(String nazwa, String kolumnaLp) {
		this(null, nazwa, kolumnaLp);
	}

	record Cel(String tabela, String lp, String warunek) {}

	Cel celGrupy(DialektSql dialekt, String[] kolumnyGrupy) {
		if (kolumnyGrupy == null || kolumnyGrupy.length == 0) {
			throw new IllegalArgumentException("Podaj kolumny grupy. Dla tabeli z jedną listą użyj wariantu DlaCalejTabeli.");
		}
		return cel(dialekt, kolumnyGrupy);
	}

	Cel cel(DialektSql dialekt, String... kolumnyGrupy) {
		Objects.requireNonNull(dialekt, "dialekt");
		String tabela = cytuj(dialekt, nazwa);
		String kwalifikowana = schemat == null ? tabela : cytuj(dialekt, schemat) + "." + tabela;
		String lp = cytujKolumne(dialekt, kolumnaLp);
		// Nazwy z katalogu mają dokładną pisownię. SQLite utożsamia też warianty liter ASCII.
		var nazwaLp = Pattern.compile(kolumnaLp, Pattern.LITERAL | (dialekt == DialektSql.SQLITE ? Pattern.CASE_INSENSITIVE : 0));
		var warunek = new StringJoiner(" AND ");
		for (String kolumna : kolumnyGrupy) {
			String nazwaKolumny = cytujKolumne(dialekt, kolumna);
			sprawdzKolumneGrupy(nazwaLp, kolumna);
			// Kwalifikacja wyklucza interpretację nieznanej kolumny jako literału w SQLite.
			warunek.add(tabela + "." + nazwaKolumny + " = ?");
		}
		return new Cel(kwalifikowana, lp, warunek.toString());
	}

	private static void sprawdzKolumneGrupy(Pattern nazwaLp, String kolumnaGrupy) {
		if (nazwaLp.matcher(kolumnaGrupy).matches()) {
			throw new IllegalArgumentException("Kolumna LP nie może być kolumną grupy.");
		}
	}

	private static String cytujKolumne(DialektSql dialekt, String nazwa) {
		if (dialekt == DialektSql.ORACLE && "ROWID".equals(nazwa)) {
			throw new IllegalArgumentException("Oracle zastrzega nazwę kolumny ROWID.");
		}
		return cytuj(dialekt, nazwa);
	}

	private static String cytuj(DialektSql dialekt, String nazwa) {
		if (nazwa == null || nazwa.isBlank() || nazwa.indexOf('\0') >= 0
				|| !StandardCharsets.UTF_8.newEncoder().canEncode(nazwa)) {
			throw new IllegalArgumentException("Podaj niepustą nazwę SQL zapisaną w Unicode bez znaku NUL.");
		}
		if (dialekt == DialektSql.SQL_SERVER) {
			if (nazwa.length() > 128) {
				throw new IllegalArgumentException("Nazwa SQL Server przekracza 128 jednostek UTF-16.");
			}
			return "[" + nazwa.replace("]", "]]") + "]";
		}
		if (dialekt == DialektSql.POSTGRESQL && nazwa.getBytes(StandardCharsets.UTF_8).length > 63) {
			throw new IllegalArgumentException("Nazwa PostgreSQL przekracza 63 bajty UTF-8.");
		}
		if (dialekt == DialektSql.ORACLE && (nazwa.indexOf('"') >= 0 || nazwa.getBytes(StandardCharsets.UTF_8).length > 128)) {
			throw new IllegalArgumentException("Oracle wymaga nazwy bez cudzysłowu o długości do 128 bajtów UTF-8.");
		}
		return "\"" + nazwa.replace("\"", "\"\"") + "\"";
	}

	// Dawne sygnatury zachowują pisownię i domyślne reguły wielkości liter
	// dla prostych nazw, np. Dokumenty w PostgreSQL i Oracle.
	private static final String NAZWA = "[A-Za-z_][A-Za-z_0-9]*+";
	private static final Pattern KOLUMNA = Pattern.compile(NAZWA);
	private static final Pattern TABELA = Pattern.compile(NAZWA + "(?:\\." + NAZWA + ")*+");
	private static final Pattern ROWNOSC = Pattern.compile(
			"(" + NAZWA + ")\\s*+=\\s*+(\\?|[+-]?[0-9]++)(?:\\s++(AND|OR)\\s++|\\z)", Pattern.CASE_INSENSITIVE);

	static Cel zProstegoZapisu(String tabela, String lp, String where) {
		if (tabela == null || !TABELA.matcher(tabela).matches() || lp == null || !KOLUMNA.matcher(lp).matches()) {
			throw new IllegalArgumentException("Dla nazw niestandardowych użyj TabelaSql i wybierz dialekt.");
		}
		String nazwaTabeli = tabela.substring(tabela.lastIndexOf('.') + 1);
		String tekst = where == null ? "" : where.strip();
		var rownosc = ROWNOSC.matcher(tekst);
		var nazwaLp = Pattern.compile(lp, Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
		var warunek = new StringBuilder();
		int od = 0;
		while (od < tekst.length()) {
			if (!rownosc.region(od, tekst.length()).lookingAt()) {
				throw new IllegalArgumentException("Podaj prosty warunek grupy lub użyj TabelaSql z kolumnami grupy.");
			}
			String kolumna = rownosc.group(1);
			sprawdzKolumneGrupy(nazwaLp, kolumna);
			String wartosc = rownosc.group(2);
			String spojnik = rownosc.group(3);
			if (!wartosc.equals("?") && (od != 0 || spojnik != null)) {
				throw new IllegalArgumentException("W warunku z kilkoma porównaniami użyj parametrów JDBC.");
			}
			warunek.append(nazwaTabeli).append('.').append(kolumna).append(" = ").append(wartosc);
			if (spojnik != null) {
				warunek.append(spojnik.equalsIgnoreCase("AND") ? " AND " : " OR ");
			}
			od = rownosc.end();
		}
		return new Cel(tabela, lp, warunek.toString());
	}
}
