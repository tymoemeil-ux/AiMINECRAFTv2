#!/usr/bin/env python3
"""Buduje assety AiMINECRAFT v2:
  1. tekstury blokow  (teksturki.zip -> src/main/resources/textures/blocks/*.png)
  2. tekstury itemow  (teksturki.zip -> src/main/resources/textures/items/*.png)
  3. font bitmapowy   (DejaVu/systemowy -> src/main/resources/font/font.png + widths.txt)
  4. dzwieki          (synteza -> src/main/resources/sounds/*.wav)

Uzycie:  python tools/make_assets.py [--zip SCIEZKA]
Wymaga:  pip install pillow
"""
import math
import random
import struct
import sys
import wave
import zipfile
from pathlib import Path

PROJECT = Path(__file__).resolve().parent.parent
BLOCKS = PROJECT / "src" / "main" / "resources" / "textures" / "blocks"
ITEMS = PROJECT / "src" / "main" / "resources" / "textures" / "items"
FONT = PROJECT / "src" / "main" / "resources" / "font"
SOUNDS = PROJECT / "src" / "main" / "resources" / "sounds"

GRASS_TINT = (145, 189, 89)     # trawa / dab / wysoka trawa
BIRCH_TINT = (128, 167, 85)     # liscie brzozy (staly)
SPRUCE_TINT = (97, 153, 97)     # liscie swierku (staly)
WATER_TINT = (63, 118, 228)     # woda

# (plik w ZIP-ie, plik docelowy, tryb, tint)
BLOCK_JOBS = [
    ("grass_block_top.png", "grass_top.png", "tint", GRASS_TINT),
    ("grass_block_side.png", "grass_side.png", "raw", None),
    ("dirt.png", "dirt.png", "raw", None),
    ("stone.png", "stone.png", "raw", None),
    ("oak_log.png", "oak_log_side.png", "raw", None),
    ("oak_log_top.png", "oak_log_top.png", "raw", None),
    ("oak_leaves.png", "oak_leaves.png", "tint_alpha", GRASS_TINT),
    ("bedrock.png", "bedrock.png", "raw", None),
    ("water_still.png", "water.png", "water", WATER_TINT),
    ("sand.png", "sand.png", "raw", None),
    ("cobblestone.png", "cobblestone.png", "raw", None),
    ("oak_planks.png", "oak_planks.png", "raw", None),
    ("glass.png", "glass.png", "raw", None),
    ("coal_ore.png", "coal_ore.png", "raw", None),
    ("iron_ore.png", "iron_ore.png", "raw", None),
    ("gold_ore.png", "gold_ore.png", "raw", None),
    ("diamond_ore.png", "diamond_ore.png", "raw", None),
    ("redstone_ore.png", "redstone_ore.png", "raw", None),
    ("birch_log.png", "birch_log_side.png", "raw", None),
    ("birch_log_top.png", "birch_log_top.png", "raw", None),
    ("birch_leaves.png", "birch_leaves.png", "tint_alpha", BIRCH_TINT),
    ("spruce_log.png", "spruce_log_side.png", "raw", None),
    ("spruce_log_top.png", "spruce_log_top.png", "raw", None),
    ("spruce_leaves.png", "spruce_leaves.png", "tint_alpha", SPRUCE_TINT),
    ("snow.png", "snow.png", "raw", None),
    ("dandelion.png", "dandelion.png", "raw", None),
    ("poppy.png", "poppy.png", "raw", None),
    ("short_grass.png", "short_grass.png", "tint_alpha", GRASS_TINT),
    ("torch.png", "torch.png", "raw", None),
    ("crafting_table_top.png", "crafting_table_top.png", "raw", None),
    ("crafting_table_side.png", "crafting_table_side.png", "raw", None),
    ("cactus_side.png", "cactus_side.png", "raw", None),
    ("cactus_top.png", "cactus_top.png", "raw", None),
    ("gravel.png", "gravel.png", "raw", None),
    ("obsidian.png", "obsidian.png", "raw", None),
    ("ice.png", "ice.png", "raw", None),
    ("bookshelf.png", "bookshelf.png", "raw", None),
    ("pumpkin_side.png", "pumpkin_side.png", "raw", None),
    ("pumpkin_top.png", "pumpkin_top.png", "raw", None),
    ("melon_side.png", "melon_side.png", "raw", None),
    ("melon_top.png", "melon_top.png", "raw", None),
    ("sponge.png", "sponge.png", "raw", None),
    ("bricks.png", "bricks.png", "raw", None),
]

ITEM_JOBS = [
    "stick.png", "coal.png", "raw_iron.png", "raw_gold.png", "diamond.png", "redstone.png",
    "wooden_pickaxe.png", "wooden_axe.png", "wooden_shovel.png",
    "wooden_sword.png", "wooden_hoe.png",
    "stone_pickaxe.png", "stone_axe.png", "stone_shovel.png",
    "stone_sword.png", "stone_hoe.png",
]


def tint_gray(im, color):
    from PIL import Image
    g = im.convert("L")
    r, gg, b = color
    px = g.load()
    out = Image.new("RGBA", im.size)
    op = out.load()
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            v = px[x, y] / 255.0
            op[x, y] = (int(r * v), int(gg * v), int(b * v), 255)
    return out


def tint_keep_alpha(im, color):
    from PIL import Image
    im = im.convert("RGBA")
    r, gg, b = color
    px = im.load()
    out = Image.new("RGBA", im.size)
    op = out.load()
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            R, G, B, A = px[x, y]
            v = (R + G + B) / 3.0 / 255.0
            op[x, y] = (int(r * v), int(gg * v), int(b * v), A)
    return out


def do_textures(zip_path):
    from PIL import Image
    BLOCKS.mkdir(parents=True, exist_ok=True)
    ITEMS.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(zip_path) as zf:
        for src, dst, mode, color in BLOCK_JOBS:
            with zf.open(f"assets/minecraft/textures/block/{src}") as f:
                im = Image.open(f)
                im.load()
            if mode == "water":  # animowany pasek -> pierwsza klatka
                im = im.crop((0, 0, 16, 16))
                out = tint_gray(im, color)
            elif mode == "tint":
                out = tint_gray(im, color)
            elif mode == "tint_alpha":
                out = tint_keep_alpha(im, color)
            else:
                out = im.convert("RGBA")
            if out.size != (16, 16):
                print(f"UWAGA: {src} ma {out.size}, pomijam.")
                continue
            out.save(BLOCKS / dst)
        print(f"Bloki: OK ({len(BLOCK_JOBS)})")
        for name in ITEM_JOBS:
            with zf.open(f"assets/minecraft/textures/item/{name}") as f:
                im = Image.open(f)
                im.load()
            im.convert("RGBA").save(ITEMS / name)
        print(f"Itemy: OK ({len(ITEM_JOBS)})")


def find_font():
    cands = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "C:/Windows/Fonts/arialbd.ttf",
        "C:/Windows/Fonts/arial.ttf",
    ]
    for c in cands:
        if Path(c).exists():
            return c
    return None


def do_font():
    from PIL import Image, ImageDraw, ImageFont
    FONT.mkdir(parents=True, exist_ok=True)
    path = find_font()
    if path:
        font = ImageFont.truetype(path, 15)
        print("Font:", path)
    else:
        font = ImageFont.load_default()
        print("Font: domyslny PIL (awaryjny)")
    cell, cols = 16, 16
    chars = [chr(c) for c in range(32, 127)]
    rows = (len(chars) + cols - 1) // cols
    atlas = Image.new("RGBA", (cols * cell, rows * cell), (0, 0, 0, 0))
    draw = ImageDraw.Draw(atlas)
    widths = []
    for i, ch in enumerate(chars):
        cx, cy = (i % cols) * cell, (i // cols) * cell
        if ch != " ":
            draw.text((cx + 1, cy), ch, font=font, fill=(255, 255, 255, 255))
        try:
            widths.append(max(4, int(draw.textlength(ch, font=font)) + 2))
        except Exception:
            widths.append(8)
    atlas.save(FONT / "font.png")
    (FONT / "widths.txt").write_text(" ".join(map(str, widths)))
    print(f"Font: OK ({len(chars)} znakow)")


def wav(path, samples, rate=44100):
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(struct.pack(f"<{len(samples)}h", *samples))


def do_sounds():
    rng = random.Random(7)
    rate = 44100

    def tone(freq, ms, vol=0.5, slide_to=None):
        n = int(rate * ms / 1000)
        out = []
        for i in range(n):
            f = freq if slide_to is None else freq + (slide_to - freq) * i / n
            t = i / rate
            env = 1.0 - i / n
            out.append(int(vol * env * math.sin(2 * math.pi * f * t) * 32767))
        return out

    def noise(ms, vol=0.5, smooth=4):
        n = int(rate * ms / 1000)
        raw = [rng.uniform(-1, 1) for _ in range(n)]
        out = []
        for i in range(n):
            s = sum(raw[max(0, i - smooth + 1):i + 1]) / smooth
            env = 1.0 - i / n
            out.append(int(vol * env * s * 32767))
        return out

    def mix(a, b):
        n = max(len(a), len(b))
        a += [0] * (n - len(a))
        b += [0] * (n - len(b))
        return [max(-32767, min(32767, x + y)) for x, y in zip(a, b)]

    wav(SOUNDS / "click.wav", tone(950, 45, 0.35))
    wav(SOUNDS / "break.wav", noise(180, 0.6, 6))
    wav(SOUNDS / "place.wav", mix(tone(180, 90, 0.5), noise(60, 0.3, 3)))
    wav(SOUNDS / "pickup.wav", tone(620, 110, 0.4, slide_to=1240))
    wav(SOUNDS / "splash.wav", noise(350, 0.5, 10))
    print("Dzwieki: OK (5)")


def main():
    zip_path = Path(sys.argv[sys.argv.index("--zip") + 1]) if "--zip" in sys.argv else PROJECT / "teksturki.zip"
    if zip_path.exists():
        do_textures(zip_path)
    else:
        print(f"Brak {zip_path} - pomijam tekstury (font i dzwieki i tak zrobie).")
    do_font()
    do_sounds()
    print("GOTOWE.")


if __name__ == "__main__":
    main()
