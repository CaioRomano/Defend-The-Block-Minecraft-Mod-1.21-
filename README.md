# Defend The Block

Mod de Minecraft (Fabric) em que voce coloca um **Bloco Nexus** no mundo e, a partir
dai, **toda noite** uma invasao de mobs hostis marcha ate ele para destrui-lo.
Se o Nexus cair, o mundo e apagado.

Suporta **Minecraft 1.20.1** e **1.21.1**, com dois jars separados gerados a partir
do mesmo codigo.

---

## Como buildar

```bash
./gradlew buildAll          # gera os dois jars
./gradlew :fabric-1.20.1:build
./gradlew :fabric-1.21.1:build
```

Os jars saem em `fabric-<versao>/build/libs/`. Requer Java 17 (para 1.20.1) e
Java 21 (para 1.21.1).

Para rodar em desenvolvimento: `./gradlew :fabric-1.21.1:runClient`.

Requisitos em jogo: **Fabric Loader** + **Fabric API**.

### Texturas

Todas as texturas sao geradas por script, de forma deterministica:

```bash
pip install pillow
python3 tools/generate_textures.py
```

Os PNGs ja estao commitados em
`common/src/main/resources/assets/defendtheblock/textures/`; o script so precisa
ser rodado de novo se voce quiser mexer na arte.

---

## Aviso: o mod apaga o save

Quando o Nexus e destruido pelos mobs, o mod **desconecta todos os jogadores,
desliga o servidor e apaga a pasta do mundo**. Isso e proposital (e o "game over"
do desafio), mas e irreversivel.

Para desligar esse comportamento e ficar so com a mensagem de derrota, edite
`config/defendtheblock.json`:

```json
{ "deleteWorldOnNexusDestroyed": false }
```

Recomendado testar o mod em um mundo descartavel.

---

## Conteudo

### Bloco Nexus

Craft:

```
O D O      O = Obsidiana
D E D      D = Diamante
O D O      E = Olho do Ender
```

* So funciona no **Overworld**.
* **Apenas o primeiro** Nexus colocado no mundo conta. Se voce colocar um segundo,
  ele e removido e devolvido para o inventario.
* O chunk do Nexus e os **adjacentes** (3x3 por padrao) ficam **sempre
  carregados**, mesmo sem ninguem por perto — veja "Chunks carregados" abaixo.
* Depois de colocado **nao pode mais ser retirado** — nem quebrando, nem no
  criativo, nem com explosao, nem com pistao. So os invasores tiram vida dele.
* Vida padrao: 400 (configuravel).

### Torreta de Flechas

Craft:

```
 C         C = Besta
I R I      R = Bloco de Redstone
I I I      I = Barra de Ferro
```

E uma **entidade**, nao um bloco: uma besta montada num trepe que **gira para o
alvo** e atira sozinha.

| Interacao (clique direito) | Efeito |
|---|---|
| Flechas | Carrega municao (aceita flechas com efeito e flecha espectral) |
| Barra de ferro -> ouro -> diamante -> esmeralda | Sobe um nivel por vez |
| Livro encantado | Aplica Poder, Impacto, Chama, Perfuracao, Multitiro ou Carga Rapida |
| Agachado + mao vazia | Recolhe a torreta e as flechas que sobraram |
| Mao vazia | Mostra status (municao, nivel, dano, alcance, recarga) |

| Nivel | Dano | Alcance | Recarga | Vida | Municao |
|---|---|---|---|---|---|
| Madeira | 2.0 | 12 | 2.0s | 20 | 64 |
| Ferro | 3.0 | 16 | 1.6s | 30 | 96 |
| Ouro | 4.0 | 20 | 1.2s | 40 | 128 |
| Diamante | 5.5 | 26 | 0.8s | 55 | 192 |
| Esmeralda | 7.0 | 32 | 0.5s | 75 | 256 |

O nivel e a municao atual aparecem no nome acima da torreta.

### Totem de Reuniao

Craft:

```
G E G      G = Barra de Ouro
G P G      E = Esmeralda
 G         P = Perola do Ender
```

Usado com o **mesmo gesto de colocar um bloco** (clique direito, no ar ou numa
superficie), mas nao coloca nada: teleporta para o Nexus **todos os jogadores que
estiverem carregando um totem**. Cooldown padrao de 30s.

---

## As invasoes

* Comecam automaticamente **ao anoitecer** (tempo 13000), uma por noite.
* A invasao **nao tem um numero fechado de mobs**: ela spawna em lotes do
  anoitecer ate o fim da noite. O que segura a quantidade e o teto de invasores
  vivos ao mesmo tempo (`maxConcurrentInvaders`, padrao 60).
* Os invasores nascem num anel medido **em chunks** a partir do chunk do Nexus:
  de `spawnChunkRadiusMin` (2) ate `spawnChunkRadiusMax` (3). Os 2 chunks
  colados no Nexus ficam livres de spawn.
* **Todo mob hostil que nascer dentro de 3 chunks do Nexus** — spawn natural,
  spawner ou ovo — e recrutado pela invasao, ganha as habilidades de invasor e
  marcha ate o bloco. **Enderman e a unica excecao**: ele nunca participa.
* A cada invasao novos tipos entram no sorteio e o **ritmo de spawn aumenta**
  (o intervalo entre lotes encolhe e o lote cresce).
* Mobs do **Nether** entram a partir da invasao 3: magma cube, wither skeleton,
  blaze, zombified piglin, piglin brute, hoglin e ghast.
* Depois do fim da noite a onda para de spawnar, mas quem ja nasceu continua
  atacando; a onda fecha de vez ao amanhecer.
* Os invasores querem o Nexus, mas **atacam o jogador normalmente** se ele
  aparecer, com as mesmas habilidades.

### Progressao de equipamento

Zumbis e esqueletos recebem armadura e arma sorteadas, com qualidade subindo por
invasao: couro/madeira -> ouro -> malha -> ferro -> diamante/netherite. A partir da
invasao 4 as pecas comecam a vir **encantadas**, com o poder do encantamento
crescendo junto.

### Habilidades especiais

Subir escada e dado a **todos** os invasores. As demais sao sorteadas com chance
baixa, entao a maior parte da horda continua sendo de mobs comuns:

| Mob | Habilidade | Chance padrao |
|---|---|---|
| Todos | Sobem escadas (inclusive as construidas por outros mobs) | 100% |
| Aranha | Sobe parede (vanilla) e **cospe teia** que prende o alvo | 25% |
| Creeper | **Se explode no obstaculo** quando nao ha caminho ate o Nexus, abrindo passagem para o resto | 35% |
| Zumbi | **Picareta**: minera o bloco que atrapalha | 12% |
| Zumbi | **Escadas**: monta uma coluna de escadas no obstaculo | 10% |
| Zumbi | **TNT**: planta e acende TNT na frente do obstaculo | 5% |

A picareta nao quebra blocos muito duros (limite padrao: dureza 30, entao
obsidiana nao passa) — para esses e preciso TNT ou creeper. Bedrock, barreira e o
proprio Nexus nunca sao quebrados.

---

## Chunks carregados

O chunk onde o Nexus esta e os vizinhos ficam permanentemente carregados via o
forceload do proprio Minecraft (o mesmo do comando `/forceload`), entao a
invasao continua rodando com os jogadores longe. O estado e persistido pelo
vanilla em `forcedchunks.dat` e sobrevive a reinicios; o mod reaplica no boot
para pegar mudancas de config.

Os chunks ficam em nivel de *entity ticking*: mobs se movem, spawnam e batem no
Nexus normalmente ali dentro.

```json
{ "keepNexusChunksLoaded": true, "forcedChunkRadius": 3 }
```

`forcedChunkRadius` e **em chunks**: `0` = so o chunk do Nexus, `1` = 3x3,
`3` = 7x7 (o padrao). O padrao cobre exatamente a area de spawn (2 a 3 chunks) e
a area de atracao (3 chunks), entao a invasao roda inteira mesmo sem ninguem por
perto. Se voce mudar o valor, o mod solta a area antiga e marca a nova sozinho.

**Consequencia que vale atencao:** a invasao acontece **mesmo com todo mundo
offline**. As ondas comecam ao anoitecer independente de haver jogador, e o Nexus
toma dano sem ninguem para defender. Algumas noites seguidas sem defesa e ele
cai — e, com `deleteWorldOnNexusDestroyed` ligado, o mundo e apagado.

## HUD

Painel no canto superior direito com: invasoes sobrevividas, invasao atual, mobs
restantes / total da onda, multiplicador e a barra de vida do Nexus.

## Comandos

| Comando | Nivel | O que faz |
|---|---|---|
| `/dtb status` | todos | Mostra o estado da campanha |
| `/dtb multiplier <valor>` | 2 | Muda o multiplicador de mobs por invasao (0.1 a 20) |
| `/dtb forcewave` | 2 | Comeca a proxima invasao agora |
| `/dtb stopwave` | 2 | Cancela a invasao atual |

## Configuracao

`config/defendtheblock.json` — vida e dano do Nexus, exclusao do mundo, raios e
ritmo de spawn, limite de invasores simultaneos, chances de cada habilidade,
dureza maxima que a picareta quebra, dano e consumo de municao da torreta e
cooldown do totem.

---

## Organizacao do projeto

```
common/                     codigo e assets compartilhados pelas duas versoes
  src/main/java/            toda a logica de jogo
  src/main/resources/       texturas, modelos, blockstates, lang
fabric-1.20.1/              build + camada de compatibilidade 1.20.1 + receitas
fabric-1.21.1/              build + camada de compatibilidade 1.21.1 + receitas
tools/generate_textures.py  gerador das texturas
```

Cada sub-projeto compila `common/` junto com o seu proprio pacote
`com.defendtheblock.compat`. Toda API que mudou entre 1.20.1 e 1.21 (construcao
de `Identifier`, `EntityType.Builder`, `PersistentState`, encantamentos,
componentes de item, rede com `CustomPayload`, callbacks de tooltip) fica isolada
em `DtbCompat` / `DtbClientCompat`, e o codigo compartilhado nao sabe em qual
versao esta rodando. As receitas ficam por versao porque o campo `result` do JSON
mudou de `item` para `id` no 1.21.

---

## Status de verificacao

O codigo foi escrito e revisado, e as texturas foram geradas e conferidas, mas
**o build nao pode ser executado no ambiente onde o mod foi escrito**: a politica
de rede daquele ambiente bloqueia `maven.fabricmc.net`, `libraries.minecraft.net`
e `piston-meta.mojang.com`, entao o Loom nao consegue baixar Minecraft, mappings
nem a Fabric API. O que foi verificado localmente: todos os JSON sao validos,
todos os PNG foram gerados e inspecionados, e todos os 43 arquivos Java passam no
parser do `javac` sem nenhum erro estrutural (so faltam os simbolos do Minecraft).

Rode `./gradlew buildAll` numa maquina com acesso a rede para o primeiro build.
