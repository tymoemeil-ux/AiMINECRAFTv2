# Poprawka: `cannot find symbol` w TextureAtlas.java (linie 71–72)

## Co było nie tak?
W pliku `TextureAtlas.java` brakowało jednego importu.
Stała `GL_CLAMP_TO_EDGE` w LWJGL znajduje się w klasie `GL12`, a nie `GL11`.

## Jak naprawić? (2 sposoby)

### Sposób 1 — gotowy plik (najłatwiejszy) ✅
1. Weź plik **`TextureAtlas.java`** z tego folderu (`poprawki/`).
2. W swoim projekcie znajdź:
   `src/main/java/com/aiminecraft/render/TextureAtlas.java`
3. **Podmień go** (wklej i zamień).
4. W IntelliJ kliknij ▶ — błędy znikają.

### Sposób 2 — jedna linijka ręcznie ⌨️
Otwórz `TextureAtlas.java`, znajdź na górze importy i dopisz:

```java
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
```

tak żeby wyglądały np. tak:

```java
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
```

Zapisz (`Ctrl+S`) i kliknij ▶.

---
Uwaga: główny `AiMINECRAFT.zip` w repo jest już naprawiony —
przy świeżym pobraniu nie trzeba nic podmieniać.
