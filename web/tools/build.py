#!/usr/bin/env python3
"""AiMINECRAFT Web - build:
1. textures.js z PNG (base64) z projektu Javy,
2. dist/AiMINECRAFT-web.html - cala gra w JEDNYM pliku (double-click).
Uzycie: python3 tools/build.py
"""
import base64
import io
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX_SRC = os.path.join(ROOT, "..", "AiMINECRAFT", "src", "main", "resources", "textures")

BLOCK_ORDER = [
    "grass_top", "grass_side", "dirt", "stone",
    "oak_log_side", "oak_log_top", "oak_leaves", "bedrock",
    "water", "sand", "cobblestone", "oak_planks",
    "glass", "coal_ore", "iron_ore", "gold_ore",
    "diamond_ore", "redstone_ore", "birch_log_side", "birch_log_top",
    "birch_leaves", "spruce_log_side", "spruce_log_top", "spruce_leaves",
    "snow", "dandelion", "poppy", "short_grass",
    "torch", "crafting_table_top", "crafting_table_side", "cactus_side",
    "cactus_top", "gravel", "obsidian", "ice",
    "bookshelf", "pumpkin_side", "pumpkin_top", "melon_side",
    "melon_top", "sponge", "bricks",
]

ITEM_ORDER = [
    "stick", "coal", "raw_iron", "raw_gold", "diamond", "redstone",
    "wooden_pickaxe", "wooden_axe", "wooden_shovel",
    "wooden_sword", "wooden_hoe",
    "stone_pickaxe", "stone_axe", "stone_shovel",
    "stone_sword", "stone_hoe",
]


def data_url(path):
    with open(path, "rb") as f:
        raw = f.read()
    return "data:image/png;base64," + base64.b64encode(raw).decode("ascii")


def build_textures():
    blocks = []
    for name in BLOCK_ORDER:
        p = os.path.join(TEX_SRC, "blocks", name + ".png")
        if not os.path.isfile(p):
            print(f"BRAK TEKSTURY: {p}")
            sys.exit(1)
        blocks.append(data_url(p))
    items = []
    for name in ITEM_ORDER:
        p = os.path.join(TEX_SRC, "items", name + ".png")
        if not os.path.isfile(p):
            print(f"BRAK TEKSTURY: {p}")
            sys.exit(1)
        items.append(data_url(p))
    out = ["/* AiMINECRAFT Web - tekstury (generowane, nie edytuj). */",
           '"use strict";',
           "var AIMC = (typeof window !== \"undefined\") ? (window.AIMC || (window.AIMC = {})) : {};",
           "AIMC.TEX_BLOCKS = ["]
    out += ['"' + u + '",' for u in blocks]
    out.append("];")
    out.append("AIMC.TEX_ITEMS = [")
    out += ['"' + u + '",' for u in items]
    out.append("];")
    dest = os.path.join(ROOT, "js", "textures.js")
    with open(dest, "w", encoding="utf-8") as f:
        f.write("\n".join(out) + "\n")
    size = os.path.getsize(dest)
    print(f"textures.js: {len(blocks)} blokow + {len(items)} itemow, {size} B")


JS_ORDER = ["config.js", "noise.js", "worldgen.js", "inventory.js", "textures.js",
            "audio.js", "save.js", "raycast.js", "world.js", "player.js",
            "ui.js", "main.js"]


def build_dist():
    with open(os.path.join(ROOT, "vendor", "three.min.js"), "r", encoding="utf-8") as f:
        three = f.read()
    with open(os.path.join(ROOT, "css", "style.css"), "r", encoding="utf-8") as f:
        css = f.read()
    with open(os.path.join(ROOT, "index.html"), "r", encoding="utf-8") as f:
        html = f.read()
    parts = [three]
    for name in JS_ORDER:
        p = os.path.join(ROOT, "js", name)
        with open(p, "r", encoding="utf-8") as f:
            parts.append(f.read())
    bundle = "\n;\n".join(parts)
    # Wstrzyknij CSS i JS w miejsce znacznikow.
    if "<!--AIMC_STYLE-->" not in html or "<!--AIMC_SCRIPTS-->" not in html:
        print("index.html nie ma znacznikow <!--AIMC_STYLE--> / <!--AIMC_SCRIPTS-->")
        sys.exit(1)
    html = html.replace("<!--AIMC_STYLE-->", "<style>\n" + css + "\n</style>")
    # Ochrona przed zamknieciem skryptu w bundlu.
    bundle = bundle.replace("</script", "<\\/script")
    scripts_tag = "<script>\n" + bundle + "\n</script>"
    # Usun devowe script/link tags, wstaw bundle.
    lines = []
    for line in html.split("\n"):
        s = line.strip()
        if '<link rel="stylesheet"' in s or "<script src=" in s:
            continue
        lines.append(line)
    html = "\n".join(lines)
    html = html.replace("<!--AIMC_SCRIPTS-->", scripts_tag)
    dest = os.path.join(ROOT, "dist", "AiMINECRAFT-web.html")
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    with open(dest, "w", encoding="utf-8") as f:
        f.write(html)
    print(f"dist: {os.path.getsize(dest)} B")


if __name__ == "__main__":
    build_textures()
    build_dist()
