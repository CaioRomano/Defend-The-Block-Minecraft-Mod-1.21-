#!/usr/bin/env python3
"""Gera tudo o que os livros de modulo da torreta precisam, a partir de uma tabela so.

Rode com:  python3 tools/generate_turret_modules.py

Para cada modulo declarado em MODULES abaixo, escreve:

  - a textura 16x16 do livro (capa escura, lombada e brasao na cor do modulo);
  - o modelo de item;
  - a receita, nas DUAS versoes — o campo de resultado do JSON e "item" no
    1.20.1 e "id" no 1.21.1, e essa e a unica diferenca entre elas;
  - as tres chaves de traducao (nome do item, nome do modulo, tooltip) em
    en_us e pt_br, preservando o que ja existe nos arquivos.

Acrescentar um modulo novo e: somar uma entrada aqui, somar a constante
correspondente em TurretModifier.java, e rodar o script. Nada mais precisa ser
tocado — ModItems e ModItemGroups criam e registram os itens direto do enum.

Nao depende de Pillow (ao contrario de generate_textures.py): o PNG e escrito
na mao com struct + zlib. A saida e deterministica, entao rodar de novo produz
exatamente os mesmos arquivos.
"""
import json
import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = f"{ROOT}/common/src/main/resources/assets/defendtheblock"

# id -> (cor da fita, en, pt, tooltip en, tooltip pt, receita)
# A receita e uma tupla (pattern, key) onde 'B' = livro, e o resto e o
# ingrediente tematico do modulo.
MODULES = {
    "range": (
        (0x4F, 0xC3, 0xF7),
        "Module: Range", "Modulo: Alcance",
        "Turret range +15% per grade.",
        "Alcance da torreta +15% por grau.",
        ([" S ", "SBS", " E "], {"S": "minecraft:spyglass", "B": "minecraft:book", "E": "minecraft:ender_pearl"}),
    ),
    "damage": (
        (0xE5, 0x53, 0x53),
        "Module: Damage", "Modulo: Dano",
        "Arrow damage +20% per grade.",
        "Dano da flecha +20% por grau.",
        ([" F ", "DBD", " F "], {"F": "minecraft:flint", "B": "minecraft:book", "D": "minecraft:diamond"}),
    ),
    "fortitude": (
        (0x8D, 0xD9, 0x6C),
        "Module: Fortitude", "Modulo: Fortificacao",
        "Turret max health +25% per grade.",
        "Vida maxima da torreta +25% por grau.",
        ([" I ", "IBI", " O "], {"I": "minecraft:iron_block", "B": "minecraft:book", "O": "minecraft:obsidian"}),
    ),
    "rapid": (
        (0xFF, 0xC4, 0x4F),
        "Module: Rate of Fire", "Modulo: Cadencia",
        "Reload time -15% per grade.",
        "Tempo de recarga -15% por grau.",
        ([" R ", "RBR", " Q "], {"R": "minecraft:redstone_block", "B": "minecraft:book", "Q": "minecraft:quartz_block"}),
    ),
    "quiver": (
        (0xC8, 0x8B, 0x4F),
        "Module: Quiver", "Modulo: Aljava",
        "Magazine capacity +50% per grade.",
        "Capacidade do carregador +50% por grau.",
        ([" L ", "ABA", " C "], {"L": "minecraft:leather", "B": "minecraft:book", "A": "minecraft:arrow", "C": "minecraft:chest"}),
    ),
    "volley": (
        (0xF2, 0x8B, 0xD0),
        "Module: Volley", "Modulo: Salva",
        "+1 arrow per shot, at no extra ammo cost.",
        "+1 flecha por disparo, sem custo extra de municao.",
        (["AAA", "ABA", " D "], {"A": "minecraft:arrow", "B": "minecraft:book", "D": "minecraft:dispenser"}),
    ),
    "scavenger": (
        (0xB5, 0xA6, 0x42),
        "Module: Scavenger", "Modulo: Catador",
        "20% chance per grade to fire without spending an arrow.",
        "20% de chance por grau de atirar sem gastar flecha.",
        ([" H ", "EBE", " F "], {"H": "minecraft:hopper", "B": "minecraft:book", "E": "minecraft:emerald", "F": "minecraft:feather"}),
    ),
    "frost": (
        (0x7E, 0xD4, 0xF7),
        "Module: Frost", "Modulo: Gelo",
        "Arrows apply Slowness.",
        "As flechas aplicam Lentidao.",
        ([" I ", "PBP", " I "], {"I": "minecraft:blue_ice", "B": "minecraft:book", "P": "minecraft:packed_ice"}),
    ),
    "venom": (
        (0x9B, 0xD9, 0x4F),
        "Module: Venom", "Modulo: Veneno",
        "Arrows apply Poison.",
        "As flechas aplicam Veneno.",
        ([" S ", "FBF", " S "], {"S": "minecraft:spider_eye", "B": "minecraft:book", "F": "minecraft:fermented_spider_eye"}),
    ),
}


def png(path, pixels, w=16, h=16):
    """Escreve um PNG RGBA 8-bit sem depender de biblioteca externa."""
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *pixels[y][x]) for x in range(w)) for y in range(h))

    def chunk(tag, data):
        c = tag + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)

    out = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw, 9))
           + chunk(b"IEND", b""))
    with open(path, "wb") as f:
        f.write(out)


def book_texture(accent):
    """Livro fechado de frente: capa escura, lombada e brasao na cor do modulo."""
    T = (0, 0, 0, 0)
    OUT = (24, 18, 15, 255)
    COVER = (98, 42, 38, 255)
    COVER_HI = (132, 58, 52, 255)
    COVER_SH = (68, 28, 26, 255)
    PAGE = (236, 230, 210, 255)
    PAGE_SH = (196, 188, 166, 255)
    GOLD = (216, 178, 76, 255)
    GOLD_SH = (158, 126, 46, 255)
    A = (*accent, 255)
    A_HI = tuple(min(255, c + 55) for c in accent) + (255,)
    A_SH = tuple(int(c * 0.55) for c in accent) + (255,)

    px = [[T] * 16 for _ in range(16)]

    def rect(x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                px[y][x] = c

    # contorno, capa e volume
    rect(2, 1, 13, 14, OUT)
    rect(3, 2, 12, 13, COVER)
    rect(3, 2, 12, 2, COVER_HI)
    rect(3, 13, 12, 13, COVER_SH)

    # lombada na cor do modulo
    rect(3, 2, 4, 13, A_SH)
    rect(3, 3, 3, 12, A)

    # miolo de paginas na borda direita
    rect(11, 3, 12, 12, PAGE)
    rect(11, 3, 11, 12, PAGE_SH)

    # brasao: losango na cor do modulo, o que identifica cada livro de relance
    for y, xs in ((5, (7,)), (6, (6, 7, 8)), (7, (5, 6, 7, 8, 9)),
                  (8, (5, 6, 7, 8, 9)), (9, (6, 7, 8)), (10, (7,))):
        for x in xs:
            px[y][x] = A
    px[6][6] = A_HI
    px[7][5] = A_HI
    px[7][6] = A_HI
    px[9][8] = A_SH
    px[8][9] = A_SH

    # fecho dourado prendendo as paginas
    rect(10, 7, 12, 8, GOLD)
    rect(10, 8, 12, 8, GOLD_SH)

    return px


def main():
    os.makedirs(f"{ASSETS}/models/item", exist_ok=True)
    os.makedirs(f"{ASSETS}/textures/item", exist_ok=True)

    lang_en, lang_pt = {}, {}

    for key, (accent, en, pt, ten, tpt, (pattern, ingredients)) in MODULES.items():
        item = f"turret_module_{key}"

        png(f"{ASSETS}/textures/item/{item}.png", book_texture(accent))

        with open(f"{ASSETS}/models/item/{item}.json", "w") as f:
            json.dump({"parent": "minecraft:item/generated",
                       "textures": {"layer0": f"defendtheblock:item/{item}"}}, f, indent=2)
            f.write("\n")

        # Receita: mesma forma nas duas versoes, so o campo do resultado muda
        # ("item" no 1.20.1, "id" no 1.21).
        for version, result_key in (("1.20.1", "item"), ("1.21.1", "id")):
            path = f"{ROOT}/fabric-{version}/src/main/resources/data/defendtheblock/recipes/{item}.json"
            with open(path, "w") as f:
                json.dump({
                    "type": "minecraft:crafting_shaped",
                    "category": "equipment",
                    "pattern": pattern,
                    "key": {k: {"item": v} for k, v in ingredients.items()},
                    "result": {result_key: f"defendtheblock:{item}", "count": 1},
                }, f, indent=2)
                f.write("\n")

        lang_en[f"item.defendtheblock.{item}"] = en
        lang_en[f"turret_module.defendtheblock.{key}"] = en.split(": ", 1)[1]
        lang_en[f"tooltip.turret_module.defendtheblock.{key}"] = ten
        lang_pt[f"item.defendtheblock.{item}"] = pt
        lang_pt[f"turret_module.defendtheblock.{key}"] = pt.split(": ", 1)[1]
        lang_pt[f"tooltip.turret_module.defendtheblock.{key}"] = tpt

    shared_en = {
        "tooltip.defendtheblock.turret_module.grade": "Apply again on the same turret to raise its grade (max %s).",
        "tooltip.defendtheblock.turret_module.slots": "A turret holds up to %s module types.",
        "turret.defendtheblock.module_installed": "Module %s %s installed on the turret.",
        "turret.defendtheblock.module_upgraded": "Module %s raised to %s.",
        "turret.defendtheblock.module_max_grade": "%s is already at its highest grade.",
        "turret.defendtheblock.module_no_slot": "This turret already has %s modules. Use a grindstone to strip them.",
        "turret.defendtheblock.module_none": "This turret has no modules to strip.",
        "turret.defendtheblock.module_stripped": "All modules stripped from the turret.",
        "screen.defendtheblock.modules_title": "Modules (%s / %s)",
        "screen.defendtheblock.modules_empty": "None. Right click holding a module book.",
        "screen.defendtheblock.module_entry": "- %s %s",
        "tooltip.defendtheblock.arrow_turret.5":
            "Module books customise one attribute each - up to 2 per turret.",
    }
    shared_pt = {
        "tooltip.defendtheblock.turret_module.grade": "Aplique de novo na mesma torreta para subir o grau (max %s).",
        "tooltip.defendtheblock.turret_module.slots": "Cada torreta aceita ate %s tipos de modulo.",
        "turret.defendtheblock.module_installed": "Modulo %s %s instalado na torreta.",
        "turret.defendtheblock.module_upgraded": "Modulo %s subiu para %s.",
        "turret.defendtheblock.module_max_grade": "%s ja esta no grau maximo.",
        "turret.defendtheblock.module_no_slot": "Esta torreta ja tem %s modulos. Use um rebolo para desmontar.",
        "turret.defendtheblock.module_none": "Esta torreta nao tem nenhum modulo para desmontar.",
        "turret.defendtheblock.module_stripped": "Todos os modulos foram removidos da torreta.",
        "screen.defendtheblock.modules_title": "Modulos (%s / %s)",
        "screen.defendtheblock.modules_empty": "Nenhum. Clique com um livro de modulo na mao.",
        "screen.defendtheblock.module_entry": "- %s %s",
        "tooltip.defendtheblock.arrow_turret.5":
            "Livros de modulo personalizam um atributo cada - ate 2 por torreta.",
    }
    lang_en.update(shared_en)
    lang_pt.update(shared_pt)

    for name, extra in (("en_us", lang_en), ("pt_br", lang_pt)):
        path = f"{ASSETS}/lang/{name}.json"
        with open(path) as f:
            data = json.load(f)
        before = len(data)
        data.update(extra)
        with open(path, "w") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
        print(f"{name}: {before} -> {len(data)} chaves")

    print(f"{len(MODULES)} modulos: texturas, modelos e receitas (x2 versoes) gerados")


if __name__ == "__main__":
    main()
