# Renumeracja listy

Biblioteka Java 17 do przesuwania pojedynczych elementów oraz wielu rozłącznych bloków na uporządkowanej liście.

Rdzeń biblioteki nie wymaga pola `LP` w obiekcie. Najpierw wylicza docelową permutację listy, a następnie może:

- zbudować nową kolejność elementów,
- wygenerować skompresowany plan zmian LP,
- przełożyć plan na `CASE WHEN` przydatny przy aktualizacji bazy danych.

## Tryby przesuwania

### `LINIOWE`

Całe zaznaczenie jest przesuwane sztywno o zadaną liczbę pozycji. Jeśli choć skrajny blok wyszedłby poza listę, operacja nie jest wykonywana.

### `CYKLICZNE`

Pozycje są liczone modulo długość listy. Element wychodzący poza koniec wraca na początek i odwrotnie.

### `DOCISKAJACE`

Każdy zaznaczony blok próbuje przesunąć się o zadaną wartość, ale zatrzymuje się na granicy listy albo na wcześniej ustawionym bloku. Dzięki temu rozłączne zaznaczenia mogą „złożyć się” w kierunku ruchu bez kolizji.

## Bloki zaznaczenia

Dla zaznaczonych LP:

```text
[1, 2, 3, 7, 10, 11]
```

powstają bloki:

```text
[1–3], [7], [10–11]
```

Biblioteka obsługuje dowolną liczbę takich bloków oraz przesunięcie o dowolną wartość `int`.

## Bezpieczna i szybka ścieżka wejścia

Domyślnie kolejność listy `wybrane` nie ma znaczenia. Biblioteka sama odtwarza ją zgodnie z kolejnością elementów w liście źródłowej:

```java
PrzesuwanieObsluga<Integer> obsluga =
        new PrzesuwanieObsluga.Builder<Integer>()
                .trybPrzesuwania(TrybPrzesuwania.LINIOWE)
                .build();

List<Integer> wynik = obsluga.przesunWDolOJeden(
        List.of(1, 2, 3, 4, 5, 6),
        List.of(5, 2)); // kolejność wejściowa nie ma znaczenia
```

Jeżeli caller (np. `JTable#getSelectedRows()`) gwarantuje już kolejność zgodną z listą źródłową, można wyłączyć normalizację i ominąć jej koszt:

```java
PrzesuwanieObsluga<Integer> obsluga =
        new PrzesuwanieObsluga.Builder<Integer>()
                .trybPrzesuwania(TrybPrzesuwania.LINIOWE)
                .normalizujKolejnoscWybranych(false)
                .build();
```

W tym trybie `wybrane` **musi** być przekazane w kolejności źródłowej. Naruszenie kontraktu kończy się `IllegalArgumentException`.

## Pełny wynik i plan LP

```java
PrzesuwanieWynik<Integer> wynik = obsluga.przesunPelny(
        List.of(1, 2, 3, 4, 5, 6),
        List.of(2, 5),
        1);

List<Integer> nowaLista = wynik.getLista();
PrzesuwaniePlan plan = wynik.getPlan();
```

Plan zawiera operacje:

```text
lpOd, lpDo, offset
```

czyli spójne zakresy starych LP przesunięte o jeden stały offset.

## Minimalna liczba zakresów `CASE WHEN`

Dla każdego starego LP definiujemy:

```text
delta(lp) = noweLp(lp) - stareLp
```

Generator planu scala każdą **maksymalną spójną sekwencję** LP o tym samym niezerowym `delta` w jedną operację.

W modelu:

```sql
WHEN lp BETWEEN :od AND :do THEN lp + :staly_offset
```

liczba wygenerowanych operacji jest więc minimalna: dwóch sąsiednich fragmentów nie można scalić, jeśli pomiędzy nimi występuje `delta = 0` albo zmienia się wartość offsetu.

Generator planu wykonuje pojedyncze przejście `O(n)` bez sortowania i bez tworzenia obiektu pomocniczego dla każdej zmienionej pozycji.

## SQL

```java
String sql = wynik.getPlan().toSqlCaseWhenBloki(
        "elementy",
        "lp",
        "grupa_id = 7");
```

Przykład:

```sql
UPDATE elementy
SET lp = CASE
    WHEN lp BETWEEN 2 AND 2 THEN lp + 1
    WHEN lp BETWEEN 3 AND 3 THEN lp - 1
    ELSE lp
END
WHERE grupa_id = 7;
```

Dla pustego planu metoda zwraca pusty `String`.

> `tabela`, `kolumnaLp` oraz `where` są fragmentami SQL przekazywanymi przez callera. Generator nie jest warstwą sanitizacji ani wykonawcą zapytań; wartości powinny pochodzić z zaufanego kodu.

## Zastosowania

- przyciski `↑` / `↓`,
- przesuwanie o `±N`,
- drag & drop w tabelach i listach,
- wiele rozłącznych zaznaczeń,
- cykliczne rotowanie list,
- renumeracja rekordów uporządkowanych po kolumnie LP,
- GUI nad bazą danych, w którym użytkownik zmienia kolejność rekordów,
- generowanie zwartego planu zmian do dalszej obsługi transakcyjnej.

## Złożoność

Główne etapy są liniowe względem rozmiaru listy:

- budowanie bloków: `O(n + k)`, gdzie `k` to liczba wybranych elementów,
- wyliczenie permutacji: `O(n)`,
- generowanie planu: `O(n)`,
- przebudowa listy: `O(n)`.

Biblioteka zawiera również proste benchmarki dla list do 1 000 000 elementów w `src/benchmark/java`.

## Testy

Testy JUnit obejmują scenariusze wszystkich trzech trybów, kontrakty wejścia, skrajne wartości przesunięcia oraz test własnościowy enumerujący tysiące kombinacji zaznaczeń i przesunięć. Test własnościowy sprawdza również, czy plan odpowiada wynikowej permutacji i czy zawiera minimalną liczbę maksymalnych zakresów stałego offsetu.

```bash
mvn test
```

## Licencja

MIT.
