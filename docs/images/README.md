# Imagens da documentacao

Duas origens diferentes moram aqui, e vale nao misturar:

- **Vitrines geradas por script** — `blocos_e_itens.png` e `textura_torreta.png`
  saem de `tools/generate_showcase.py`. Sao texturas do mod ampliadas, nao
  capturas de tela. Nao edite na mao: mexa no gerador e rode de novo.
- **Capturas de tela de jogo** — as da lista abaixo. Precisam ser tiradas
  jogando, e ainda **nao existem**.

## Capturas pendentes

Os dois READMEs — [portugues](../../README.md) e [ingles](../../README.en.md) —
ja tem o espaco demarcado para cada uma delas, com
a descricao no lugar exato onde a imagem entra. Salve o arquivo aqui com o nome
da tabela e troque o bloco `> 🖼️ ...` correspondente **nos dois arquivos** por
`![descricao](docs/images/NOME.png)`. Os nomes de arquivo sao os mesmos nas duas
linguas de proposito: imagem nao precisa de traducao, e assim uma captura serve
as duas versoes da documentacao.

| Arquivo | O que a captura deve mostrar |
|---|---|
| `hero.png` | Capa: o Nexus brilhando no centro de uma base cercada de torretas, horda ao fundo, de noite. Paisagem, ~1280x480. |
| `primeira-noite.png` | A primeira invasao chegando, vista de cima do muro, com o HUD visivel no canto. |
| `nexus.png` | O Nexus recem-colocado, com as particulas de ativacao, e a barra de vida cheia no HUD. |
| `torreta-niveis.png` | As cinco torretas lado a lado (madeira, ferro, ouro, diamante, esmeralda), mostrando a diferenca de textura. |
| `aba-torreta.png` | A aba de estatisticas aberta, com vida, municao, dano/alcance/recarga e modulos instalados. |
| `modulos.png` | Os nove livros de modulo lado a lado na aba do criativo, mostrando as cores distintas. |
| `horda.png` | Uma noite avancada com o teto alto de mobs, para dar a escala da densidade. |
| `habilidades.png` | Colagem: um zumbi minerando a parede, outro montando escada, um creeper prestes a explodir num obstaculo. |
| `hud.png` | Recorte do canto superior direito com o painel durante uma invasao ativa. |

## Dicas para as capturas

- Tire em **1920x1080** e recorte depois; o GitHub reduz bem, mas nao amplia.
- Esconda a mao/hotbar com **F1** nas fotos de cenario (menos nas do HUD, que
  precisam dele).
- Para as de invasao, `/dtb forcewave` comeca uma onda na hora sem esperar
  anoitecer, e `/dtb multiplier` ajuda a encher a tela de mobs para a foto.
- `/dtb removenexus` tira o Nexus **sem apagar o mundo**, entao da para montar
  varios cenarios no mesmo save de teste.
