# v2.5 — menu bez myszy + pełny ekran (F11) ✅

## Co to daje?
Menu da się teraz obsłużyć **samą klawiaturą** — mysz nie jest już
do niczego potrzebna (poza rozglądaniem w grze, ale i na to jest awaria):

- **Ekran tytułowy:** `1`/`2`/`3` = Graj/Ustawienia/Wyjdź, `Enter` = Graj.
- **Wybór świata:** strzałki (i `Tab`) = zaznaczenie, `Enter` = start.
  Seed wpisujesz normalnie literami/cyframi.
- **Ustawienia:** góra/dół = wiersz, lewo/prawo = zmiana, `Enter`/`Esc` = Gotowe.
- **Pauza:** `1`/`2`/`3`, **panel admina:** `1`–`8`, **usuwanie zapisu:** `Y`/`N`.
- **F11** = pełny ekran WŁ/WYŁ (działa wszędzie, też w menu).
- **Strzałki w grze** = awaryjne rozglądanie bez myszy.

## Jak naprawić? (2 sposoby)

### Sposób 1 — nowy ZIP (najłatwiejszy) ⭐
Pobierz z repo nowy **`AiMINECRAFT.zip`** (zawiera fixy v2.1–v2.5)
i rozpakuj do świeżego katalogu.

### Sposób 2 — podmień 4 pliki 📄
Weź z folderu `v2.5/src/...` i podmień w projekcie:
- `src/main/java/com/aiminecraft/Game.java`
- `src/main/java/com/aiminecraft/ui/Screens.java`
- `src/main/java/com/aiminecraft/render/Window.java`
- `src/main/java/com/aiminecraft/player/Player.java`

Zapisz (`Ctrl+S`) i kliknij ▶ — a potem walnij `1`, żeby zagrać.
