# v2.1 — naprawa crasha „font.png musi miec 256x256 pikseli” ✅

## Co było nie tak?
Gra crashowała zaraz po starcie z błędem:

```
java.lang.IllegalStateException: font.png musi miec 256x256 pikseli
    at com.aiminecraft.render.FontAtlas.load (FontAtlas.java:60)
```

Powód: `FontAtlas.java` wymagał fontu **256×256**, a generator (`tools/make_assets.py`)
poprawnie tworzy font **256×96** (95 znaków mieści się w 6 rzędach po 16 pikseli —
reszta atlasu byłaby pusta). Sam plik `font.png` jest dobry, zły był tylko test rozmiaru w kodzie.

## Jak naprawić? (2 sposoby)

### Sposób 1 — nowy ZIP (najłatwiejszy) ⭐
Pobierz z repo nowy **`AiMINECRAFT.zip`** (wersja 2.0.0 + fix) i rozpakuj do świeżego katalogu.

### Sposób 2 — podmień 1 plik 📄
Weź `FontAtlas.java` z folderu `v2.1/src/...` i podmień nim plik w projekcie:
`src/main/java/com/aiminecraft/render/FontAtlas.java`

Zapisz (`Ctrl+S`) i kliknij ▶ — menu gry powinno się pojawić!
