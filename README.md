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

| Clique com... | Acontece |
|---|---|
| Mao vazia (botao direito) | Abre a **aba de estatisticas** |
| Soco (ataque, qualquer item na mao) | Recolhe a torreta e as flechas que sobraram, sem dar dano nela |
| Flechas | Carrega municao (aceita flecha com efeito e espectral) |
| Material do nivel atual, com a torreta ferida | **Repara vida** |
| Material do proximo nivel, com a torreta com vida cheia | Contribui para o upgrade (acumula, sobe de nivel sozinha ao completar) |
| Livro encantado | Aplica Poder, Impacto, Chama, Perfuracao, Multitiro ou Carga Rapida |

Nao ha mais texto de status no chat: o botao direito de mao vazia abre uma aba
mostrando nivel, vida, municao, dano/alcance/recarga, qual material repara a
torreta e quanto falta para o proximo nivel. O progresso de upgrade e por
unidade — cada material correto clicado conta um ponto, e a torreta sobe de
nivel sozinha assim que atinge a quantidade necessaria (nao precisa ter tudo
na mao de uma vez). A aba **nao e um inventario de verdade** (nenhum slot para
arrastar item): o material entra do mesmo jeito de sempre, clicando com o
botao direito na torreta com a aba fechada — reabrir a aba so mostra o
progresso mais atualizado. Veja "Status de verificacao" para o porque dessa
escolha.

Ela **so atira quando esta de fato apontada para o alvo** — gira primeiro, dispara
depois, nunca o contrario.

**Cone de visao.** A besta gira 360 graus na horizontal, mas so inclina ate 60
graus para cima e para baixo (`turretVerticalFovDegrees`). Isso deixa dois
**pontos cegos** naturais: um cone logo acima e outro logo abaixo dela. Um mob
dentro desses cones nunca e escolhido como alvo — e por isso ela nunca trava
mirando algo que jamais conseguiria apontar.

O alcance e medido **so no plano horizontal**, nao em linha reta 3D — uma
torreta no topo de uma torre nao perde alcance efetivo contra quem se aproxima
pelo chao. Ela **reavalia o proprio alvo a cada poucos ticks**: se o atual sair
de alcance, ficar sem visada ou sair do cone, ela troca por outro mob de
verdade alcancavel em vez de ficar grudada olhando pro mesmo lugar. E, como
rede de seguranca final, **se passar 2 segundos com um alvo sem conseguir
disparar nenhuma vez, ela abandona esse alvo** e o ignora por um tempo.

| Nivel | Dano | Alcance | Recarga | Vida | Municao | Custo do upgrade |
|---|---|---|---|---|---|---|
| Madeira | 2.0 | 14 | 2.0s | 20 | 64 | — |
| Ferro | 3.0 | 21 | 1.6s | 30 | 96 | 20x Barra de Ferro |
| Ouro | 4.0 | 28 | 1.2s | 40 | 128 | 7x Barra de Ouro |
| Diamante | 5.5 | 36 | 0.8s | 55 | 192 | 4x Diamante |
| Esmeralda | 7.0 | 46 | 0.5s | 75 | 256 | 2x Esmeralda |

O custo cai conforme o material fica mais raro: ferro (facil de juntar em
quantidade) pede bem mais unidades, esmeralda (o mais raro) pede so 2. O
material que **repara** a torreta e o mesmo que a trouxe ate o nivel atual —
ferro tanto para o nivel madeira quanto para o ferro, ja que madeira nao tem
material de upgrade proprio.

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
blaze e zombified piglin na 3, piglin brute na 5, hoglin na 6, **ghast na 6**
(antes era so na 8). Da invasao 3 em diante eles sao cerca de um quarto da
horda.

O **creeper foi bem reduzido** no sorteio de mobs (peso 6, contra 48 do zumbi
e 28 do esqueleto): com o peso antigo, a horda ficava facilmente dominada por
creepers, que so avancam ate o Nexus para se explodir em vez de marchar e
bater normalmente — deixava as invasoes menos dinamicas e escondia os outros
mobs.

O **zumbi e a base da horda em dois sentidos diferentes**: alem do maior peso
no sorteio (que muda a *proporcao*), cada zumbi sorteado nasce acompanhado de
mais 2 (`zombieExtraSpawnCount`), o que muda a *quantidade*. Na pratica ele
chega em grupo enquanto os outros tipos chegam um a um.

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
| Creeper | **Se explode no obstaculo** quando nao ha caminho, abrindo passagem para o resto da horda (no maximo 5s ate acender) | 100% |
| Zumbi | **Escadas**: monta uma coluna de escadas no obstaculo | 16% |
| Zumbi | **Construtor**: ergue caminho/pilar de blocos ate o Nexus, muito mais rapido que um invasor comum | 12% |
| Zumbi | **Picareta**: minera o bloco que atrapalha | 18% |
| Zumbi | **TNT**: **arremessa** uma unica TNT em arco, como um projetil | 7% |
| Zumbi | **Isqueiro**: ateia fogo em obstaculo de madeira em vez de quebra-lo | 10% |

Cada zumbi recebe **no maximo uma** dessas habilidades — os sorteios sao
intervalos exclusivos. E cada uma delas e sorteada por conta propria, sem
depender do cenario: o zumbi com isqueiro, por exemplo, nasce com a chance
acima em qualquer invasao; o que depende de haver madeira por perto e so o
*uso* da habilidade, nao o nascimento dele.

**Quando o Nexus esta suspenso no ar ou bem acima do chao**, as chances de
zumbi com escadas e de zumbi construtor sao multiplicadas por 2.5
(`elevatedNexusBuilderBonus`) — sao justamente as duas habilidades que
resolvem esse cenario, entao a horda passa a trazer bem mais delas.

O zumbi com escadas **nao precisa de uma parede pronta**: se nao houver bloco
solido para grudar a escada, ele constroi uma colunazinha de 2 blocos de
cobblestone do proprio lado e prende as escadas nela — o suficiente para a
horda escalar ate um Nexus suspenso mesmo sem nenhuma construcao por perto
(contra uma parede de verdade, a escada sobe ate 5 blocos).

O **zumbi bombardeiro** carrega uma unica TNT e a **arremessa em arco**, como
um projetil, contra o inimigo que estiver perseguindo (jogador ou torreta) ou
contra o proprio Nexus, a ate 16 blocos. Ela sai ja acesa, entao explode logo
depois de aterrissar. Como e so uma, depois disso ele vira um zumbi comum.

O **zumbi construtor** faz o que qualquer invasor preso eventualmente faz
(empilhar bloco para subir), so que muito melhor: comeca quase de imediato,
coloca bloco em ritmo tres vezes mais rapido e nao para tao cedo.

### Ovos de invasor

A aba do mod tem um **ovo para cada zumbi especial** — escadas, TNT,
construtor, isqueiro e picareta. O mob nasce com aquela habilidade garantida
(sem depender do sorteio), ja recrutado pela invasao, e marcha para o Nexus
normalmente. Ele **nao entra na contagem oficial da onda**, entao invocar
varios nao bagunca o HUD nem o fechamento da noite — servem para testar cada
comportamento isoladamente.

### Invasores nao dropam itens

A invasao spawna centenas de mobs por noite. Se cada um dropasse, o chao em
volta do Nexus viraria uma montanha de carne podre, ossos e equipamento —
mais lag do que recompensa. A recompensa da noite vem do **saque que o Nexus
solta ao amanhecer**, nao de farmar a horda. O XP continua caindo normalmente.
Desligavel em `invadersDropLoot`.

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

**Reavaliacao de rota.** Antes de tratar o que esta na frente como um muro a
ser quebrado, o invasor que empaca procura um **desvio**: varre um cubo de 4
blocos em volta atras de um ponto que fique mais perto do Nexus *e* que o
pathfinding vanilla consiga alcancar de verdade. Se achar, ele contorna. E o
que resolve o caso "existe uma passagem limpa tres blocos ao lado, mas o mob
fica batendo na parede porque ela esta exatamente na linha reta ate o bloco" —
antes ele so tinha duas saidas, quebrar o que estava na frente ou empilhar
bloco para subir, nunca simplesmente dar a volta.

**Se o Nexus estiver num lugar alto ou suspenso no ar**, o invasor que ficar
preso por tempo demais comeca a **construir um caminho de blocos** (cobblestone)
embaixo dos proprios pes, pulando ate ganhar altura ou atravessar um vao — uma
heuristica simples para nao deixar o Nexus inalcancavel so por estar no ar.
Desligue com `invadersCanBridge: false` se preferir que mobs nunca coloquem
bloco no mundo.

**Subir escada tambem ficou mais insistente.** Antes, o mob so ganhava
velocidade de escalada depois que a propria colisao do vanilla marcava
`isClimbing()` — o que raramente acontecia perto o bastante de uma torre com
escada, e a horda ficava parada na base sem nunca subir. Agora, assim que o mob
chega perto o bastante da coluna da escada, ele e empurrado para o centro dela
e ganha velocidade de escalada mesmo antes do vanilla marcar a colisao, e
continua sendo puxado de volta ao centro da coluna a cada tick para nao
"desgrudar" da escada no meio da subida.

### Depois de cada noite: saque ao redor do Nexus

Toda invasao repelida (o jogador aguentou ate o amanhecer) derruba um punhado
de itens aleatorios perto do Nexus: minerios, comida, blocos, flechas... A
quantidade de rolagens cresce (com teto) conforme as invasoes avancam.

### Jogador e torreta sao alvos — mas o Nexus e a prioridade

Um invasor **corpo a corpo** so passa a brigar com a torreta se **ela acertar
ele primeiro** — para esses nao existe deteccao a distancia (isso ja foi
tentado numa rodada anterior e causou o bug descrito abaixo). O gatilho e o
`RevengeGoal` do proprio vanilla, nativo de todo mob hostil: levar uma
flechada da torreta e exatamente o mesmo estimulo que levar uma flechada de um
jogador. O jogador continua sendo alvo do jeito vanilla de sempre (ele ataca,
o mob revida).

**Esqueletos sao a excecao, de proposito:** eles *procuram* as torretas e
priorizam derruba-las a ate 20 blocos (`rangedTurretPriorityRange`). O
problema que fez a deteccao a distancia ser removida — mob saindo do caminho
atras de uma torreta que nao consegue alcancar — simplesmente nao existe para
quem atira parado de onde esta. Na pratica, as torretas viram alvo de fogo
concentrado da linha de esqueletos, o que da a elas um contrapeso real.

Mesmo depois de ser alvejado, o foco na torreta **nunca e permanente**, por
duas regras que trabalham juntas:

- **Alcance de engajamento** (`nexusPriorityEngageRange`, 6 blocos por padrao):
  se o alvo atual (jogador ou torreta) esta mais longe que isso, o mob esquece
  ele a cada tick e volta a caminhar para o Nexus. E o que evita a situacao de
  3 torretas em fila separadas por 5 blocos cada obrigarem o invasor a mata-las
  todas antes de sequer tentar o bloco.
- **Desistencia por falta de progresso**: mesmo perto o bastante, se o mob
  passa 60 ticks engajado numa torreta sem reduzir a distancia ate ela (sinal
  de que o pathfinding nao da conta — por exemplo, uma torreta num pilar
  isolado), ele solta o alvo e volta para o Nexus. Reusa a mesma logica de
  "distancia nao diminuiu" que ja detecta o mob preso perto do bloco
  (`AttackNexusGoal`), em vez de depender de uma checagem de pathfinding
  separada e nao testada.

**Bug corrigido nesta rodada:** antes, uma goal dedicada varria um raio de 64
blocos a cada segundo e forcava o alvo do mob para a torreta mais proxima
visivel — o que fazia a horda inteira ficar permanentemente grudada nas
torretas, nunca soltando o alvo, nunca voltando a atacar o Nexus. Isso
explicava varios sintomas juntos: creeper que nunca explodia (preso perseguindo
a torreta em vez de chegar no Nexus), zumbi com TNT que nunca plantava a bomba,
e aranha em cima do Nexus sem causar dano (o alvo dela ainda era uma torreta
distante, nao o bloco embaixo dela). Removendo essa goal e deixando o combate
com torreta ser puramente reativo, os quatro sintomas somem juntos, porque a
causa era uma so.

**Segundo bug, mais sutil (corrigido numa rodada seguinte):** mesmo depois de
remover a deteccao a distancia, creeper e aranha ainda deixavam de causar dano
ao Nexus com um alvo por perto — porque a `AttackNexusGoal` (a IA que
efetivamente causa o dano) tinha prioridade numerica mais baixa que a goal de
combate **vanilla** do proprio mob, entao perdia o controle de movimento pra
ela sempre que havia um alvo por perto, mesmo com o mob literalmente em cima
do bloco. Agora ela sempre vence esse empate (prioridades negativas, ver
"Status de verificacao") e, alem disso, **nunca cede o alcance de golpe do
Nexus** para nenhum alvo, nem torreta nem jogador — chegou perto o bastante,
o Nexus toma o golpe, ponto final.

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
| `/dtb removenexus` | 2 | Remove o Nexus **sem apagar o mundo** — para testar no criativo |

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
| `zombieExtraSpawnCount` | 2 | Zumbis extras que nascem junto a cada zumbi sorteado |
| `invadersDropLoot` | `false` | Invasores dropam itens ao morrer |
| `mobMultiplier` | 1.0 | Multiplicador global do ritmo |
| `creeperBreachChance` | 1.0 | Chance de creeper arrombador |
| `spiderWebChance` | 0.25 | Chance de aranha com teia |
| `zombiePickaxeChance` | 0.18 | Chance de zumbi mineiro |
| `zombieLadderChance` | 0.16 | Chance de zumbi carpinteiro |
| `zombieTntChance` | 0.07 | Chance de zumbi bombardeiro (arremessa TNT) |
| `zombieBuilderChance` | 0.12 | Chance de zumbi construtor |
| `elevatedNexusBuilderBonus` | 2.5 | Multiplicador de escada/construtor com o Nexus suspenso |
| `zombieFireStarterChance` | 0.10 | Chance de zumbi com isqueiro (incendeia madeira) |
| `creeperBreachTimeoutTicks` | 100 | Prazo maximo (5s) ate o creeper acender diante de um obstaculo |
| `invadersCanBridge` | `true` | Mobs constroem caminho de blocos quando o Nexus esta elevado |
| `nexusPriorityEngageRange` | 6.0 | Raio (blocos) para brigar com jogador/torreta antes de voltar ao Nexus |
| `rangedTurretPriorityRange` | 20.0 | Raio no qual esqueletos priorizam atirar nas torretas |
| `maxTurretEngageTicks` | 200 | Teto de tempo focado numa torreta antes de voltar ao Nexus |
| `turretIgnoreTicksAfterGiveUp` | 120 | Carencia antes de poder mirar em outra torreta |
| `doorBreakTicksPerHardness` | 10 | Ritmo de arrombamento de portas (ferro demora mais que madeira) |
| `turretRepairHealthPerItem` | 8.0 | Vida recuperada por unidade de material usada no reparo |
| `turretVerticalFovDegrees` | 60.0 | Abertura vertical do cone de visao da torreta (define os pontos cegos) |

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

**Terceira rodada:** o playtest de verdade revelou o bug do foco eterno na
torreta (ver secao "Jogador e torreta sao alvos" acima) e trouxe a aba de
estatisticas, o reparo de torreta, o saque pos-invasao, o `/dtb removenexus`,
o zumbi com isqueiro e a correcao da escalada de escada. Igual as rodadas
anteriores, **nada disso passou por `buildAll` nem por teste em jogo ainda**.
Pontos de risco, do maior para o menor:

- **A aba de estatisticas da torreta e a maior area de risco do projeto.** E a
  primeira vez que o codigo usa `Screen`/`DrawContext` fora do HUD (que so
  desenha overlay, nunca abre uma tela) e a primeira rede **S2C dedicada**
  alem da sincronizacao do HUD. Por isso ela foi deliberadamente simplificada:
  **nao existe `ScreenHandler`/`Slot`/`ScreenHandlerRegistry`** — familia de
  API sem nenhum precedente testado neste projeto, e que exigiria registrar um
  tipo de menu por versao. Em vez de um slot de inventario sincronizado, o
  material continua entrando do jeito que ja funcionava (clique direito na
  torreta), e a aba so mostra o estado mais recente. Isso reduz o risco, mas
  `TurretStatsScreen`, `TurretStatsPayload` (1.21) e o registro
  `ClientPlayNetworking`/`ServerPlayNetworking` (1.20.1) continuam sem nenhum
  teste em jogo.
- **`TurretEntity#damage(DamageSource, float)`** e a primeira vez que o mod
  sobrescreve esse metodo (em vez de so chama-lo, como o `WebShotEntity` ja
  fazia). A assinatura `boolean damage(DamageSource, float)` e uma das mais
  estaveis da API de entidade — nao mudou nem no salto 1.20.1 → 1.21 — mas
  "sobrescrever" e "chamar" sao coisas diferentes, e isso nunca tinha sido
  testado num build de verdade aqui.
- **Interpretacao de um pedido ambiguo:** o playtest pediu tanto "clique
  esquerdo abre a aba" quanto "soco devolve a torreta" — como soco *e* o
  clique esquerdo no Minecraft, as duas frases se contradizem. Foi
  interpretado como: **botao direito de mao vazia** abre a aba (convencao
  normal de interagir/abrir GUI) e **soco (ataque)** devolve a torreta. Se nao
  for o que fazia sentido, e so pedir a troca.
- **Predicado de angulo degenerado** (`TurretEntity#isDegenerateAngle`) exclui
  da propria selecao de alvo qualquer mob quase exatamente embaixo/em cima da
  torreta, resolvendo a causa raiz do "torreta inerte" em vez de so tratar o
  sintoma. Risco baixo (e so matematica sobre coordenadas), mas o
  comportamento resultante (torreta ignorando mobs no proprio eixo vertical)
  ainda nao foi visto em jogo.
- **Saque pos-invasao** usa `ItemEntity` com uma lista fixa de itens vanilla —
  API de entidade basica, risco baixo.
- **Zumbi com isqueiro** so ateia `Blocks.FIRE` acima do bloco de madeira (deixa
  o fogo vanilla se espalhar) em vez de queimar o bloco diretamente — mais
  simples e mais barato que simular combustao a mao, mas depende do fogo
  vanilla realmente pegar no bloco de baixo, o que nao foi visto em jogo ainda.

**Quarta rodada:** o segundo playtest (agora com a torreta e a aba de verdade
em jogo) achou causas raiz mais profundas que a rodada anterior nao cobria.
Resumo dos diagnosticos:

- **Torreta grudava num alvo e ignorava tudo o mais.** A causa era a propria
  escolha do alvo: um `ActiveTargetGoal` no target selector segurava o alvo
  ate ele morrer ou sair do *follow range* (40 blocos, bem mais que o alcance
  real de tiro de qualquer nivel), sem nunca reavaliar se aquele alvo ainda
  dava pra atacar de verdade. Um mob fora de alcance de tiro, atras de uma
  parede, ou simplesmente pior posicionado que outro passando na frente
  continuava sendo "o alvo" indefinidamente. **Removido o `ActiveTargetGoal`
  por completo** — agora e a propria `TurretShootGoal` que escolhe e reavalia
  o alvo a cada 5 ticks, trocando por um candidato de verdade engajavel
  (dentro de alcance, visivel, fora do proprio eixo vertical) sempre que o
  atual deixa de servir, ou por um bem mais perto (margem de 2 blocos pra nao
  ficar oscilando entre dois alvos parecidos).
- **Torreta numa torre alta nao acertava mobs abaixo dela** (mesmo nao estando
  exatamente embaixo). O alcance era medido em distancia 3D
  (`squaredDistanceTo`), entao a altura da torre "comia" parte do orcamento de
  alcance — uma torreta de nivel madeira (12 blocos) 15 blocos no ar mal
  enxergava o chao. Trocado para **distancia so no plano horizontal**
  (`TurretEntity#horizontalSquaredDistanceTo`), do jeito que uma torre de
  defesa deveria funcionar.
- **Creeper nao explodia no Nexus, aranha ficava em cima dele sem dar dano, e
  de modo geral "as torretas ficaram focadas em mobs e nao davam dano ao
  Nexus".** Essa era a causa raiz por tras de tres queixas diferentes: a
  `AttackNexusGoal` (prioridade numerica 4) perdia o controle de
  movimento/mira para as goals de combate **vanilla** do proprio mob
  (`ZombieAttackGoal` e equivalentes, tipicamente prioridade 2), sempre que
  havia um alvo por perto — mesmo que o mob estivesse literalmente em cima do
  Nexus. Como o alvo (torreta) fica "engajado" ate 60 ticks por causa da
  correcao da rodada anterior, essa janela de perda de prioridade ficou bem
  mais comum. Duas mudancas: (1) `AttackNexusGoal#canStart()` agora retorna
  `true` incondicionalmente quando o mob ja esta dentro do alcance de golpe do
  Nexus, nao cede mais pra "tem alvo por perto" nesse caso; (2) toda a cadeia
  de goals customizadas (arrombar, escalar, pontilhar/teia, atacar o Nexus)
  passou a usar **prioridades negativas** (-3 a 0), garantindo que ela sempre
  vence qualquer goal vanilla de combate na disputa por Control.MOVE/LOOK, nao
  so nominalmente mas de fato. Essa e provavelmente a correcao mais importante
  desta rodada.
- **`/dtb removenexus` deixava mobs "atacando o vento"** no lugar onde o Nexus
  costumava estar, e o Nexus novo (colocado em outro lugar) tomava dano
  desses mobs antigos. Duas causas empilhadas: `despawnAllInvaders` so
  descartava invasores com `countsForWave() == true`, deixando de fora
  qualquer mob "atraido" (recrutado fora do lote oficial de spawn) — esses
  sobreviviam para sempre a qualquer reset. E mesmo os que fossem descartados
  a tempo, `AttackNexusGoal#attackNexus` aplicava dano no **estado global
  atual** do Nexus sem checar se a posicao que o mob tinha guardada ainda
  batia com ele — um mob parado no lugar do Nexus antigo conseguia ferir um
  Nexus novo em outra coordenada. Corrigido nos dois pontos: o despawn agora
  derruba todo invasor (contado ou nao), e `attackNexus` se recusa a bater
  (e se "aposenta" como invasor) se a posicao do Nexus que ele conhece nao for
  mais a atual.
- **A aba de estatisticas estava "apagada" e com texto vazando pra fora da
  moldura.** Dois problemas de verdade: nada escurecia o mundo atras da aba
  (baixo contraste contra um ceu claro), e o texto era desenhado direto numa
  caixa de altura/largura fixas, sem quebra de linha — qualquer traducao mais
  comprida que o espaco disponivel simplesmente vazava. Reescrita para: (1)
  escurecer a tela toda antes de desenhar o painel; (2) quebrar todo texto com
  `TextRenderer#wrapLines` antes de desenhar; (3) calcular a altura da caixa a
  partir do conteudo de verdade (numero de linhas depois da quebra), nunca um
  valor fixo. O botao "Fechar" deixou de ser um `ButtonWidget` registrado em
  `init()` porque a caixa muda de altura conforme o texto quebra — agora e
  desenhado e testado a mao em `render()`/`mouseClicked()`.

Como sempre, **nada disso passou por `buildAll` nem por teste em jogo ainda**
— e a rodada com mais mudancas de comportamento de goal (prioridades
negativas, torreta escolhendo o proprio alvo) desde o inicio do projeto, entao
vale prestar atencao especial nela no proximo playtest.

**Quinta rodada — a causa raiz de verdade da torreta.** O playtest seguinte
mostrou que a torreta *continuava* travada num alvo sem atirar, mesmo depois
de toda a reescrita de selecao de alvo da rodada anterior. O motivo era bem
mais embaixo, e nao tinha nada a ver com escolha de alvo:

> O `LookControl` do vanilla roda **depois** das goals, dentro de
> `MobEntity#tickNewAi`, e como `shouldStayHorizontal()` e `true` por padrao
> ele executa `setPitch(0)` a cada tick. Ou seja: a torreta calculava a
> inclinacao certa na goal e o vanilla zerava logo em seguida, todo tick. Ela
> nunca conseguia apontar para cima nem para baixo — e como o disparo so
> acontece quando a mira converge dentro da tolerancia, qualquer alvo que nao
> estivesse exatamente na altura dos olhos dela **jamais** era acertado, e o
> foco nunca era liberado.

Isso explica de uma vez todos os sintomas que sobraram: "fica focada num mob
que nao consegue disparar", "nao atira em quem esta abaixo", "torreta no alto
nao acerta nada la embaixo" e ate o "funcionou no comeco e depois parou" (o
primeiro alvo calhou de estar na altura certa). A correcao guarda a mira em
campos proprios (`aimYaw`/`aimPitch`) e os **reaplica depois do
`super.tick()`**, ou seja, depois do `LookControl` ter feito o estrago —
sem depender de sobrescrever `LookControl`, cuja API varia entre versoes.

Outras mudancas desta rodada:

- **Cone de visao de verdade.** O remendo anterior so excluia mobs
  exatamente no eixo vertical. Agora a torreta tem uma abertura vertical real
  (`turretVerticalFovDegrees`, 60 graus por padrao): gira 360 graus na
  horizontal, mas so inclina ate esse limite, o que cria dois **pontos cegos**
  naturais (um cone acima e outro abaixo). Alvo fora do cone nunca e
  escolhido.
- **Rede de seguranca absoluta contra travamento.** Independente de qualquer
  checagem de cone/alcance/visada, se a torreta passa 40 ticks com o mesmo
  alvo **sem conseguir disparar nenhuma vez**, ela abandona esse alvo e o
  ignora por 5 segundos. Se algum caso nao previsto aparecer no futuro, ele
  vira no maximo 2 segundos de pausa em vez de um travamento permanente.
- **A aba estava borrada por causa do proprio vanilla.** Nao era falta de
  contraste: `Screen#render` comeca chamando `renderBackground`, que no
  1.20.5+ aplica **blur** no framebuffer inteiro. Como a chamada a
  `super.render(...)` estava no fim do metodo, o desfoque caia por cima do
  painel e do texto ja desenhados. Como a tela nao registra nenhum widget (o
  botao tambem e desenhado a mao), a correcao foi simplesmente **nao chamar o
  super** e fazer o escurecimento com um `fill` proprio.
- **Creeper com prazo de 5 segundos.** Antes ele so acendia depois de
  conseguir encostar no obstaculo (3.2 blocos), o que podia demorar muito ou
  nunca acontecer. Agora ele e avaliado antes da fase de aproximacao: acende
  na hora se ja estiver perto, e tem no maximo `creeperBreachTimeoutTicks`
  (100 ticks = 5s) preso tentando chegar antes de acender de qualquer jeito.
  O padrao de `creeperBreachChance` tambem subiu para 1.0 — com o peso de
  spawn do creeper agora baixo, um creeper que nao sabe abrir passagem nao
  contribui em nada.
- **Esqueleto prioriza torreta.** Voltou uma versao da `TargetTurretGoal`,
  agora **restrita a mobs de ataque a distancia** (`RangedAttackMob`). O
  motivo de ela ter sido removida antes — mobs corpo a corpo saindo atras de
  torretas que nao conseguiam alcancar — nao se aplica a quem atira parado. O
  alcance dessa prioridade e proprio (`rangedTurretPriorityRange`, 20 blocos),
  e o foco continua tendo teto: `maxTurretEngageTicks` (200 ticks) para
  qualquer invasor, seguido de uma carencia antes de poder mirar em outra
  torreta.

**Sexta rodada — conteudo novo e ajustes de balanceamento.** Esta rodada
mexeu menos em bugs e mais em conteudo:

- **Zumbi como base da horda, de dois jeitos.** O peso no sorteio subiu (36 →
  48), o que muda a *proporcao*; e cada zumbi sorteado agora nasce com mais 2
  junto (`zombieExtraSpawnCount`), o que muda a *quantidade*. Sao coisas
  diferentes de proposito — so mexer no peso deixaria a horda com a mesma
  densidade, so que mais monotona.
- **Mais zumbis especiais.** Picareta 12% → 18%, escadas 10% → 16%, isqueiro
  6% → 10%, TNT 5% → 7%, mais o novo construtor com 12%. O zumbi com isqueiro
  ja era sorteado independente de haver madeira por perto — o cenario so
  decide se a habilidade *serve* para alguma coisa, nao se ele nasce.
- **Zumbi construtor** (`BLOCK_BUILDER`): faz o que qualquer invasor preso
  eventualmente faz, so que muito melhor — comeca com 1 ciclo de travamento em
  vez de 3, coloca bloco a cada 2 ticks em vez de 6, e vai ate 128 blocos em
  vez de 48. Reusa a `BridgeToNexusGoal` ja existente em vez de duplicar a
  logica.
- **Nexus suspenso puxa quem resolve o problema.** As chances de zumbi com
  escada e de construtor sao multiplicadas por `elevatedNexusBuilderBonus`
  (2.5) quando o Nexus esta flutuando ou bem acima do chao onde a horda nasce.
- **TNT arremessada.** O zumbi bombardeiro passou a ter **uma unica** TNT e a
  **arremessa em arco** ate 16 blocos, contra o alvo perseguido ou contra o
  proprio Nexus, com o pavio ja correndo. E uma `TntEntity` normal com
  velocidade inicial — nao precisou de entidade nova.
- **Invasores nao dropam mais itens** (`invadersDropLoot`, padrao false), via
  dois cortes: `InvaderDropsMixin` cancela `dropLoot` (a loot table) e o
  `MobEntityMixin` cancela `dropEquipment` (armadura/arma sorteada). O XP
  continua caindo. Nota de versao: `dropLoot(DamageSource, boolean)` tem a
  mesma assinatura no 1.20.1 e no 1.21.1 — o parametro `ServerWorld` so entrou
  no 1.21.2 — entao o mixin pode viver no codigo compartilhado.
- **Alcance da torreta virou o atributo que define o papel dela.** A
  progressao era timida (12→32); agora e 14→46. Como o alcance passou a ser
  medido no plano horizontal, esses numeros valem por igual no chao ou no alto
  de uma torre.
- **Ovos de invasor**: cinco itens novos (`InvaderEggItem`), um por
  habilidade, que invocam o zumbi com aquela habilidade garantida e ja
  recrutado pela invasao, mas fora da contagem da onda.
- **Reavaliacao de rota** (`AttackNexusGoal#tryDetour`): antes de marcar o
  caminho como bloqueado, o mob varre um cubo de raio 4 procurando um ponto
  mais perto do Nexus que o pathfinding vanilla alcance de verdade
  (`findPathTo` + `Path#reachesTarget`), e vai para la. O custo da busca de
  caminho so e pago para o melhor candidato, uma vez a cada ciclo de
  travamento — fazer isso para cada posicao do cubo, em dezenas de mobs, seria
  caro demais.

**Setima rodada — a horda que congelava.** O relato era que, conforme a noite
avancava, os mobs iam ficando **parados** ate a horda inteira travar. Isso nao
era lag nem chunk descarregado: era um bug na `BreachObstacleGoal`, e o
codigo das rodadas anteriores tinha agravado ele duas vezes.

> `canStart()` perguntava apenas "este mob tem alguma habilidade de
> arrombamento?". Como `DOOR_BREACHER` e dado a **todo** invasor menos o
> creeper, a resposta era sempre sim. O goal entao assumia o controle de
> movimento para qualquer obstaculo marcado, chegava perto, chamava
> `navigation.stop()` — e caia atraves de todos os branches sem executar
> nenhum, porque a parede nao era porta e o mob nao tinha picareta, TNT nem
> escada. Ficava ali parado, com `shouldContinue()` retornando true para
> sempre.

O efeito era **cumulativo e irreversivel por mob**: cada invasor que
esbarrasse uma vez numa parede que nao sabe tratar congelava de vez. Como ao
longo da noite mais e mais mobs esbarram em alguma coisa, a horda ia
"endurecendo" ate parecer que tudo tinha parado — exatamente o sintoma
relatado.

Duas mudancas minhas pioraram isso: as **prioridades negativas** da quarta
rodada colocaram essa goal na prioridade mais alta do mod (entao o mob
congelado nem sequer devolvia o movimento para a `AttackNexusGoal`), e
reduzir o zumbi da TNT para **uma unica banana** na sexta rodada criou mais um
caminho para o estado travado (depois de usar a TNT, `placeTnt` passa a
retornar false e o mob cai no mesmo buraco).

A correcao ataca a causa e ainda poe duas redes de seguranca:

- **`canHandle(pos)`** substitui o antigo `hasBreachAbility()`: agora a
  pergunta e "este mob consegue fazer alguma coisa com **este** obstaculo?" —
  porta so conta com `DOOR_BREACHER`, TNT/escada so contam enquanto o item
  existe no slot, isqueiro so conta em madeira, picareta so conta ate o limite
  de dureza. Se a resposta e nao, o obstaculo e largado na hora e a
  `AttackNexusGoal` (com a reavaliacao de rota da sexta rodada) assume para
  tentar contornar.
- **Teto de tempo** (`MAX_GOAL_TICKS`, 400 ticks): nenhum arrombamento
  legitimo demora tanto, e sem isso qualquer caso nao previsto voltaria a
  virar um mob congelado para sempre.
- **`stop()` libera a marcacao de obstaculo**, para o mob nao reentrar no
  mesmo lugar no tick seguinte e travar em loop.

Vale registrar que a explicacao que eu tinha dado antes para esse mesmo
relato — municao das torretas acabando — estava errada, e era errada em dois
sentidos: eu tinha entendido o sintoma ao contrario (mobs ficando *menos*
inertes) e a hipotese nao explicava um travamento permanente.

---

## Licenca

Apache 2.0 — veja [LICENSE](LICENSE).
