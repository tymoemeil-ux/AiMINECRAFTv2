# AiMINECRAFT 2.0 — tutorial (IntelliJ IDEA)

Klona Minecrafta w **Javie 17 + LWJGL (OpenGL)**: biomy, woda i pływanie,
jaskinie i rudy, crafting, chat z komendami, panel admina, dzień/noc,
zapis świata, dźwięki i pełne menu.

---

## 1. Co musisz mieć

1. **JDK 17** (lub nowszy) — np. Eclipse Temurin: <https://adoptium.net>
   (najłatwiej: IntelliJ sam pobierze JDK — patrz krok 3).
2. **IntelliJ IDEA Community** (darmowy): <https://www.jetbrains.com/idea/download/>
3. Internet przy pierwszym otwarciu (Maven pobierze biblioteki LWJGL/JOML).
4. Kartę graficzną wspierającą **OpenGL 3.3** (każda z ostatnich ~15 lat daje radę).

---

## 2. Otwieranie projektu

1. Rozpakuj `AiMINECRAFT.zip` (np. do `Dokumenty/AiMINECRAFT`).
2. W IntelliJ: **File → Open…** → wybierz katalog `AiMINECRAFT` (ten z `pom.xml`) → **OK**.
3. Jeśli wyskoczy **„Trust Project”** → **Trust Project**.
4. Na dole po prawej IntelliJ zapyta o Mavena — kliknij **Load Maven Project**
   (albo poczekaj, aż sam zaimportuje; pasek postępu jest na dole).
5. Jeśli IntelliJ zgłosi **„No SDK”**:
   **File → Project Structure → Project → SDK → Download JDK… → 17** (Temurin/Oracle, co chcesz).

Po chwili w panelu po lewej zobaczysz kod w `src/main/java/com/aiminecraft`.

> 💡 Pliki `.idea/` i `*.iml` generują się same — nie musisz ich mieć w ZIP-ie.

---

## 3. Uruchamianie gry ▶

Otwórz plik **`src/main/java/com/aiminecraft/Main.java`** i kliknij zielony trójkąt **▶**
przy metodzie `main` (albo `Shift+F10`). To wszystko!

Zobaczysz menu główne: **Graj** (wybór świata + seed), **Ustawienia**, **Wyjdź**.

**Własny seed:** wpisz go w polu w menu „Wybierz świat” (puste = losowy).
Możesz też podać seed jako argument uruchomieniowy
(Run → Edit Configurations… → **Program arguments**, np. `12345`) —
trafi prosto do pola w menu. Ten sam seed = ten sam świat.

**Uruchomienie z konsoli** (opcjonalnie, z katalogu projektu):

```bash
mvn compile exec:java
```

---

## 4. Sterowanie 🎮

| Klawisz | Akcja |
|---|---|
| `W` `A` `S` `D` | chodzenie |
| `Mysz` | rozglądanie |
| `Spacja` | skok / wypływanie z wody |
| `Shift` (na ziemi) | sprint |
| `Ctrl` | sprint (ziemia i latanie) |
| `F` | latanie WŁ / WYŁ |
| `LPM` | kopanie (survival: pasek postępu, narzędzia kopią szybciej) |
| `PPM` | stawianie bloku / otwieranie stołu (pustą ręką) |
| `1`–`9`, kółko myszy | wybór slotu hotbara |
| `E` | ekwipunek (+ crafting 2×2) / creative |
| `T` | chat i komendy (`/help`) |
| `G` | panel admina |
| `F3` | nakładka diagnostyczna |
| `Esc` | pauza / zamykanie okien |
| `F11` | pelny ekran WL / WYL |
| `Strzalki` + `Enter` | obsluga menu bez myszy (wybor swiata, ustawienia) |
| `1`–`3` / `1`–`8` | skroty w menu, pauzie i panelu admina |
| `Strzalki` (w grze) | awaryjne rozgladanie bez myszy |

W tytule okna widzisz FPS, pozycję, tryb gry, porę dnia i seed.

---

## 5. Co jest w grze ✨

- **Świat:** 6 biomów (równiny, las, pustynia, góry, śnieg, ocean),
  plaże, jaskinie, rudy (węgiel, żelazo, złoto, diament, redstone),
  3 rodzaje drzew, kwiaty, kaktusy, lód na jeziorach.
- **Woda:** pływanie, gęsta mgła pod wodą, dźwięk plusku.
- **Tryby:** Survival (kopanie z postępem, wypadanie bloków, narzędzia mają znaczenie)
  i Creative (latanie, niszczenie od razu, zakładka ze wszystkim).
- **Crafting:** siatka 2×2 w ekwipunku, stół 3×3, książka z 16 recepturami
  (deski, patyki, pochodnie, stół, narzędzia drewniane i kamienne).
- **Chat (T):** komendy `/help /seed /spawn /tp /time /day /night /fly
  /gamemode /give /setblock /fill /kill /clear`.
- **Panel admina (G):** dzień/noc, zmiana trybu, latanie, teleport na spawn,
  zestaw startowy, respawn.
- **Dzień/noc:** pełna doba (10 minut), zachody słońca, ciemne noce.
- **Zapis:** świat zapisuje się sam co 30 s i przy wyjściu (`worlds/swiat.dat`).
- **Ustawienia:** zasięg widzenia, FOV, czułość myszy, VSync, dźwięk.
- **Dźwięki:** klikanie, kopanie, stawianie, podnoszenie, plusk.

---

## 6. Budowanie pliku JAR 📦

1. Panel **Maven** (prawa strona) → **AiMINECRAFT → Lifecycle → `package`**
   (kliknij dwa razy). Ewentualnie z konsoli: `mvn package`.
2. Gotowy plik: `target/aiminecraft-2.0.0-jar-with-dependencies.jar`
   (wersja w `pom.xml`).
3. Uruchomienie: `java -jar target/aiminecraft-*-jar-with-dependencies.jar`
   (klik dwukrotny też zwykle działa).

To tzw. „gruby JAR” — LWJGL sam wypakowuje natywne `.dll`/`.so` z jego środka,
więc działa na komputerze, na którym został zbudowany.

---

## 7. Tekstury, font i dźwięki 🖼️

Gra używa **Twoich tekstur** (`teksturki.zip` z repo):

```
src/main/resources/textures/blocks/   43 tekstury 16×16 (atlas 8×8)
src/main/resources/textures/items/    16 ikon przedmiotów (atlas 4×4)
src/main/resources/font/              font.png (256×256) + widths.txt
src/main/resources/sounds/            5 dźwięków WAV (syntezowane)
```

- **Podmiana:** nadpisz któryś PNG własnym **16×16** (nazwa musi zostać ta sama).
- **Odtworzenie wszystkiego:** połóż `teksturki.zip` obok `pom.xml` i uruchom
  `python tools/make_assets.py` (wymaga `pip install pillow`).
  Skrypt wycina klatki wody, nakłada odcienie trawy/listowia/wody jak prawdziwy
  Minecraft, buduje font z DejaVu i syntetyzuje dźwięki.
- Tekstury pochodzą z assetów Minecrafta (Mojang) — do nauki i własnej zabawy.

---

## 8. Jak zbudowany jest kod 🧩

```
src/main/java/com/aiminecraft/
├── Main.java            punkt startowy
├── Game.java            pętla gry, stany, kopanie/stawianie, chat, komendy, zapis
├── ui/
│   └── Screens.java     wszystkie ekrany: menu, ekwipunek, stół, chat, admin, HUD
├── world/
│   ├── Block.java       36 bloków: twardość, narzędzie, drop, tekstury
│   ├── Biome.java       biomy
│   ├── Chunk.java       chunk 16×64×16 + siatka stała i wodna
│   ├── World.java       streaming, przebudowa, rysowanie 2-pass
│   ├── WorldGenerator.java  biomy, oceany, jaskinie, rudy, drzewa, rośliny
│   ├── WorldSave.java   zapis świata do jednego pliku
│   ├── PerlinNoise.java szum Perlina 2D/3D + fbm + hash
│   └── ChunkMesher.java bloki, krzyżyki roślin, pochodnie, woda
├── player/
│   ├── Player.java      ruch, skok, latanie, pływanie, kolizje AABB
│   └── BlockRaycast.java celowanie w blok (DDA)
├── item/
│   ├── Item.java        rejestr przedmiotów + lista creative
│   ├── ItemStack.java   stos przedmiotów
│   ├── Inventory.java   36 slotów + hotbar
│   ├── Recipes.java     16 receptur + dopasowanie kształtu
│   └── Tool.java        typy narzędzi
├── render/
│   ├── Window.java      okno GLFW + wpisywanie tekstu + kółko myszy
│   ├── Shader.java      shadery GLSL + uniformy
│   ├── Mesh.java        VAO/VBO/IBO
│   ├── TextureAtlas.java atlas bloków 8×8
│   ├── ItemAtlas.java   atlas przedmiotów 4×4
│   ├── FontAtlas.java   font bitmapowy
│   ├── GuiRenderer.java prostokąty, tekst, ikony (GUI)
│   └── Hud.java         celownik + ramka bloku
└── util/
    ├── Settings.java    ustawienia (settings.properties)
    └── Sound.java       odtwarzanie WAV
src/main/resources/
├── shaders/   block (bloki+mgła+dzień/noc), water, gui, line (HUD)
├── textures/  bloki + itemy
├── font/      font.png + widths.txt
└── sounds/    click/break/place/pickup/splash.wav
tools/
└── make_assets.py       budowanie tekstur, fontu i dźwięków z teksturki.zip
```

**Ciekawe miejsca do grzebania:**

- `WorldGenerator` — kształt terenu, biomy, gęstość drzew i rud.
- `Block` — twardość, narzędzie, co wypada z bloku.
- `Recipes` — dodawanie własnych receptur.
- `Game.DAY_LENGTH` — długość doby, `Game.runCommand()` — własne komendy.
- `Screens` — wygląd wszystkich okien.

---

## 9. Typowe problemy ❓

**„No JDK specified” / czerwony kod** → File → Project Structure → SDK → Download JDK 17.

**Maven nic nie pobiera / „Cannot resolve symbol lwjgl”** →
prawy panel Maven → 🔄 Reload. Sprawdź internet. Pierwsze pobieranie chwilę trwa.

**Widać tylko niebo (brak terenu)** → naciśnij `C` / `V` (testy renderingu)
i sprawdź konsolę. Upewnij się, że masz nowy kod v2.

**`UnsatisfiedLinkError` / brak natywek** → zwykle zły profil w `pom.xml`.
Upewnij się, że masz 64-bitową Javę (Help → About → Runtime: `amd64`/`x86_64`/`aarch64`).

**Czarne okno / crash „OpenGL 3.3”** → zaktualizuj sterowniki karty graficznej.
Na bardzo starym sprzęcie gra nie ruszy (wymaga OpenGL 3.3).

**Gra działa, ale mysz nie rozgląda** → kliknij w okno gry (mysz musi być „złapana”).

**Linux: błąd o `libX11` / `glfw`** → doinstaluj pakiety, np. (Ubuntu/Debian):
`sudo apt install libx11-6 libgl1 libxrandr2 libxinerama1 libxcursor1 libxi6`

**Zapis nie działa** → gra zapisuje do `worlds/swiat.dat` w katalogu roboczym
(przy starcie z IntelliJ to katalog projektu). Sprawdź uprawnienia do zapisu.

---

## 10. Co dalej? 🚀

Pomysły na kolejne wersje (daj znać, co chcesz!):

- piec i przetapianie rud 🔥
- zasilany redstone (prąd, dźwignie, drzwi) ⚡
- moby (zwierzaki/zombie) 🐄
- zdrowie i głód 🍖
- więcej receptur i bloków 🧱
- multiplayer 🌐

Miłej zabawy i pokazuj, co zbudujesz! 🙂
