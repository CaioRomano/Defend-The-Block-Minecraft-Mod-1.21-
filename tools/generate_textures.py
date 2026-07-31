#!/usr/bin/env python3
"""Gera todas as texturas PNG do mod Defend The Block.

Rode com:  python3 tools/generate_textures.py

As texturas sao geradas de forma deterministica (pixel art escrita a mao +
formas calculadas), entao rodar o script de novo produz exatamente os mesmos
arquivos. Saida em common/src/main/resources/assets/defendtheblock/textures/.
"""

import os
import random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "defendtheblock")
TEX = os.path.join(ASSETS, "textures")

TRANSPARENT = (0, 0, 0, 0)


def rgba(hex_str, alpha=255):
    hex_str = hex_str.lstrip("#")
    return (int(hex_str[0:2], 16), int(hex_str[2:4], 16), int(hex_str[4:6], 16), alpha)


def write(img, *parts):
    path = os.path.join(TEX, *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("  ->", os.path.relpath(path, ROOT))


def from_art(rows, palette, size=None):
    """Converte arte ASCII em imagem RGBA. '.' e sempre transparente."""
    height = len(rows)
    width = len(rows[0])
    for i, row in enumerate(rows):
        assert len(row) == width, "linha %d tem %d px, esperado %d" % (i, len(row), width)
    img = Image.new("RGBA", (width, height), TRANSPARENT)
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            px[x, y] = palette[ch]
    if size and size != (width, height):
        img = img.resize(size, Image.NEAREST)
    return img


# ---------------------------------------------------------------- nexus block

NEXUS_DARK = rgba("#151021")
NEXUS_STONE = rgba("#2b2440")
NEXUS_EDGE = rgba("#473c69")
NEXUS_RIM = rgba("#5d4f88")
GLOW_DEEP = rgba("#0b7f8c")
GLOW_MID = rgba("#16c8d2")
GLOW_HOT = rgba("#7ef4f7")
GLOW_CORE = rgba("#daffff")


def nexus_face(kind):
    """kind: 'side' | 'top' | 'bottom'."""
    img = Image.new("RGBA", (16, 16), NEXUS_STONE)
    px = img.load()
    rnd = random.Random({"side": 11, "top": 22, "bottom": 33}[kind])

    # Ruido sutil na pedra para nao ficar chapado.
    for y in range(16):
        for x in range(16):
            if rnd.random() < 0.22:
                px[x, y] = NEXUS_DARK if rnd.random() < 0.5 else NEXUS_EDGE

    # Moldura.
    for i in range(16):
        px[i, 0] = NEXUS_RIM
        px[i, 15] = NEXUS_DARK
        px[0, i] = NEXUS_RIM
        px[15, i] = NEXUS_DARK
    for i in range(1, 15):
        px[i, 1] = NEXUS_EDGE
        px[1, i] = NEXUS_EDGE
        px[i, 14] = NEXUS_DARK
        px[14, i] = NEXUS_DARK

    if kind == "bottom":
        # Face inferior: so pedra + rebites, sem nucleo brilhante.
        for (x, y) in [(4, 4), (11, 4), (4, 11), (11, 11)]:
            px[x, y] = NEXUS_RIM
            px[x, y + 1] = NEXUS_DARK
        return img

    cx = cy = 7.5
    if kind == "side":
        # Nucleo em losango.
        for y in range(16):
            for x in range(16):
                d = abs(x - cx) + abs(y - cy)
                if d <= 2.5:
                    px[x, y] = GLOW_CORE
                elif d <= 4.0:
                    px[x, y] = GLOW_HOT
                elif d <= 5.5:
                    px[x, y] = GLOW_MID
                elif d <= 6.5:
                    px[x, y] = GLOW_DEEP
        # Runas nos cantos.
        for (x, y) in [(3, 3), (12, 3), (3, 12), (12, 12)]:
            px[x, y] = GLOW_MID
    else:
        # Topo: anel de energia.
        for y in range(16):
            for x in range(16):
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if d <= 1.6:
                    px[x, y] = GLOW_CORE
                elif 3.2 <= d <= 4.4:
                    px[x, y] = GLOW_HOT if d < 3.9 else GLOW_MID
                elif 5.2 <= d <= 6.0:
                    px[x, y] = GLOW_DEEP
        # Quatro raios ligando o anel ao centro.
        for d in range(2, 4):
            px[7, 7 - d + 1] = GLOW_MID
            px[8, 8 + d - 1] = GLOW_MID
            px[7 - d + 1, 8] = GLOW_MID
            px[8 + d - 1, 7] = GLOW_MID
    return img


# ------------------------------------------------------------- itens (16x16)

TURRET_ITEM_PALETTE = {
    "A": rgba("#6b4a2a"),  # madeira escura do arco
    "W": rgba("#8a6236"),  # madeira clara
    "I": rgba("#b9b9c4"),  # ferro
    "i": rgba("#7d7d88"),  # ferro sombra
    "S": rgba("#e9e2d2"),  # corda
    "M": rgba("#8f8f9b"),  # pernas de metal
    "m": rgba("#5f5f6b"),  # metal sombra
    "C": rgba("#16c8d2"),  # cristal
}

TURRET_ITEM_ART = [
    "................",
    "..A..........A..",
    "..AA........AA..",
    "...AA......AA...",
    "....AAWWWWAA....",
    "...SAAWIIWAAS...",
    "....SWWIIWWS....",
    ".....SSIISS.....",
    ".......IIi......",
    "......ICCIi.....",
    "......MMMMi.....",
    ".....mMM.MMm....",
    "....mM.....Mm...",
    "...mM.......Mm..",
    "..mM.........Mm.",
    "..m...........m.",
]

TOTEM_PALETTE = {
    "D": rgba("#6d4f10"),
    "G": rgba("#a37a1e"),
    "Y": rgba("#e8c53a"),
    "C": rgba("#28e6e6"),
    "W": rgba("#c6f9ff"),
}

TOTEM_ART = [
    "................",
    ".....DGGGGD.....",
    "....DGYYYYGD....",
    "...DGYYCCYYGD...",
    "...GYYCWWCYYG...",
    "...GYCWWWWCYG...",
    "...GYCWWWWCYG...",
    "...GYYCWWCYYG...",
    "...DGYYCCYYGD...",
    "....DGYYYYGD....",
    ".....GYYYYG.....",
    ".....GY..YG.....",
    "....GYY..YYG....",
    "....GY....YG....",
    "...DG......GD...",
    "...D........D...",
]


# ------------------------------------------------------- textura da entidade
#
# Layout UV usado por TurretModel (64x64):
#   base      8x4x8   -> UV (0, 0)    regiao 32x12
#   pedestal  4x6x4   -> UV (0, 13)   regiao 16x10
#   cabeca    5x3x5   -> UV (0, 24)   regiao 20x8
#   coronha   2x2x10  -> UV (0, 34)   regiao 24x12
#   gatilho   2x2x2   -> UV (34, 22)  regiao 8x4
#   limbo     6x1x2   -> UV (34, 14)  regiao 16x3
#   (o segundo limbo reusa o mesmo UV espelhado, ver TurretModel)
#   trave     12x1x2  -> UV (0, 47)   regiao 28x3
#   corda     12x1x1  -> UV (0, 51)   regiao 26x2
#
# A besta tem uma textura por nivel (0=madeira .. 4=esmeralda): a coronha de
# madeira e o suporte de pedra ficam iguais em todo nivel (o "detalhe original
# da besta"), so o mecanismo (trave, bracos, corda, gatilho, calha, faixa do
# pedestal e a gema) muda de cor para remeter ao material daquele nivel.

STONE = rgba("#6f6f79")
STONE_D = rgba("#4c4c55")
IRON = rgba("#c2c2cc")
IRON_D = rgba("#8b8b96")
WOOD = rgba("#7d5730")
WOOD_D = rgba("#5a3d20")

TURRET_TIER_PALETTES = [
    {"fitting": rgba("#b9b9c4"), "fitting_d": rgba("#7d7d88"), "gem": rgba("#d8d8e0")},  # 0 madeira: ferro neutro
    {"fitting": rgba("#dcdce4"), "fitting_d": rgba("#9a9aa4"), "gem": rgba("#eaeaf2")},  # 1 ferro: metal polido
    {"fitting": rgba("#e8c53a"), "fitting_d": rgba("#a37a1e"), "gem": rgba("#fbe98a")},  # 2 ouro
    {"fitting": rgba("#6be8f2"), "fitting_d": rgba("#16c8d2"), "gem": rgba("#c6f9ff")},  # 3 diamante
    {"fitting": rgba("#4ee88a"), "fitting_d": rgba("#1f9c52"), "gem": rgba("#a8ffce")},  # 4 esmeralda
]


def fill(px, x0, y0, w, h, color):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            px[x, y] = color


def hatch(px, x0, y0, w, h, color, step=3, offset=0):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if (x + y + offset) % step == 0:
                px[x, y] = color


def turret_entity_texture(tier):
    palette = TURRET_TIER_PALETTES[tier]
    fitting = palette["fitting"]
    fitting_d = palette["fitting_d"]
    gem = palette["gem"]

    img = Image.new("RGBA", (64, 64), TRANSPARENT)
    px = img.load()

    # base de pedra: e so o suporte, fica igual em todos os niveis
    fill(px, 0, 0, 32, 12, STONE)
    hatch(px, 0, 0, 32, 12, STONE_D, 4)
    fill(px, 0, 0, 32, 1, STONE_D)
    fill(px, 0, 11, 32, 1, STONE_D)

    # pedestal de ferro, com uma faixa colorida indicando o nivel
    fill(px, 0, 13, 16, 10, IRON_D)
    hatch(px, 0, 13, 16, 10, IRON, 3)
    fill(px, 0, 20, 16, 2, fitting_d)

    # cabeca giratoria (ferro + gema colorida por nivel na frente)
    fill(px, 0, 24, 20, 8, IRON)
    hatch(px, 0, 24, 20, 8, IRON_D, 5)
    fill(px, 12, 27, 3, 3, fitting)
    px[13, 28] = gem

    # coronha de madeira: o "detalhe original da besta", igual em todo nivel
    fill(px, 0, 34, 24, 12, WOOD)
    hatch(px, 0, 34, 24, 12, WOOD_D, 3)
    fill(px, 0, 34, 24, 1, WOOD_D)
    # calha metalica em cima da coronha, colorida por nivel
    fill(px, 2, 36, 20, 2, fitting_d)

    # bracos do arco: o mecanismo em si, recolorido por nivel
    fill(px, 34, 14, 16, 3, fitting_d)
    hatch(px, 34, 14, 16, 3, fitting, 2)
    fill(px, 34, 18, 16, 3, fitting_d)
    hatch(px, 34, 18, 16, 3, fitting, 2, 1)

    # gatilho / guarda-mao
    fill(px, 34, 22, 8, 4, WOOD_D)
    hatch(px, 34, 22, 8, 4, fitting, 3)

    # trave onde os bracos se apoiam
    fill(px, 0, 47, 28, 3, fitting_d)
    hatch(px, 0, 47, 28, 3, fitting, 3)

    # corda, fina e discreta
    fill(px, 0, 51, 26, 2, fitting_d)

    return img


def mod_icon():
    """Icone 128x128 do mod: bloco Nexus com brilho."""
    base = nexus_face("side").resize((128, 128), Image.NEAREST)
    overlay = Image.new("RGBA", (128, 128), TRANSPARENT)
    px = overlay.load()
    for y in range(128):
        for x in range(128):
            if x < 8 or y < 8 or x > 119 or y > 119:
                px[x, y] = rgba("#0b0814")
    base.alpha_composite(overlay)
    return base


def main():
    print("Gerando texturas em", os.path.relpath(TEX, ROOT))
    write(nexus_face("side"), "block", "nexus_block.png")
    write(nexus_face("top"), "block", "nexus_block_top.png")
    write(nexus_face("bottom"), "block", "nexus_block_bottom.png")
    write(from_art(TURRET_ITEM_ART, TURRET_ITEM_PALETTE), "item", "arrow_turret.png")
    write(from_art(TOTEM_ART, TOTEM_PALETTE), "item", "gathering_totem.png")
    for tier in range(len(TURRET_TIER_PALETTES)):
        write(turret_entity_texture(tier), "entity", f"arrow_turret_{tier}.png")

    icon_path = os.path.join(ASSETS, "icon.png")
    os.makedirs(os.path.dirname(icon_path), exist_ok=True)
    mod_icon().save(icon_path)
    print("  ->", os.path.relpath(icon_path, ROOT))
    print("ok")


if __name__ == "__main__":
    main()
