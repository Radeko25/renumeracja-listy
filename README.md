📌 Renumeracja listy

Biblioteka Java do zaawansowanego przesuwania elementów listy w różnych trybach:

🔹 liniowym (bez możliwości “przeskakiwania” — ruch tylko jeśli wszystkie elementy mogą się przesunąć)

🔹 cyklicznym (“karuzela”: elementy mogą przejść z dołu na górę i odwrotnie)

🔹 dociskającym (przesunięcie trwa dopóki cały blok nie osiągnie granicy listy)

Dodatkowo biblioteka oferuje:

🔹 automatyczną detekcję bloków wybranych elementów

🔹 generowanie Planu SQL na potrzeby aktualizacji pozycji (LP) w bazie danych

🔹 przebudowę permutacji listy na podstawie wykonanych operacji

✨ Funkcje biblioteki
✔ Wykrywanie bloków (ciągów kolejnych elementów)

Przykład:
Wybrane indeksy: 1, 2, 3, 7, 10, 11
Zostaną wykryte bloki:

[1–3], [7–7], [10–11]

✔ Obsługa trzech trybów przesuwania
1️⃣ Tryb liniowy

Elementy mogą się przesunąć TYLKO jeśli wszystkie mogą wykonać ruch.

Gdy któryś element byłby poza listą — przesuwanie jest anulowane.

Przykład:

Lista: 1 2 3 4 5
Wybrane: [4, 5]
Ruch: +1 (dół)
→ Ruch niemożliwy (element 5 dotyka już końca)

2️⃣ Tryb cykliczny

Przesuwanie działa jak pierścień:

[1] → [n]
[n] → [1]


Bloki mogą przeskoczyć początek/koniec listy.

3️⃣ Tryb dociskający

Przesuwanie trwa dopóki cały blok nie osiągnie granicy.

Przykład:

Lista: [A, B, C, D, E, F]
Wybrane: [C, D]
Ruch: -10 (góra)
→ Wynik: blok [C, D] zatrzymuje się na górze (index 0–1)

📦 Instalacja

(Jeśli później wrzucisz na Maven Central lub GitHub Packages, dodamy snippet.)

Na razie można dodać jako moduł jar:

git clone https://github.com/Radeko25/renumeracja-listy.git
mvn install

🧠 Architektura
Najważniejsze klasy
Klasa	Rola
PrzesuwanieObsluga	Główna klasa, wykonuje przesuwanie listy
PrzesuwanieObliczenia	Logika określająca wykonalność ruchu
PrzesuwanieIndeksyBlok	Reprezentuje pojedynczy blok
PrzesuwaniePlan	Plan aktualizacji pozycji (LP), używany przy SQL
PrzesuwanieWynik	Wynik operacji — nowa lista i wykonane przemieszczenia
🛠 Przykład użycia
List<Item> lista = ...; // Twoja lista

// Utwórz handler
PrzesuwanieObsluga<Item> obsluga = new PrzesuwanieObsluga<>(
        TrybPrzesuwania.LINIOWY
);

// Przesuń elementy
PrzesuwanieWynik wynik = obsluga.przesun(
        lista,
        Arrays.asList(3, 4),  // indeksy wybrane
        +1                    // przesunięcie w dół
);

// Nowa lista
List<Item> po = wynik.getListaPo();

// Plan SQL
PrzesuwaniePlan plan = wynik.getPlan();

🗂 Przykład wygenerowanego planu SQL

Plan jest listą operacji typu:

[blokLPod=5–7 offset=+1]
[blokLPod=12–12 offset=-1]


Można go łatwo zamienić na SQL:

UPDATE dokument
SET lp = lp + :offset
WHERE lp BETWEEN :lpOd AND :lpDo;

🧪 Testy i benchmarki

Projekt zawiera:

testy jednostkowe (src/test/java)

testy scenariuszowe przesuwania bloków

benchmarki (src/benchmark/java) porównujące:

ruch liniowy

cykliczny

dociskający

przebudowę permutacji

📘 Przykłady zachowania trybów
🔹 Przesunięcie cykliczne
Wejście:  [1, 2, 3, 4, 5]
Wybrane:  [5]
Ruch:     +1
Wynik:    [5, 1, 2, 3, 4]

🔹 Tryb dociskający
Wejście:  [A, B, C, D, E]
Wybrane:  [C, D]
Ruch:     -5
Wynik:    [C, D, A, B, E]

📄 Licencja

Projekt jest objęty licencją MIT — możesz używać w projektach prywatnych i komercyjnych bez ograniczeń.

🤝 Współpraca

Pull requesty, zgłoszenia błędów i propozycje funkcji są mile widziane.

⭐ Jeśli projekt Ci się podoba

Rozważ zostawienie ⭐ na GitHub – to pomaga projektowi rosnąć 😊
