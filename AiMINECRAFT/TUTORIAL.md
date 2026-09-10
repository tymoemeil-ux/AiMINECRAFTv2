# AiMINECRAFT — tutorial (IntelliJ IDEA)

Prosty klon Minecrafta w **Javie 17 + LWJGL (OpenGL)**: nieskończony teren z pagórków,
drzewa, chodzenie, skakanie, latanie, kolizje, niszczenie i stawianie bloków.

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

Przy pierwszym starcie Maven pobierze zależności (kilka MB) — gra wystartuje po chwili.
W konsoli zobaczysz `Seed swiata: ...` i `Generowanie swiata...`, a potem otworzy się okno gry.

**Własny seed:** edytuj konfigurację uruchomieniową
(Run → Edit Configurations… → **Program arguments**, np. `12345`) — ten sam seed = ten sam świat.

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
| `Spacja` | skok |
| `Shift` (na ziemi) | sprint |
| `Ctrl` | sprint (ziemia i latanie) |
| `F` | latanie WŁ / WYŁ |
| `Spacja` / `Shift` (w locie) | w górę / w dół |
| `LPM` | niszczenie bloku (przytrzymaj = seria) |
| `PPM` | stawianie bloku |
| `1`–`6` | wybór bloku: trawa, ziemia, kamień, drewno, liście, bedrock |
| `Esc` | uwolnij mysz; **klik** = złap z powrotem |

W tytule okna widzisz FPS, pozycję, wybrany blok i seed.

---

## 5. Budowanie pliku JAR 📦

1. Panel **Maven** (prawa strona) → **AiMINECRAFT → Lifecycle → `package`**
   (kliknij dwa razy). Ewentualnie z konsoli: `mvn package`.
2. Gotowy plik: `target/aiminecraft-1.0.0-jar-with-dependencies.jar`
3. Uruchomienie: `java -jar target/aiminecraft-1.0.0-jar-with-dependencies.jar`
   (klik dwukrotny też zwykle działa).

To tzw. „gruby JAR” — LWJGL sam wypakowuje natywne `.dll`/`.so` z jego środka,
więc działa na komputerze, na którym został zbudowany.

---

## 6. Tekstury 🖼️

Gra używa **Twoich tekstur** (`teksturki.zip` z repo). Gotowe 8 plików 16×16 leży w:

```
src/main/resources/textures/blocks/
  grass_top.png  grass_side.png  dirt.png  stone.png
  oak_log_side.png  oak_log_top.png  oak_leaves.png  bedrock.png
```

- **Podmiana:** nadpisz któryś PNG własnym **16×16** (nazwa musi zostać ta sama) i uruchom grę.
- **Odtworzenie z ZIP-a:** połóż `teksturki.zip` obok `pom.xml` i uruchom
  `python tools/extract_textures.py` (wymaga `pip install pillow`).
  Skrypt nakłada też zielony odcień na trawę i liście — tak jak prawdziwy Minecraft.
- Trawa góra/dół/bok to osobne kafle — patrz `Block.java`, jak są przypisane.

> ⚖️ Tekstury pochodzą z assetów Minecrafta (Mojang) — używaj ich do nauki
> i własnej zabawy, nie do publikowania cudzej gry.

---

## 7. Jak zbudowany jest kod 🧩

```
src/main/java/com/aiminecraft/
├── Main.java            punkt startowy (seed z argumentu)
├── Game.java            pętla gry, sterowanie, niszczenie/stawianie, kamera, mgła
├── world/
│   ├── Block.java       definicje bloków + indeksy tekstur
│   ├── Chunk.java       chunk 16×64×16
│   ├── World.java       chunki wokół gracza, kolejka przebudowy, rysowanie
│   ├── WorldGenerator.java  teren z szumu Perlina + drzewa
│   ├── PerlinNoise.java     szum Perlina + fbm + hash
│   └── ChunkMesher.java     zamiana bloków na trójkąty (face culling)
├── player/
│   ├── Player.java      ruch, skok, latanie, kolizje AABB
│   └── BlockRaycast.java celowanie w blok (DDA)
└── render/
    ├── Window.java      okno GLFW, klawiatura/mysz (polling)
    ├── Shader.java      shadery GLSL + uniformy
    ├── Mesh.java        VAO/VBO/IBO
    ├── TextureAtlas.java atlas 8× 16×16 → tekstura 64×64
    └── Hud.java         celownik + ramka bloku
src/main/resources/
├── shaders/            block.vert/.frag (bloki+mgła+wycinanie liści), line.vert/.frag (HUD)
└── textures/blocks/    8 tekstur PNG
```

**Ciekawe miejsca do grzebania:**

- `WorldGenerator.getHeight()` — kształt terenu (częstotliwość, wysokość).
- `WorldGenerator.TREE_CHANCE` — gęstość lasu (0.012 ≈ 3 drzewa na chunk).
- `Game.RENDER_DISTANCE` — zasięg widzenia (6 = szybko; więcej = ładniej, ale wolniej).
- `Player` — prędkości, grawitacja, siła skoku.
- `ChunkMesher.SHADE` — cieniowanie ścian.

---

## 8. Typowe problemy ❓

**Widać tylko niebieskie niebo (brak terenu)** →
najpierw naciśnij `C` (wyłącza culling), potem `V` (wyłącza mgłę).
Jeśli któreś pomaga — daj znać które! Tak czy siak skopiuj z konsoli
cały blok `=== DIAGNOSTYKA ===` (karta graficzna, chunki, indeksy) —
po nim od razu widać, gdzie leży problem.

**„No JDK specified” / czerwony kod** → File → Project Structure → SDK → Download JDK 17.

**Maven nic nie pobiera / „Cannot resolve symbol lwjgl”** →
prawy panel Maven → 🔄 Reload. Sprawdź internet. Pierwsze pobieranie chwilę trwa.

**`UnsatisfiedLinkError` / brak natywek** → zwykle zły profil w `pom.xml`.
Upewnij się, że masz 64-bitową Javę (Help → About → Runtime: `amd64`/`x86_64`/`aarch64`).

**Czarne okno / crash „OpenGL 3.3”** → zaktualizuj sterowniki karty graficznej.
Na bardzo starym sprzęcie gra nie ruszy (wymaga OpenGL 3.3).

**Gra działa, ale mysz nie rozgląda** → kliknij w okno gry (mysz musi być „złapana”).

**Krzaki zamiast polskich znaków w konsoli** → normalne na Windows,
komunikaty w kodzie są celowo bez ogonków. Na grę nie ma to wpływu.

**Linux: błąd o `libX11` / `glfw`** → doinstaluj pakiety, np. (Ubuntu/Debian):
`sudo apt install libx11-6 libgl1 libxrandr2 libxinerama1 libxcursor1 libxi6`

---

## 9. Co dalej? 🚀

Pomysły na kolejne wersje (daj znać, co chcesz!):

- woda + pływanie 🏊
- zapis/wczytywanie świata 💾
- jaskinie i rudy ⛏️
- dzień/noc + słońce/księżyc 🌙
- moby (zwierzaki/zombie) 🐄
- ekwipunek i pełny creative inventory 🎒
- dźwięki 🔊
- multiplayer 🌐

Miłej zabawy i pokazuj, co zbudujesz! 🙂
