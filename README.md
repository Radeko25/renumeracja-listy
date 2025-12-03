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

## Przykład użycia

```java
PrzesuwanieObsluga<TestDane> obsluga =
        new PrzesuwanieObsluga<>(TrybPrzesuwania.LINIOWY);

PrzesuwanieWynik wynik = obsluga.przesun(
        lista,
        Arrays.asList(3, 4),
        +1
);

List<TestDane> wynikowaLista = wynik.getListaPo();
PrzesuwaniePlan plan = wynik.getPlan();
```

## Przykład wygenerowanego planu

Plan składa się z operacji typu:

```
lpOd = 5, lpDo = 7, offset = +1
lpOd = 12, lpDo = 12, offset = -1
```

Może zostać łatwo użyty w SQL:

```sql
UPDATE tabela
SET lp = lp + :offset
WHERE lp BETWEEN :lpOd AND :lpDo;
```

## Typowe scenariusze

* przesuwanie pojedynczych elementów,
* przesuwanie wielu oddzielnych bloków jednocześnie,
* cykliczne rotowanie dużych list,
* dociskanie bloków do góry lub do dołu listy,
* generowanie permutacji po ruchu w celu późniejszego porównywania stanu listy.

## Licencja

Projekt udostępniany jest na licencji MIT.
