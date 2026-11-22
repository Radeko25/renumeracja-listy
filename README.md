# Renumeracja listy

Biblioteka Java do przesuwania elementów listy w trzech trybach:

* liniowym – przesunięcie wykonalne tylko wtedy, gdy wszystkie elementy mogą zmienić pozycję,
* cyklicznym – elementy mogą przechodzić przez koniec/początek listy,
* dociskającym – przesunięcie wykonywane jest do momentu osiągnięcia granicy przez cały blok.

Biblioteka obsługuje również:

* wykrywanie bloków ciągłych indeksów,
* generowanie planu aktualizacji pozycji (np. dla SQL),
* przebudowę permutacji po wykonanej operacji przesunięcia.

## Najważniejsze pojęcia

**Blok indeksów**
Ciąg kolejnych wybranych pozycji, np. z [1, 2, 3, 7, 10, 11] powstają bloki:
`[1–3]`, `[7]`, `[10–11]`.

**Tryb przesuwania**

* *Liniowy*: brak możliwości „przeskoku”, ruch tylko jeśli wszystkie elementy mogą się przesunąć.
* *Cykliczny*: przeskakiwanie przez koniec listy jest dozwolone.
* *Dociskający*: ruch trwa dopóki blok nie osiągnie granicy.

Poprawiono obliczanie pozycji dla skrajnych przesunięć (`Integer.MIN_VALUE` i `Integer.MAX_VALUE`) we wszystkich trzech trybach.

## Kolejność zaznaczenia

Poprawiono przesuwanie elementów zaznaczonych w innej kolejności, niż występują na liście. We wszystkich trzech trybach wybór `[C, B]` na liście `[A, B, C, D, E]` daje ten sam wynik i plan zmian co `[B, C]`. Jeśli na liście są równe elementy, domyślnie wybierane są ich pierwsze wystąpienia w liczbie podanej w zaznaczeniu.

Jeśli tabela zwraca zaznaczone wiersze w kolejności listy źródłowej, można pominąć odtwarzanie tej kolejności, ustawiając w builderze `normalizujKolejnoscWybranych(false)`. Wtedy każde kolejne wystąpienie jest wyszukiwane za poprzednim. Błędna kolejność powoduje `IllegalArgumentException`. W obu wariantach brak potrzebnego wystąpienia powoduje `PrzesuwanieBrakElementuException`.

## Przykład użycia

```java
import java.util.List;
import pl.com.razi.listy.przesuwanie.PrzesuwanieObsluga;
import pl.com.razi.listy.przesuwanie.PrzesuwanieObsluga.TrybPrzesuwania;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwanieWynik;

public class Przyklad {
    public static void main(String[] args) throws Exception {
        var obsluga = new PrzesuwanieObsluga.Builder<String>()
                .trybPrzesuwania(TrybPrzesuwania.LINIOWE)
                .build();

        List<String> lista = List.of("A", "B", "C", "D", "E");
        PrzesuwanieWynik<String> wynik = obsluga.przesunPelny(
                lista, List.of("C", "B"), +1);

        System.out.println(wynik.getLista()); // [A, D, B, C, E]
        System.out.println(wynik.getPlan().toSqlCaseWhenBloki(
                "Dokumenty", "lp", "id_kategorii = 3"));
    }
}
```

Drugi argument zawiera wybrane elementy listy. Dodatnie przesunięcie oznacza ruch w dół, ujemne w górę. `przesunListe` zwraca edytowalną kopię i pomija obliczanie planu. `przesunPelny` zwraca listę i plan tylko do odczytu. Obie metody zachowują tożsamość obiektów. Lista źródłowa `null` jest traktowana jak pusta. Przy niepustym zaznaczeniu brak rekordu powoduje `PrzesuwanieBrakElementuException`.

## Przykład wygenerowanego planu

W powyższym przykładzie elementy B i C przesuwają się o jedną pozycję w dół, a D o dwie pozycje w górę. Plan opisuje te zmiany względem pozycji sprzed przesunięcia.

```text
lpOd = 2, lpDo = 3, offset = +1
lpOd = 4, lpDo = 4, offset = -2
```

Metoda `toSqlCaseWhenBloki` zapisuje oba zakresy w jednej instrukcji SQL. Dla pustego planu, np. po zablokowanym przesunięciu, zwraca pusty tekst (`""`).

```sql
UPDATE Dokumenty
SET lp = CASE
    WHEN lp BETWEEN 2 AND 3 THEN lp + 1
    WHEN lp BETWEEN 4 AND 4 THEN lp - 2
    ELSE lp
END
WHERE (id_kategorii = 3) AND ((lp BETWEEN 2 AND 3) OR (lp BETWEEN 4 AND 4));
```

### Widoczna część listy

W tabeli z pozycjami od 1 do 5 użytkownik widzi rekordy na 1, 3 i 5. Przesunięcie ostatniego o jedno miejsce w górę zamienia rekordy na 3 i 5. Pozycje 2 i 4 pozostają przy swoich rekordach. Do tej operacji wystarczy pobrać widoczne wiersze wraz z rzeczywistym LP, w kolejności rosnącej.

```java
record Wiersz(String nazwa, int lp) {}
var widoczne = List.of(new Wiersz("A", 1), new Wiersz("C", 3), new Wiersz("E", 5));
var obsluga = new PrzesuwanieObsluga.Builder<Wiersz>().build();
var wynik = obsluga.przesunPelny(widoczne, List.of(widoczne.get(2)), -1, Wiersz::lp);
var polecenia = wynik.getPlan().toSql(PrzesuwaniePlan.DialektSql.POSTGRESQL,
        "Dokumenty", "lp", "id_kategorii = ?");
```

`PrzesuwaniePlan` znajduje się w pakiecie `pl.com.razi.listy.przesuwanie.wynik`. Wariant z funkcją `Wiersz::lp` wymaga dodatnich, ściśle rosnących LP. W każdym z trzech trybów permutuje dokładnie te pozycje. Zachowuje początkową globalną numerację, również gdy zawiera ona luki. Wariant trzyargumentowy zakłada, że lista reprezentuje kolejne LP od 1. Dla mutowalnych rekordów `wynik.ustawLp(setter)` przypisuje końcowe LP z widoku. Przeciążenie `ustawLp(int, setter)` służy do jawnego nadania nowej, ciągłej numeracji.

### Obsługiwane bazy i sprawdzone wersje

`plan.toSql(dialekt, tabela, kolumnaLp, warunekGrupy)` zwraca kolejne instrukcje dla JDBC, bez końcowych średników. Dla pustego planu zwraca pustą listę. Wybór dialektu dotyczy wyłącznie zapisu. Starsza metoda `toSqlCaseWhenBloki` zwraca pojedynczy tekst z `CASE` do zastosowań, w których schemat dopuszcza taki zapis.

| Silnik | Dialekt | Wersja sprawdzona w testach JDBC | Zapis przy `UNIQUE (id_kategorii, lp)` |
| --- | --- | --- | --- |
| PostgreSQL | `POSTGRESQL` | 18.4 | Dwa `UPDATE` w jednej transakcji, przez tymczasowe `-lp`. |
| Microsoft SQL Server | `SQL_SERVER` | 2022 Developer (16.00.4325) | Jeden `UPDATE` z `CASE`. |
| Oracle Database | `ORACLE` | 26ai Free (23.26.4.0.0) | Jeden `UPDATE` z `CASE`. |
| SQLite | `SQLITE` | 3.54.1 | Dwa `UPDATE` w jednej transakcji, przez tymczasowe `-lp`. |

Na SQLite 3.53.4 sprawdzono dodatkowo przesunięcie w widoku z ukrytymi wierszami za pomocą klienta `sqlite3`. Inne wydania serwerów można sprawdzić tym samym profilem `testy-baz`, wskazując odpowiednie instancje. Wersja ma znaczenie zwłaszcza przy dużych planach. Przykładowo Oracle 10g R2 dopuszcza 255 argumentów jednego `CASE`, a 26ai dopuszcza 65 535. Każdy zakres planu zajmuje dwa argumenty, a końcowe `ELSE` jeden. [Oracle 10g R2](https://docs.oracle.com/cd/B19306_01/server.102/b14200/expressions004.htm) · [Oracle 26ai](https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CASE-Expressions.html).

### Warunki bezpiecznego zapisu

Zapisane LP są dodatnimi liczbami całkowitymi i są unikalne w obrębie listy. W PostgreSQL i SQLite odpowiadające im wartości ujemne muszą być wolne i dozwolone na czas obu instrukcji. `CHECK (lp > 0)` odrzuci pierwszy krok. Trigger audytujący zmiany LP może zarejestrować także wartość pośrednią, a reguły dostępu muszą pozwalać na zapis w obu krokach. PostgreSQL przy `UNIQUE DEFERRABLE INITIALLY IMMEDIATE` pozwala też zamienić pozycje jednym `UPDATE`. Można wtedy użyć `toSqlCaseWhenBloki`. [PostgreSQL o sprawdzaniu unikalności](https://www.postgresql.org/docs/18/sql-createtable.html#SQL-CREATETABLE-COMPATIBILITY).

Warunek grupy musi ograniczać zapis do jednej listy i obowiązywać przez całą operację, także po zmianie znaku LP. Można przekazać pusty warunek, gdy tabela stanowi jedną listę. Nazwy tabeli i kolumny oraz treść warunku pochodzą z zaufanego kodu aplikacji. Wartości użytkownika podaje się jako parametry `PreparedStatement`.

Wykonaj wszystkie instrukcje na jednym połączeniu z wyłączonym autocommitem. W każdej podstaw te same parametry. Liczba wierszy objętych każdą instrukcją powinna być sumą długości zakresów w planie. Błąd albo inna liczba wierszy wymaga wycofania całej transakcji. Plan wskazuje rekordy po LP. Jeśli inny użytkownik zdążył zamienić dwa rekordy, liczba zmienionych wierszy może się nadal zgadzać. Przy współbieżnej edycji wszyscy zapisujący powinni stosować wspólną blokadę listy od odczytu do zatwierdzenia albo atomowo sprawdzać i zwiększać wersję listy w tej samej transakcji co zapis. Po konflikcie wylicz plan ponownie z aktualnych danych.

### Testy

`mvn test` uruchamia testy algorytmu. `mvn -Ptesty-baz test` sprawdza dodatkowo zapis przez JDBC na PostgreSQL, SQL Server, Oracle i SQLite. Profil korzysta ze zmiennych `RENUM_<DIALEKT>_URL`, `RENUM_<DIALEKT>_USER` i `RENUM_<DIALEKT>_PASSWORD` dla trzech serwerów. SQLite działa w pamięci w wersji dostarczanej przez sterownik JDBC. Test tworzy własną tabelę z `UNIQUE`, odczytuje widok i sprawdza stan wszystkich rekordów oraz drugiej grupy po przesunięciu i po wycofaniu błędnego zapisu. Osobny test SQLite przesuwa rekordy na 1100 widocznych pozycjach rozdzielonych ukrytymi wierszami. [Test integracyjny](src/test/java/pl/com/razi/listy/przesuwanie/PrzesuwanieSqlIT.java).

## Typowe scenariusze

* przesuwanie pojedynczych elementów,
* przesuwanie wielu oddzielnych bloków jednocześnie,
* cykliczne rotowanie dużych list,
* dociskanie bloków do góry lub do dołu listy,
* generowanie permutacji po ruchu w celu późniejszego porównywania stanu listy.

## Licencja

Projekt udostępniany jest na licencji MIT.

## Pliki projektu

- [Biblioteka](src/main/java/pl/com/razi/listy/przesuwanie/) - publiczna obsługa przesuwania i obliczenia.
- [Testy JUnit](src/test/java/pl/com/razi/listy/przesuwanie/) - scenariusze i narzędzia kontroli wyników.
- [Demonstrator Swing](src/test/java/pl/com/razi/listy/przesuwanie/gui/PrzesuwanieSwingTest.java) - tabela do ręcznego sprawdzania ruchów.
- [Programy pomiarowe](src/benchmark/java/pl/com/razi/listy/przesuwanie/) - kod pomiaru poszczególnych etapów i pełnej operacji.
- [Konfiguracja](pom.xml) - projekt Maven, Java 17 i zależność testowa JUnit 5.
