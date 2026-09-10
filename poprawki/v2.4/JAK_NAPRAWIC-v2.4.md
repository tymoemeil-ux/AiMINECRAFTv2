# v2.4 — diagnostyka myszy + Enter = Graj ✅

## Co to daje?
Jeśli przyciski nadal nie działają, ta wersja pokazuje **na ekranie**,
co gra widzi:

- **etykieta wersji** (prawy dolny róg) — udowadnia, że odpalasz nowy kod,
- **pozycja myszy** (lewy górny róg, np. `mysz: 640,360`),
- **czerwony krzyżyk** w miejscu, gdzie gra widzi Twój kursor.

Dodatkowo **Enter na ekranie tytułowym = Graj** (awaryjne obejście
niedziałającej myszy; w ekranie wyboru świata Enter też startuje grę).

## Jak naprawić? (2 sposoby)

### Sposób 1 — nowy ZIP (najłatwiejszy) ⭐
Pobierz z repo nowy **`AiMINECRAFT.zip`** (zawiera fixy v2.1–v2.4)
i rozpakuj do świeżego katalogu.

### Sposób 2 — podmień 2 pliki 📄
Weź z folderu `v2.4/src/...` i podmień w projekcie:
- `src/main/java/com/aiminecraft/Game.java`
- `src/main/java/com/aiminecraft/ui/Screens.java`

## Co dalej?
1. Odpal grę i **zrób screena menu** (z widocznym kursorem myszy!).
2. Napisz, czy czerwony krzyżyk chodzi za Twoim kursorem, czy stoi w miejscu.
3. Jeśli krzyżyk nie chodzi za kursorem — napisz, jakie masz skalowanie ekranu
   (Ustawienia Windows → Ekran → Skala, np. 100% / 125% / 150%).

Te 3 informacje wystarczą, żeby namierzyć przyczynę.
