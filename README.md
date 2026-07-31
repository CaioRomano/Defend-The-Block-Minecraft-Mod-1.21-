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

Os tres itens do mod vivem na **propria aba do inventario criativo**,
"Defend The Block" — nao espalhados pelas abas vanilla de Combate ou Blocos
Funcionais.

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
entidade, nao um bloco. O icone do item e **visto de cima**: a besta deitada
sobre a base, corda e bracos para cima na imagem, coronha de madeira descendo
ate o topo da base metalica. Craft:

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

Ela **so atira quando esta de fato apontada para o alvo** — gira primeiro, dispara
depois, nunca o contrario.

| Nivel | Dano | Alcance | Recarga | Vida | Municao | Custo do upgrade |
|---|---|---|---|---|---|---|
| Madeira | 2.0 | 12 | 2.0s | 20 | 64 | — |
| Ferro | 3.0 | 16 | 1.6s | 30 | 96 | 20x Barra de Ferro |
| Ouro | 4.0 | 20 | 1.2s | 40 | 128 | 7x Barra de Ouro |
| Diamante | 5.5 | 26 | 0.8s | 55 | 192 | 4x Diamante |
| Esmeralda | 7.0 | 32 | 0.5s | 75 | 256 | 2x Esmeralda |

O custo cai conforme o material fica mais raro: ferro (facil de juntar em
quantidade) pede bem mais unidades, esmeralda (o mais raro) pede so 2.

O nivel e a municao aparecem no nome, acima da torreta. O **visual muda a cada
upgrade**: a coronha de madeira e o suporte de pedra sao o "detalhe original da
besta" e ficam iguais em todo nivel, mas o mecanismo — trave, bracos, corda,
gatilho, a faixa do pedestal e a gema — troca de cor para o material daquele
nivel (cinza neutro → cinza polido → dourado → ciano → verde).

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
| Todos, exceto creeper | **Sobem escadas** — inclusive as montadas por outros mobs | 100% |
| Todos, exceto creeper | **Arrombam portas fechadas** em vez de so abri-las (veja abaixo) | 100% |
| Aranha | Escala parede e **cospe teia** que prende o alvo | 25% |
| Creeper | **Se explode no obstaculo** quando nao ha caminho, abrindo passagem para o resto da horda | 35% |
| Zumbi | **Picareta**: minera o bloco que atrapalha | 12% |
| Zumbi | **Escadas**: monta uma coluna de escadas no obstaculo | 10% |
| Zumbi | **TNT**: planta e acende TNT na frente do muro | 5% |

As chances baixas sao de proposito: a maior parte da horda continua sendo de
mobs comuns, e o encontro com um zumbi carregando TNT vira um evento.

### Portas: arrombadas, nunca so abertas

Nenhum invasor (exceto o creeper, que tem seu proprio jeito de passar) usa uma
porta como se fosse um jogador abrindo-a. Uma porta fechada no caminho e sempre
tratada como obstaculo: o mob para e **arromba a porta a base de golpes**, sem
precisar de nenhuma ferramenta.

Portas de ferro sao mais resistentes que as de madeira, na mesma proporcao da
dureza real do bloco no jogo — a mesma logica que ja faz a picareta do zumbi
demorar mais em paredes mais duras.

A picareta nao vence blocos muito duros (limite de dureza 30, entao obsidiana
segura) — para esses e preciso TNT ou creeper. Bedrock, barreira e o proprio
Nexus nunca sao quebrados.

### Todo mob ataca o Nexus, nao so quem chega perto

**Qualquer invasor que alcance o Nexus causa dano nele** — zumbi, esqueleto,
creeper, blaze, o que for. Isso inclui o **creeper**, que se explode em cima do
bloco em vez de so bater nele. As IAs de movimento dos invasores tem prioridade
bem acima das goals de vagar/olhar do vanilla, entao um creeper (ou qualquer
outro mob) nao fica perambulando a toa em vez de seguir ate o alvo.

**Esqueletos atiram flechas que danificam o Nexus** quando o acertam
diretamente, alem do golpe corpo a corpo quando ficam perto.

**Se o Nexus estiver num lugar alto ou suspenso no ar**, o invasor que ficar
preso por tempo demais comeca a **construir um caminho de blocos** (cobblestone)
embaixo dos proprios pes, pulando ate ganhar altura ou atravessar um vao — uma
heuristica simples para nao deixar o Nexus inalcancavel so por estar no ar.
Desligue com `invadersCanBridge: false` se preferir que mobs nunca coloquem
bloco no mundo.

### Jogador e torreta sao alvos — mas o Nexus e a prioridade

Os invasores **enxergam a torreta a distancia**, do mesmo jeito que enxergariam
um jogador — nao precisam levar um tiro dela primeiro para reagir. E, assim como
o jogador, atacam o que estiver **no caminho** ate o Nexus.

O importante e que isso nao vira uma cacada: **o Nexus continua sendo a
prioridade real**. Um invasor so briga de verdade com um jogador ou uma torreta
quando ela esta genuinamente perto (por padrao, dentro de 6 blocos) — se estiver
mais longe que isso, o mob simplesmente esquece aquele alvo e volta a caminhar
para o bloco. Isso evita a situacao de, por exemplo, 3 torretas em fila
separadas por 5 blocos cada: o invasor nao precisa matar as tres em sequencia
antes de sequer tentar o Nexus — ele so briga com o que estiver bloqueando a
passagem no momento, sem sair do caminho para cacar algo distante. Ajustavel em
`nexusPriorityEngageRange`.

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

**Use sempre o `./gradlew` do repositorio, nao um Gradle instalado na maquina.**
O wrapper esta fixado no **Gradle 8.10.2** porque o **Fabric Loom 1.7** usa a API
incubadora `Problems.forNamespace(String)`, que o Gradle removeu na versao 8.11.
Rodar com Gradle 8.11 ou mais novo quebra logo na aplicacao do plugin:

```
Failed to apply plugin 'fabric-loom'.
> Could not create an instance of type ...LoomProblemReporter.
   > 'org.gradle.api.problems.ProblemReporter
      org.gradle.api.problems.Problems.forNamespace(java.lang.String)'
```

Para subir o Gradle acima de 8.11, e preciso subir o Loom junto (1.9+) em
`build.gradle`.

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
| `invadersCanBridge` | `true` | Mobs constroem caminho de blocos quando o Nexus esta elevado |
| `turretDetectionRadius` | 64.0 | Raio (blocos) no qual invasores enxergam a torreta como alvo |
| `nexusPriorityEngageRange` | 6.0 | Raio (blocos) para brigar com jogador/torreta antes de voltar ao Nexus |
| `doorBreakTicksPerHardness` | 10 | Ritmo de arrombamento de portas (ferro demora mais que madeira) |

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

**`./gradlew buildAll` compila com sucesso as duas versoes** (Fabric Loom
1.7.4 + Gradle 8.10.2, testado numa maquina com acesso a rede). Isso prova que
o codigo bate com as APIs do Minecraft e do Fabric nas duas versoes — nao
prova que o mod funciona jogando. Ninguem colocou o Nexus, viu a torreta girar
nem sobreviveu a uma invasao ainda.

Historico de como chegou aqui: o codigo foi escrito num ambiente que bloqueia
`maven.fabricmc.net`, `libraries.minecraft.net` e `piston-meta.mojang.com`, entao
o Loom nao conseguia baixar Minecraft, mappings nem a Fabric API. Antes do
primeiro build de verdade, o que dava para verificar ali era: todos os JSON
validos, todos os PNG gerados e inspecionados, e os arquivos Java passando no
parser do `javac` sem erro estrutural — o que prova sintaxe, nao assinatura de
API. A primeira compilacao de verdade, feita depois, apontou 5 erros, todos
corrigidos:

| Erro | Versao | Correcao |
|---|---|---|
| `LookControl.lookAtEntity` nao existe | ambas | e `lookAt(Entity, float, float)` |
| `HudRenderCallback` passa `RenderTickCounter`, nao `float` | 1.21.1 | lambda com tipo inferido, serve nas duas |
| `SoundEvents.ITEM_CROSSBOW_LOADING_END` virou `RegistryEntry` | 1.21.1 | constante `DtbCompat.CROSSBOW_LOADED` |
| `PersistentProjectileEntity.setPunch` sumiu | 1.21.1 | `DtbCompat.applyPunch` |
| `setPierceLevel` virou privado | 1.21.1 | `DtbCompat.applyPiercing` |

**Limitacao conhecida no 1.21:** Impacto e Perfuracao da torreta ficam **sem
efeito**. O 1.20.5 passou a derivar esses dois dos encantamentos do
`ItemStack` da arma que disparou, e a torreta guarda os niveis dela em NBT
proprio, nao numa arma. Os outros quatro encantamentos (Poder, Chama, Multitiro,
Carga Rapida) funcionam normalmente nas duas versoes, e no 1.20.1 os seis
funcionam. Para resolver, a torreta precisa montar um stack de besta encantado e
passar como arma no construtor da flecha.

**Ainda nao testado em jogo.** Compilar prova assinatura de API, nao
comportamento: falta rodar `runClient`, colocar o Nexus, forcar uma invasao com
`/dtb forcewave` e ver se a torreta realmente gira e atira. E nessa etapa que
problemas de mixin (nomes de campo do `MobEntityAccessor`) ou de logica de jogo
apareceriam, se existirem.

### Mudancas depois do playtest

As secoes acima ja refletem os ajustes pedidos depois de jogar, em duas
rodadas. **Nenhuma das duas passou por um novo `buildAll` nem por teste em
jogo ainda** — a verificacao continua sendo so estrutural (`javac` sem erro),
a mesma limitacao de sempre.

**Primeira rodada:** redesign da torreta com textura por nivel, custo de
upgrade variavel, todo mob (inclusive creeper) dando dano no Nexus, flecha de
esqueleto danificando o Nexus, bridging quando o Nexus esta elevado, torreta
tratada como alvo a distancia e a correcao do delay tiro-antes-de-mirar.
Pontos de maior risco:

- **`ArrowNexusDamageMixin`** mixina em `ProjectileEntity#onBlockHit`. Se o
  Loom nao aplicar esse mixin (erro no boot, nao no build), e porque esse
  metodo esta declarado em outra classe da hierarquia nessa versao especifica
  — o log do Fabric aponta exatamente onde.
- **`ModelTransform.of(...)`** (usado para angular os bracos da besta) e uma
  API que ja existia no projeto de forma indireta, mas nunca tinha sido
  chamada com argumentos de rotacao aqui; se a assinatura estiver errada e
  erro de compilacao, facil de achar.
- **Bridging** e deliberadamente uma heuristica (pular + colocar bloco embaixo
  dos pes), nao um pathfinder. Pode ficar estranho visualmente em terrenos
  complicados; o objetivo e so evitar o Nexus ficar impossivel de alcancar.

**Segunda rodada:** o Nexus como prioridade real de combate (jogador/torreta so
sao engajados quando genuinamente perto), arrombamento de portas para todo
invasor exceto creeper, e a aba propria no inventario. Pontos de maior risco:

- **`InvaderCombatPriority`** limpa `mob.getTarget()` a cada tick quando o alvo
  esta longe demais. Isso pode gerar um "pisca" de 1 tick em que uma IA de
  combate vanilla comeca a reagir antes do alvo ser limpo de novo — esperado,
  imperceptivel, nao e bug.
- **`FabricItemGroup`** (aba propria do inventario) e uma API de conveniencia
  do Fabric API historicamente estavel entre versoes, ao contrario das APIs de
  Mojang que causaram os erros da primeira compilacao — risco baixo, mas ainda
  nao testada aqui.
- **Deteccao de porta fechada** usa `DoorBlock`/`DoubleBlockHalf`, API estavel
  desde a introducao de portas em duas metades (Minecraft 1.13) — risco baixo.

---

## Licenca

Apache 2.0 — veja [LICENSE](LICENSE).
