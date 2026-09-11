# AiMINECRAFT Web

Gra voxelowa w przegladarce (Three.js). Zero instalacji - sciagnij JEDEN plik
i otworz go dwuklikiem.

## Granie

Otworz w przegladarce (Chrome / Edge / Firefox):

    web/dist/AiMINECRAFT-web.html

Sterowanie: WASD + mysz, LPM kopie, PPM stawia, E ekwipunek, T czat,
F3 debug, F11 pelny ekran. Reszta w pomocy w grze (przycisk Pomoc).

## Budowanie (dla developera)

Wymagane: Python 3 (do buildu), Node.js (do testow). Bez npm install.

    cd web
    python3 tools/build.py        # tekstury + paczka dist/AiMINECRAFT-web.html
    node tools/selftest.js        # testy logiki (seed, szum, receptury, swiat)
    node tools/smoketest.js       # test calej gry w node (stub DOM + THREE)

Wersja developerska (wiele plikow): otworz `web/index.html` w przegladarce.

## Struktura

    index.html        szkielet strony (znaczniki buildu: AIMC_STYLE, AIMC_SCRIPTS)
    css/style.css     style w klimacie Minecraft
    vendor/three.min.js  Three.js r128 (z npm, plik lokalny)
    js/config.js      bloki, przedmioty, receptury
    js/noise.js       seed + szum Perlina
    js/worldgen.js    generator swiata (biomy, jaskinie, rudy, drzewa)
    js/inventory.js   ekwipunek + klikanie w sloty
    js/textures.js    tekstury base64 (GENEROWANE z projektu Javy, nie edytuj)
    js/audio.js       syntezowane dzwieki (WebAudio)
    js/save.js        zapis w localStorage + indeks zmian
    js/raycast.js     celowanie w bloki (DDA)
    js/world.js       chunki, streaming, meshowanie
    js/player.js      ruch i fizyka gracza
    js/ui.js          caly interfejs (DOM)
    js/main.js        gra: petla, niebo, input, komendy, zapis
    tools/build.py    build tekstur + paczki single-file
    tools/selftest.js testy jednostkowe logiki
    tools/smoketest.js  test integracyjny calej gry
    dist/             gotowa gra w jednym pliku

Zapis gry trzymany jest w przegladarce (localStorage), osobno dla wersji
developerskiej i paczki dist (rozne sciezki = rozne zapisy).
