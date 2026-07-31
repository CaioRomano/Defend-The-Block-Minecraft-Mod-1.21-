#!/usr/bin/env python3
"""Monta a imagem de vitrine do README a partir das texturas reais do mod.

Rode depois de generate_textures.py:  python3 tools/generate_showcase.py

Nao e screenshot de jogo: sao as proprias texturas do mod ampliadas em nearest
neighbor, para dar para ver o pixel art como ele e.
"""

import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "defendtheblock", "textures")
OUT = os.path.join(ROOT, "docs", "images")

BG = (24, 20, 34, 255)
PANEL = (38, 32, 54, 255)
ACCENT = (22, 200, 210, 255)
TEXT = (226, 226, 236, 255)

TILE = 128
PAD = 18
LABEL_H = 22


def load(*parts):
    return Image.open(os.path.join(TEX, *parts)).convert("RGBA")


def tile(img, size=TILE):
    """Amplia mantendo o pixel duro e centraliza num quadro."""
    scale = min(size // img.width, size // img.height)
    scaled = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
    frame = Image.new("RGBA", (size, size), PANEL)
    frame.alpha_composite(scaled, ((size - scaled.width) // 2, (size - scaled.height) // 2))
    return frame


def sheet(entries, columns=None):
    columns = columns or len(entries)
    rows = (len(entries) + columns - 1) // columns
    width = PAD + columns * (TILE + PAD)
    height = PAD + rows * (TILE + LABEL_H + PAD)

    canvas = Image.new("RGBA", (width, height), BG)
    draw = ImageDraw.Draw(canvas)

    for index, (img, label) in enumerate(entries):
        col = index % columns
        row = index // columns
        x = PAD + col * (TILE + PAD)
        y = PAD + row * (TILE + LABEL_H + PAD)

        canvas.alpha_composite(tile(img), (x, y))
        draw.rectangle([x, y, x + TILE - 1, y + TILE - 1], outline=ACCENT, width=1)

        text_width = draw.textlength(label)
        draw.text((x + (TILE - text_width) / 2, y + TILE + 6), label, fill=TEXT)

    return canvas


def main():
    os.makedirs(OUT, exist_ok=True)

    entries = [
        (load("block", "nexus_block.png"), "Nexus (lado)"),
        (load("block", "nexus_block_top.png"), "Nexus (topo)"),
        (load("item", "arrow_turret.png"), "Torreta"),
        (load("item", "gathering_totem.png"), "Totem"),
    ]
    path = os.path.join(OUT, "blocos_e_itens.png")
    sheet(entries).save(path)
    print("->", os.path.relpath(path, ROOT))

    tier_names = ["Madeira", "Ferro", "Ouro", "Diamante", "Esmeralda"]
    tier_entries = [
        (load("entity", f"arrow_turret_{tier}.png"), tier_names[tier])
        for tier in range(len(tier_names))
    ]
    path = os.path.join(OUT, "textura_torreta.png")
    sheet(tier_entries, columns=5).save(path)
    print("->", os.path.relpath(path, ROOT))


if __name__ == "__main__":
    main()
