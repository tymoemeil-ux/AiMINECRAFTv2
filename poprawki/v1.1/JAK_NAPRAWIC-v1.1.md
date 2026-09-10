# v1.1 — niebieski ekran: diagnostyka + awaryjne przełączniki

## Najłatwiej ⭐
Pobierz z repo **nowy `AiMINECRAFT.zip`** (jest już naprawiony) i rozpakuj
do świeżego katalogu. Gotowe — nic nie trzeba podmieniać.

## Albo podmień 6 plików ręcznie
Skopiuj zawartość folderu `v1.1/src/` do swojego projektu
(zachowaj strukturę katalogów `src/main/java/...`).
Zmienione pliki (diagnostyka + klawisze C/V + drobne poprawki):

- `com/aiminecraft/Game.java`
- `com/aiminecraft/world/World.java`
- `com/aiminecraft/world/Chunk.java`
- `com/aiminecraft/render/Mesh.java`
- `com/aiminecraft/render/TextureAtlas.java`
- `com/aiminecraft/player/Player.java`

## Co potem?
1. Uruchom grę (▶).
2. Wciśnij **`C`** — jeśli świat się pojawi, winny był culling.
3. Jeśli nie — wciśnij **`V`** — jeśli świat się pojawi, winna była mgła.
4. Tak czy siak **skopiuj z konsoli cały blok `=== DIAGNOSTYKA ===`**
   i wyślij go — po nim będzie 100% wiadomo, co dalej.
