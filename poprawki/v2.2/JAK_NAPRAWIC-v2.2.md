# v2.2 — naprawa „krzaków” w foncie (złe znaki w menu) ✅

## Co było nie tak?
Gra startowała, ale wszystkie napisy pokazywały **złe znaki**
(np. `Q[] Y^USBQVD` zamiast `AiMINECRAFT`).

Powód: font jest ładowany **bez odwracania w pionie** (flip=false),
więc wiersz 0 pliku leży na dole tekstury (t=0). Wzory `vTop`/`vBottom`
w `FontAtlas.java` zakładały odwrotnie — brały wiersze „w lustrze”
(wiersz 0 ↔ 5, 1 ↔ 4, 2 ↔ 3). Np. `A` (wiersz 2) wyświetlał znak
z wiersza 3, czyli `Q`. Dodatkowo `GuiRenderer.drawText` podawał
górę/dół glypha w złej kolejności.

## Jak naprawić? (2 sposoby)

### Sposób 1 — nowy ZIP (najłatwiejszy) ⭐
Pobierz z repo nowy **`AiMINECRAFT.zip`** (zawiera fixy v2.1 + v2.2)
i rozpakuj do świeżego katalogu.

### Sposób 2 — podmień 2 pliki 📄
Weź z folderu `v2.2/src/...` i podmień w projekcie:
- `src/main/java/com/aiminecraft/render/FontAtlas.java`
- `src/main/java/com/aiminecraft/render/GuiRenderer.java`

Zapisz (`Ctrl+S`) i kliknij ▶ — napisy będą poprawne!
