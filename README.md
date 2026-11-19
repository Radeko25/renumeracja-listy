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

Drugi argument zawiera wybrane elementy listy. Dodatnie przesunięcie oznacza ruch w dół, ujemne w górę. Gdy potrzebna jest tylko lista wynikowa, służy do tego `przesunListe`. Metoda `przesunPelny` zwraca również plan zmian.

## Przykład wygenerowanego planu

W powyższym przykładzie elementy B i C przesuwają się o jedną pozycję w dół, a D o dwie pozycje w górę. Plan opisuje te zmiany względem pozycji sprzed przesunięcia.

```text
lpOd = 2, lpDo = 3, offset = +1
lpOd = 4, lpDo = 4, offset = -2
```

Metoda `toSqlCaseWhenBloki` zapisuje oba zakresy w jednej instrukcji SQL.

```sql
UPDATE Dokumenty
SET lp = CASE
    WHEN lp BETWEEN 2 AND 3 THEN lp + 1
    WHEN lp BETWEEN 4 AND 4 THEN lp - 2
    ELSE lp
END
WHERE id_kategorii = 3;
```

## Typowe scenariusze

* przesuwanie pojedynczych elementów,
* przesuwanie wielu oddzielnych bloków jednocześnie,
* cykliczne rotowanie dużych list,
* dociskanie bloków do góry lub do dołu listy,
* generowanie permutacji po ruchu w celu późniejszego porównywania stanu listy.

## Licencja

Projekt udostępniany jest na licencji MIT.
