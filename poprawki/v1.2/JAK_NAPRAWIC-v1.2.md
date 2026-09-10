# v1.2 — PRAWDZIWA naprawa niebieskiego ekranu ✅

## Co było nie tak?
Macierze kamery (projekcja + widok) **nigdy nie docierały do karty graficznej**.
`Shader.setMatrix4f()` robił `flip()` po **absolutnym** zapisie JOML,
co zerowało limit bufora → LWJGL liczył `count = 0` → uniformy zostawały
wyzerowane → każdy wierzchołek wpadał w `gl_Position = 0` → wszystko
obcinane → na ekranie tylko kolor nieba. Bez żadnego błędu i crasha!

## Jak naprawić? (2 sposoby)

### Sposób 1 — nowy ZIP (najłatwiejszy) ⭐
Pobierz z repo nowy **`AiMINECRAFT.zip`** (wersja 1.1.0) i rozpakuj do świeżego katalogu.

### Sposób 2 — podmień 1 plik 📄
Weź `Shader.java` z folderu `v1.2/src/...` i podmień nim plik w projekcie:
`src/main/java/com/aiminecraft/render/Shader.java`

### Sposób 3 — jedna linijka ⌨️
W `Shader.java`, w metodzie `setMatrix4f`, zamień:

```java
buffer.flip();
```

na:

```java
buffer.rewind();
```

Zapisz (`Ctrl+S`) i kliknij ▶ — świat się pojawi!
