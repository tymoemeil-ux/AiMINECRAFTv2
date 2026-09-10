#!/usr/bin/env python3
"""Wyciaga 8 tekstur blokow 16x16 z teksturki.zip (assety Minecrafta)
i zapisuje je do src/main/resources/textures/blocks/.

Trawa i liscie w MC sa szare - gra naklada na nie kolor (tint),
wiec ten skrypt robi to samo z gory (kolory biomu plains).

Uzycie:
    1. Poloz teksturki.zip w katalogu AiMINECRAFT/ (obok pom.xml).
    2. Uruchom:  python tools/extract_textures.py

Wymaga: pip install pillow
"""
import sys
import zipfile
from pathlib import Path

PROJECT = Path(__file__).resolve().parent.parent
ZIP_PATH = PROJECT / "teksturki.zip"
DST = PROJECT / "src" / "main" / "resources" / "textures" / "blocks"

GRASS_TINT = (145, 189, 89)    # plains #91BD59
FOLIAGE_TINT = (119, 171, 47)  # plains #77AB2F

JOBS = [
    ("grass_block_top.png", "grass_top.png", "tint"),
    ("grass_block_side.png", "grass_side.png", "raw"),
    ("dirt.png", "dirt.png", "raw"),
    ("stone.png", "stone.png", "raw"),
    ("oak_log.png", "oak_log_side.png", "raw"),
    ("oak_log_top.png", "oak_log_top.png", "raw"),
    ("oak_leaves.png", "oak_leaves.png", "tint_alpha"),
    ("bedrock.png", "bedrock.png", "raw"),
]


def main():
    try:
        from PIL import Image
    except ImportError:
        print("Brakuje biblioteki pillow:  pip install pillow")
        sys.exit(1)
    if not ZIP_PATH.exists():
        print(f"Nie ma pliku {ZIP_PATH}. Poloz tam teksturki.zip i sprobuj znowu.")
        sys.exit(1)
    DST.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(ZIP_PATH) as zf:
        for src, dst, mode in JOBS:
            name = f"assets/minecraft/textures/block/{src}"
            try:
                with zf.open(name) as f:
                    im = Image.open(f)
                    im.load()
            except KeyError:
                print(f"UWAGA: brak {name} w ZIP-ie, pomijam.")
                continue
            if im.size != (16, 16):
                print(f"UWAGA: {src} ma rozmiar {im.size}, a nie 16x16. Pomijam.")
                continue
            if mode == "tint":
                out = tint_gray(im, GRASS_TINT)
            elif mode == "tint_alpha":
                out = tint_keep_alpha(im, FOLIAGE_TINT)
            else:
                out = im.convert("RGBA")
            out.save(DST / dst)
            print("OK", dst)
    print("Gotowe:", DST)


def tint_gray(im, color):
    from PIL import Image
    g = im.convert("L")
    r, gg, b = color
    px = g.load()
    out = Image.new("RGBA", (16, 16))
    op = out.load()
    for y in range(16):
        for x in range(16):
            v = px[x, y] / 255.0
            op[x, y] = (int(r * v), int(gg * v), int(b * v), 255)
    return out


def tint_keep_alpha(im, color):
    from PIL import Image
    im = im.convert("RGBA")
    r, gg, b = color
    px = im.load()
    out = Image.new("RGBA", (16, 16))
    op = out.load()
    for y in range(16):
        for x in range(16):
            R, G, B, A = px[x, y]
            v = (R + G + B) / 3.0 / 255.0
            op[x, y] = (int(r * v), int(gg * v), int(b * v), A)
    return out


if __name__ == "__main__":
    main()
