# v2.3 — działające przyciski + ładniejsze menu ✅

## Co było nie tak?
1. **Przyciski nie działały** — na Windows ze skalowaniem ekranu 125%/150%
   pozycja myszy była liczona w innych jednostkach niż obraz, więc kliknięcia
   lądowały obok przycisków. `Window.java` przeskalowuje teraz mysz do pikseli
   bufora (działa przy każdym DPI).
2. **Brzydkie menu** — nowy wygląd ekranu tytułowego: słońce z poświatą,
   dryfujące chmury, ziemia z prawdziwych tekstur bloków, logo z cieniem
   i przyciski w stylu Minecrafta (fazowane, niebieskie po najechaniu).

## Jak naprawić? (2 sposoby)

### Sposób 1 — nowy ZIP (najłatwiejszy) ⭐
Pobierz z repo nowy **`AiMINECRAFT.zip`** (zawiera fixy v2.1 + v2.2 + v2.3)
i rozpakuj do świeżego katalogu.

### Sposób 2 — podmień 3 pliki 📄
Weź z folderu `v2.3/src/...` i podmień w projekcie:
- `src/main/java/com/aiminecraft/render/Window.java`
- `src/main/java/com/aiminecraft/Game.java`
- `src/main/java/com/aiminecraft/ui/Screens.java`

Zapisz (`Ctrl+S`) i kliknij ▶ — przyciski klikają się tam, gdzie je widać!
