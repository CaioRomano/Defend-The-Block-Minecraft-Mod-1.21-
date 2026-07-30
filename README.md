# Defend The Block

> Coloque o Nexus. Sobreviva a todas as noites. Se ele cair, o mundo acaba — literalmente.

Mod de Minecraft (Fabric) que transforma o mundo num modo de sobrevivencia por
ondas. Voce coloca um bloco — o **Nexus** — e a partir daquele momento **toda
noite** uma horda de mobs hostis marcha ate ele para destrui-lo. Eles cavam,
explodem paredes, montam escadas e sobem no que voce construir. Voce ergue
defesas, posiciona torretas e segura a linha.

Se o Nexus for destruido, o mod apaga a pasta do mundo. Nao ha segunda chance.

**Minecraft 1.20.1 e 1.21.1** · Fabric Loader + Fabric API

![Blocos e itens do mod](docs/images/blocos_e_itens.png)

<sub>As imagens acima sao as texturas do mod ampliadas, nao capturas de tela de jogo.</sub>

---

## O ciclo de jogo

1. **Prepare-se.** Fabrique o Nexus e escolha bem onde coloca-lo — a decisao e
   definitiva.
2. **Coloque o Nexus.** A partir daqui ele nao pode mais ser retirado, e os
   chunks em volta ficam permanentemente carregados.
3. **Anoiteceu.** A invasao comeca. Os mobs nascem em volta e vao direto ao
   bloco, atacando voce se cruzar o caminho.
4. **Segure a linha.** Muros, torretas, armadilhas. Os invasores respondem:
   creepers abrem brecha, zumbis mineram, aranhas escalam.
5. **Amanheceu.** A horda que sobrou se desfaz. Voce ganhou um dia para
   reconstruir — e a proxima noite vem pior.

---

## O que voce constroi

### Bloco Nexus

O coracao do modo. Craft:

```
O D O      O = Obsidiana
D E D      D = Diamante
O D O      E = Olho do Ender
```

- Funciona **so no Overworld**.
- **Apenas o primeiro** conta. Colocou um segundo? Ele volta para o inventario.
- Depois de colocado, **nao sai mais**: nao quebra na mao, nao quebra no
  criativo, resiste a explosao e pistao nao empurra. So os invasores tiram vida
  dele.
- 400 de vida por padrao. Quando chega a zero, acabou.

### Torreta de Flechas

![Textura da torreta](docs/images/textura_torreta.png)

Uma besta montada num trepe que **gira sozinha para o alvo** e atira. E uma
entidade, nao um bloco. Craft:

```
 C         C = Besta
I R I      R = Bloco de Redstone
I I I      I = Barra de Ferro
```

Clique com a mao para ver o status. O resto e interacao direta:

| Clique com... | Acontece |
|---|---|
| Flechas | Carrega municao (aceita flecha com efeito e espectral) |
| Ferro → Ouro → Diamante → Esmeralda | Sobe um nivel por vez |
| Livro encantado | Aplica Poder, Impacto, Chama, Perfuracao, Multitiro ou Carga Rapida |
| Agachado + mao vazia | Recolhe a torreta e as flechas que sobraram |

| Nivel | Dano | Alcance | Recarga | Vida | Municao |
|---|---|---|---|---|---|
| Madeira | 2.0 | 12 | 2.0s | 20 | 64 |
| Ferro | 3.0 | 16 | 1.6s | 30 | 96 |
| Ouro | 4.0 | 20 | 1.2s | 40 | 128 |
| Diamante | 5.5 | 26 | 0.8s | 55 | 192 |
| Esmeralda | 7.0 | 32 | 0.5s | 75 | 256 |

O nivel e a municao aparecem no nome, acima da torreta.

### Totem de Reuniao

```
G E G      G = Barra de Ouro
G P G      E = Esmeralda
 G         P = Perola do Ender
```

Usado com o **mesmo gesto de colocar um bloco** — clique direito, no ar ou numa
superficie — mas nao coloca nada. Puxa para o Nexus **todos os jogadores que
estiverem carregando um totem**. Util para reagrupar o time quando a noite cai.
Cooldown de 30s.

---

## O que vem atras de voce

### A horda

A invasao **nao tem numero fixo de mobs**. Ela spawna sem parar do anoitecer ate
o fim da noite, e o que segura a quantidade e o teto de invasores vivos ao mesmo
tempo — que tambem sobe a cada noite.

| Invasao | Mobs por lote | Intervalo | Teto de vivos | Nether |
|---|---|---|---|---|
| 1 | 2 | 2.8s | 35 | — |
| 3 | 4 | 2.4s | 45 | 27% |
| 8 | 6 | 1.4s | 70 | 28% |
| 14+ | 9+ | 0.5s | 100 | 26% |

Os invasores nascem **so dentro da area de ativacao**, de 1 a 3 chunks a partir
do chunk do Nexus. O chunk do Nexus em si nunca spawna nada. Fora dessa area, o
mundo segue com o spawn normal do vanilla.

**Todo mob hostil que nascer dentro de 3 chunks do Nexus** — spawn natural,
spawner ou ovo — e recrutado pela invasao e marcha ate o bloco. **Enderman e a
unica excecao.**

Reforcos do **Nether** entram cedo: magma cube e wither skeleton na invasao 2,
blaze e zombified piglin na 3, piglin brute na 5, hoglin na 6, ghast na 8. Da
invasao 3 em diante eles sao cerca de um quarto da horda.

### Eles nao sao burros

Zumbis e esqueletos vem com armadura e arma sorteadas, e a qualidade sobe a cada
noite: couro e madeira no comeco, depois ouro, malha, ferro, e diamante ou
netherite no fim. **A partir da invasao 4 as pecas comecam a vir encantadas**, e
o poder do encantamento cresce junto.

Alem disso, alguns invasores tem habilidades que mudam o jogo:

| Mob | Habilidade | Chance |
|---|---|---|
| Todos | **Sobem escadas** — inclusive as montadas por outros mobs | 100% |
| Aranha | Escala parede e **cospe teia** que prende o alvo | 25% |
| Creeper | **Se explode no obstaculo** quando nao ha caminho, abrindo passagem para o resto da horda | 35% |
| Zumbi | **Picareta**: minera o bloco que atrapalha | 12% |
| Zumbi | **Escadas**: monta uma coluna de escadas no obstaculo | 10% |
| Zumbi | **TNT**: planta e acende TNT na frente do muro | 5% |

As chances baixas sao de proposito: a maior parte da horda continua sendo de
mobs comuns, e o encontro com um zumbi carregando TNT vira um evento.

A picareta nao vence blocos muito duros (limite de dureza 30, entao obsidiana
segura) — para esses e preciso TNT ou creeper. Bedrock, barreira e o proprio
Nexus nunca sao quebrados.

---

## HUD e comandos

Um painel no canto superior direito acompanha a campanha: invasoes sobrevividas,
invasao atual, mobs vivos e nascidos na noite, multiplicador e a barra de vida do
Nexus.

| Comando | Nivel | O que faz |
|---|---|---|
| `/dtb status` | todos | Estado da campanha |
| `/dtb multiplier <valor>` | 2 | Multiplicador de mobs (0.1 a 20) |
| `/dtb forcewave` | 2 | Comeca a proxima invasao agora — otimo para testar |
| `/dtb stopwave` | 2 | Cancela a invasao atual |

---

## Instalacao

1. Instale o **Fabric Loader** para 1.20.1 ou 1.21.1.
2. Baixe a **Fabric API** da versao correspondente.
3. Ponha o jar do mod em `mods/`.

### Buildando do codigo

```bash
./gradlew buildAll                # gera os dois jars
./gradlew :fabric-1.20.1:build    # so 1.20.1
./gradlew :fabric-1.21.1:build    # so 1.21.1
```

Os jars saem em `fabric-<versao>/build/libs/`. Java 17 para 1.20.1, Java 21 para
1.21.1. Para testar em desenvolvimento: `./gradlew :fabric-1.21.1:runClient`.

As texturas sao geradas por script, de forma deterministica — os PNGs ja estao
commitados e so precisam ser refeitos se voce mexer na arte:

```bash
pip install pillow
python3 tools/generate_textures.py
python3 tools/generate_showcase.py
```

---

## Antes de jogar: o mod apaga o save

Quando o Nexus cai, o mod desconecta todos os jogadores, desliga o servidor e
**apaga a pasta do mundo**. E o game over do desafio, e e irreversivel.

Para ficar so com a mensagem de derrota, edite `config/defendtheblock.json`:

```json
{ "deleteWorldOnNexusDestroyed": false }
```

Vale testar o mod num mundo descartavel antes de comecar pra valer.

**Um detalhe que decorre disso:** os chunks em volta do Nexus ficam sempre
carregados, entao **a invasao acontece mesmo com todo mundo offline**. As ondas
comecam ao anoitecer independente de haver jogador, e o Nexus toma dano sem
ninguem defendendo. Algumas noites assim e ele cai.

---

## Configuracao

`config/defendtheblock.json`, criado no primeiro boot. Os principais:

| Chave | Padrao | O que faz |
|---|---|---|
| `nexusMaxHealth` | 400 | Vida do Nexus |
| `nexusDamagePerHit` | 3 | Dano por golpe de mob |
| `deleteWorldOnNexusDestroyed` | `true` | Apaga o mundo na derrota |
| `keepNexusChunksLoaded` | `true` | Mantem os chunks do Nexus carregados |
| `forcedChunkRadius` | 3 | Raio carregado, em chunks (7x7) |
| `spawnChunkRadiusMin` / `Max` | 1 / 3 | Area de spawn, em chunks |
| `attractionChunkRadius` | 3 | Raio de recrutamento, em chunks |
| `maxConcurrentInvaders` | 100 | Teto absoluto de invasores vivos |
| `mobMultiplier` | 1.0 | Multiplicador global do ritmo |
| `creeperBreachChance` | 0.35 | Chance de creeper arrombador |
| `spiderWebChance` | 0.25 | Chance de aranha com teia |
| `zombiePickaxeChance` | 0.12 | Chance de zumbi mineiro |
| `zombieLadderChance` | 0.10 | Chance de zumbi carpinteiro |
| `zombieTntChance` | 0.05 | Chance de zumbi com TNT |

Se o servidor sofrer nas invasoes altas, `maxConcurrentInvaders` e o botao certo.

---

## Como o projeto e organizado

```
common/                       codigo e assets compartilhados
  src/main/java/              toda a logica de jogo
  src/main/resources/         texturas, modelos, blockstates, lang
fabric-1.20.1/                build + compat 1.20.1 + receitas
fabric-1.21.1/                build + compat 1.21.1 + receitas
tools/generate_textures.py    gerador das texturas
tools/generate_showcase.py    gerador das imagens deste README
docs/images/                  imagens do README
```

Cada sub-projeto compila `common/` junto com o seu proprio pacote
`com.defendtheblock.compat`. Toda API que mudou entre 1.20.1 e 1.21 fica isolada
ali — construcao de `Identifier`, `EntityType.Builder`, `PersistentState`,
encantamentos, componentes de item, rede com `CustomPayload` e callbacks de
tooltip — e o codigo compartilhado nao sabe em qual versao esta rodando. As
receitas ficam por versao porque o campo `result` do JSON mudou de `item` para
`id` no 1.21.

---

## Status de verificacao

O codigo foi escrito e revisado e as texturas foram geradas e conferidas, mas
**o build nunca foi executado**: o ambiente onde o mod foi escrito bloqueia
`maven.fabricmc.net`, `libraries.minecraft.net` e `piston-meta.mojang.com`, entao
o Loom nao consegue baixar Minecraft, mappings nem a Fabric API.

Verificado localmente: todos os JSON validos, todos os PNG gerados e
inspecionados, e os 44 arquivos Java passam no parser do `javac` sem erro
estrutural — o que prova sintaxe, nao assinatura de API.

Espere alguns erros de compilacao no primeiro build. Os pontos de maior risco,
em ordem:

1. **`SoundEvents`** — no 1.21 varios campos viraram `RegistryEntry<SoundEvent>`
   em vez de `SoundEvent`.
2. **Construtores de `ArrowEntity` / `SpectralArrowEntity` no 1.21.1** — usei a
   forma de 4 argumentos.
3. **`EnchantmentHelper.enchant(...)` no 1.21.1** — assinatura de 5 argumentos.
4. **`Item.BLOCK_ITEMS`** em `ModItems` — depende do campo ser publico no yarn.
5. **`FlyingItemEntityRenderer::new`** e **`AbstractSkeletonEntity.updateAttackType()`**
   — aridade e visibilidade.

Sao erros de compilacao, nao bugs silenciosos: o compilador aponta arquivo e
linha, e a correcao e local.

---

## Licenca

Apache 2.0 — veja [LICENSE](LICENSE).
