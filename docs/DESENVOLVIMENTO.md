# Diario de desenvolvimento

> Este arquivo e o **registro honesto** de como o mod chegou onde esta: o que
> foi verificado e como, o que ainda **nao** foi testado, os bugs que
> apareceram em cada playtest e a razao por tras de cada decisao de
> implementacao que nao e obvia olhando so para o codigo.
>
> Ele existe separado do [README](../README.md) de proposito. O README responde
> "o que o mod faz e como jogar"; aqui fica o "por que esta assim" e,
> principalmente, **o que ainda pode quebrar**. Se voce vai mexer no codigo,
> comece por aqui — varias decisoes que parecem tortas sao cicatrizes de
> problemas reais, e desfaze-las sem ler o motivo tende a reintroduzir o bug.
>
> Regra que se repete em quase todas as secoes abaixo: **assinatura de API
> assumida ja derrubou este mod mais de uma vez.** Sempre que houver escolha
> entre uma API elegante e nao verificada e uma feia porem ja comprovada nas
> duas versoes, este projeto escolhe a segunda — e anota o porque.

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
- **Invasores nao dropam mais itens** (`invadersDropLoot`, padrao false). O XP
  continua caindo. Veja a oitava rodada abaixo para como isso foi feito — a
  primeira tentativa estava errada e derrubava o jogo no boot.
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

**Oitava rodada — o crash de mixin, e a licao sobre assinaturas.** A supressao
de drops da sexta rodada **derrubava o jogo no boot** no 1.21.1. O Mixin foi
direto ao ponto:

```
Invalid descriptor on MobEntityMixin->@Inject::defendtheblock$skipInvaderEquipment
Expected (ServerWorld, DamageSource, boolean, CallbackInfo)
but found (DamageSource, int, boolean, CallbackInfo)
```

Eu tinha **assumido** que `dropEquipment(DamageSource, int, boolean)` valia nas
duas versoes. No 1.21.1 o `int lootingMultiplier` deu lugar a um `ServerWorld`
na frente (parte da reforma de encantamentos/loot do 1.21) — ou seja, a
assinatura difere entre as versoes, e aquele injection nunca poderia ter vivido
em `common/`. Foi o mesmo tipo de erro que ja tinha acontecido com
`ITEM_ARMOR_EQUIP_IRON`: afirmar uma assinatura de API que eu nao tinha como
verificar aqui.

A correcao reduz a superficie de risco em vez de so trocar a assinatura:

- **Equipamento (armadura/arma) nao usa mais mixin nenhum.**
  `mob.setEquipmentDropChance(slot, 0f)` e API publica e estavel nas duas
  versoes, e resolve a parte que mais suja o chao (e a mais valiosa: armadura
  de diamante). Isso e `InvaderEquipment#dropChance`.
- **A loot table (carne podre, osso, flecha...) continua precisando de mixin**,
  mas agora ele vive **por versao**, cada um com a assinatura real da sua:
  `dropLoot(DamageSource, boolean)` no 1.20.1 e
  `dropLoot(ServerWorld, DamageSource, boolean)` no 1.21.1.
- **E esse mixin foi isolado num config proprio**
  (`defendtheblock-drops.mixins.json`) marcado como **`"required": false`**.
  Se a assinatura do `dropLoot` ainda estiver errada em alguma versao, o
  Mixin loga um aviso e segue — o mod carrega normalmente e so os itens da
  loot table voltam a cair, em vez de o jogo nao abrir. Nenhum outro mixin do
  mod (que sao os que realmente sustentam a invasao) fica exposto a isso.

**Nona rodada — zona livre de spawn.** As duas regioes em volta do Nexus
(quadrado seguro + anel circular de invasao, descritas em "O quintal seguro e
o anel de invasao") passaram a viver numa classe unica,
`NexusZones`, usada tanto pelo spawn da invasao quanto pela supressao do
spawn natural. Ter as duas regras no mesmo lugar evita o erro classico de o
spawner da invasao e a checagem da zona segura discordarem por meio chunk e um
mob nascer dentro do quintal que deveria estar limpo.

As chaves `spawnChunkRadiusMin`/`spawnChunkRadiusMax` foram substituidas por
`noSpawnChunkRadius` (raio do quadrado seguro) e `spawnRingChunks` (largura do
anel), que descrevem a intencao em vez de dois raios soltos. `forcedChunkRadius`
e `attractionChunkRadius` subiram para 4, para continuarem cobrindo exatamente
a area onde a invasao acontece.

**Decima rodada — faxina, campo de visao e o novo papel do creeper.**

- **Blocos de invasor sao desfeitos ao amanhecer.** Todo bloco que a horda
  coloca (cobblestone de pilar/ponte, escadas, teia) passa por `InvaderBlocks`,
  que registra a posicao no estado persistente da invasao. No fim da onda tudo
  e removido **sem dropar item**. Antes de remover, confere que o bloco ainda e
  um dos que a invasao sabe colocar — se o jogador minerou aquilo e construiu
  outra coisa no lugar, a faxina nao encosta. O unico caso que escapa e o
  jogador colocar exatamente o mesmo tipo de bloco na exata posicao
  registrada; guardar o estado completo de cada bloco no save nao valeria o
  custo para cobrir isso.
- **Campo de visao de verdade** (`InvaderVision`). O vanilla so oferece
  `canSee`, que e linha de visada — responde "ha caminho livre?", nao "ele esta
  olhando para la?". Sozinho, deixava o mob mirar em algo atras da propria
  nuca. Agora jogador e torreta so viram alvo dentro de um cone de 120 graus
  em volta do rumo da cabeca, *e* com visada livre.
- **Torreta virou alvo de todo mundo**, nao so de quem atira de longe. O medo
  de reabrir o bug de "horda grudada nas torretas" e endereçado pelo cone (bem
  mais restritivo que a linha de visada da primeira versao) somado aos tetos
  de tempo que ja existiam no `InvaderCombatPriority`.
- **Creeper nao danifica mais o Nexus.** O papel dele e exclusivamente abrir
  passagem explodindo obstaculos.
- **Todo invasor cava.** A picareta deixou de ser requisito e virou vantagem de
  velocidade (`unarmedMineTicksMultiplier`, 3x). Isso muda bastante a
  dificuldade: uma parede sem ninguem com picareta por perto deixou de ser um
  muro intransponivel, e agora varios mobs cavam em paralelo. O creeper
  continua sem cavar — o jeito dele e explodir.

Um efeito colateral que precisou de ajuste: o teto de tempo do
`BreachObstacleGoal` era um numero fixo (400 ticks), o que ficaria menor que o
tempo legitimo de um mob sem picareta cavando pedra. Agora ele e calculado por
obstaculo (tempo de trabalho + folga), senao a protecao contra travamento
passaria a interromper trabalho valido.

**Decima segunda rodada — o furo do Nexus emparedado, e a rampa.**

- **Nexus coberto tomava dano atraves da parede.** Bug real: `attackNexus` so
  checava distancia, nunca se havia bloco no caminho. Agora
  `NexusPathing#findCover` amostra a reta do olho do mob ate o bloco e, se
  achar solido no meio, esse bloco vira o obstaculo em vez de o Nexus tomar
  dano. Amostrar a cada 0.25 bloco em vez de usar `World#raycast` foi
  proposital: usa so `isSolidBlock`, que este projeto ja usa em varios
  lugares, e evita depender da assinatura de `RaycastContext` — o tipo de API
  cuja diferenca entre versoes ja derrubou este mod duas vezes.

  Detalhe de comportamento: quando esta coberto, o mob **nao** para a
  navegacao. Ele continua tentando se mover, o que o faz procurar um lado
  exposto e acionar a reavaliacao de rota. Parar ali deixaria a horda
  encostada na parede sem nunca tentar contornar.
- **Creeper com pavio de 3s** ao encostar no obstaculo, em vez de explodir no
  instante em que chega. Como o pavio do vanilla dura 30 ticks depois de
  `ignite()`, a contagem propria acende faltando esse tanto — assim o tempo
  total bate com a config e a segunda metade e o inchaco branco que da aviso
  ao jogador. O sistema tambem nao desperdica creeper: se outro invasor abrir
  a passagem antes, `shouldContinue()` derruba a goal e ele nunca chega a
  acender. Acender longe do obstaculo virou caso de emergencia (ficou preso
  tentando chegar).
- **Rampa de dificuldade refeita.** A noite 1 saturava um teto de 35 invasores
  vivos nos primeiros segundos. Agora comeca em 10 e leva ~25 invasoes para
  chegar ao mesmo teto. Os zumbis extras tambem passaram a ser liberados aos
  poucos (nenhum ate a invasao 4), e a chance de armadura caiu de 18% para 4%
  por peca na estreia.
- **Esqueleto para de avancar quando tem alvo.** A causa era uma incoerencia
  entre dois numeros: `InvaderCombatPriority` mantinha o alvo ate 20 blocos
  para quem atira, mas `AttackNexusGoal` usava 6 para todo mundo ao decidir se
  cede o controle de movimento. Com uma torreta a 15 blocos ele mantinha o
  alvo e marchava ao mesmo tempo. Agora os dois chamam
  `InvaderCombatPriority#engageRange`, que e a unica fonte da resposta.

**Decima primeira rodada — restricoes, carencia e coordenacao da horda.**
Parte desta rodada desfaz coisas da anterior, por decisao de design depois de
ver o resultado:

- **Quebrar bloco voltou a ser privilegio de tres habilidades** (picareta, TNT
  e creeper). Deixar toda a horda cavar tornava qualquer muro irrelevante — o
  efeito na dificuldade foi grande demais. A penalidade
  `unarmedMineTicksMultiplier` saiu junto, virou config morta.
- **Corpo a corpo nao caca mais torreta.** A `TargetTurretGoal` voltou a ser
  so de quem ataca a distancia. Para o corpo a corpo a torreta so vira alvo se
  as tres condicoes valerem juntas: ela o acertou primeiro, esta dentro do raio
  curto de engajamento, e o **pathfinding realmente chega la** — esta ultima e
  nova, checada uma vez no tick em que o alvo e adotado. Sem ela o mob ficava
  batendo a cabeca numa torreta em cima de um pilar: perto em linha reta,
  inalcancavel de fato.
- **Torretas nao tem fogo amigo** e **bloqueiam o campo de visao uma da
  outra**. A imunidade e simples (a flecha tem a torreta como atacante). O
  bloqueio precisou ser feito a mao com um raycast contra as caixas das outras
  torretas, porque o `canSee` do vanilla so testa blocos — para ele, uma
  entidade no meio do caminho nao existe. A desobstrucao e automatica e nao
  precisa de nenhum evento: a torreta destruida some da lista de entidades e o
  tiro volta a passar no rescan seguinte.
- **Carencia de 3 dias** apos colocar o Nexus, com aviso na tela por dia e um
  aviso especial no dia da estreia. O dia ja anunciado e persistido, entao
  reconectar nao repete a mensagem. Mundos antigos (sem data de colocacao
  salva) simplesmente nao entram em carencia.
- **Velocidade variavel dos zumbis**, com a chance de rapido subindo por
  invasao. Aplicada via `setBaseValue` no atributo em vez de
  `EntityAttributeModifier`, de proposito: o construtor do modifier mudou entre
  1.20.1 (UUID) e 1.21 (Identifier), enquanto `setBaseValue` e igual nas duas —
  exatamente o tipo de diferenca que ja derrubou este mod antes.
- **Horda abre espaco para quem trabalha** (`YieldToWorkerGoal`). Detalhes que
  precisaram de cuidado: a goal roda em prioridade -1, acima da
  `AttackNexusGoal`, senao o impulso de seguir para o bloco venceria o recuo;
  mas ela se recusa a rodar se o mob ja estiver em alcance de golpe do Nexus,
  entao nunca atrapalha o objetivo final. A flag de "trabalhando" e limpa
  incondicionalmente no `stop()` das duas goals que a levantam — se vazasse, a
  horda inteira ficaria abrindo espaco para um mob que nao esta fazendo nada. E
  ha um teto de 200 ticks recuando, pela mesma razao de sempre.

Nota sobre conflito de controles: no nivel de prioridade -1 convivem tres
goals, e elas nao brigam porque pegam controles disjuntos —
`BridgeToNexusGoal` usa JUMP, `SpiderWebShotGoal` usa LOOK e
`YieldToWorkerGoal` usa MOVE.

Nota de implementacao: a supressao do spawn natural e feita **descartando a
entidade no `ServerEntityEvents.ENTITY_LOAD`**, nao interceptando o
`SpawnHelper` do vanilla. E um evento do Fabric ja usado com sucesso neste
projeto (e o mesmo que recruta os mobs atraidos), enquanto mexer no
`SpawnHelper` exigiria um mixin com assinatura que eu nao tenho como verificar
aqui — e assinatura assumida ja derrubou este mod duas vezes. O custo e que o
mob chega a ser criado antes de sumir, em vez de a tentativa de spawn ser
barrada antes; na pratica isso e invisivel em jogo.

**Decima terceira rodada — auditoria de codigo morto e dos caminhos quentes.**
Esta rodada nao adiciona nenhuma feature: e uma varredura pedida sobre o codigo
existente, atras de otimizacao, simplificacao e coisa orfa. Saiu em dois
commits de proposito, para que o de risco zero pudesse ser separado do que
mexe em IA.

O que a varredura *nao* achou, e vale registrar: das 53 chaves de
`DtbConfig`, **nenhuma** esta orfa — todas sao lidas em algum lugar. Tambem
nao havia nenhum import sem uso nem nenhum campo privado escrito e nunca
lido.

**Commit 1 — remocao de codigo morto** (sem mudanca de comportamento):
`InvaderAbility.VALUES`/`toMask()`/`all()`, `InvaderData.getAbilityMask()`
/`setAbilityMask()`, `InvasionData.removeInvader()`, `TurretEntity.getAmmo()`,
`WaveComposition.unlocked()`. `NexusZones.noSpawnRadius()` virou `private`, que
e todo o uso que ela tem.

O caso que exigiu decisao foi `countsForWave`: era escrito, **persistido no
NBT** e nao tinha nenhum leitor. O ultimo sumiu quando `despawnAllInvaders`
passou a limpar todo invasor em vez de apenas os contados. Havia duas saidas —
voltar a usa-la ou remover o conceito. Removida, porque quem realmente
expressa "conta para a onda" e o conjunto `activeInvaders`: ele e alimentado
so pelo lote de `spawnInvader` (nunca por mob atraido ou por ovo) e e o que o
HUD le via `getMobsAlive()`. Ter as duas coisas era ter duas fontes para a
mesma resposta, e uma delas sem consumidor. Consequencia de save: a chave
`"Counts"` deixa de ser escrita, e saves antigos que a tenham simplesmente a
ignoram ao carregar.

**Commit 2 — caminhos quentes e duplicacoes.** O achado mais serio foi na
`TurretShootGoal`: `blockedByTurret` fazia a sua propria busca de entidades e
e chamado de dentro de `isEngageable`, que por sua vez roda para **cada
candidato** dentro de `findBestTarget`. Ou seja, uma varredura de N mobs
custava N buscas de entidade, por rescan, por torreta — o pior laco do mod, e
justamente numa base cheia de torretas com uma horda em cima. Agora a lista de
torretas vizinhas e buscada uma vez por rescan e reusada; torreta nao anda,
entao ficar ate 5 ticks desatualizada e inofensivo, e o javadoc antigo ja
prometia a desobstrucao so "no proximo rescan". `findBestTarget` tambem passou
a medir a distancia **antes** de chamar `isEngageable`, porque quem ja esta
mais longe que o melhor ate agora nao pode vencer e nao precisa pagar
cone/visada/raycast.

Alocacao em laco de tick, os outros dois pontos: `NexusPathing#findCover` roda
todo tick para cada invasor em alcance do Nexus e criava um `Vec3d` mais um
`BlockPos` a cada 0.25 bloco de linha; virou aritmetica escalar com um
`BlockPos.Mutable`. `AttackNexusGoal#tryDetour` varre 243 posicoes e alocava
dois objetos por posicao so para medir distancia; virou escalar comparado ao
quadrado, com objeto so para o melhor candidato.

Duplicacoes que agora tem fonte unica: o alcance de golpe no Nexus (2.8) era
uma constante em `AttackNexusGoal` e outra igual em `YieldToWorkerGoal` —
duas copias abriam a possibilidade de uma faixa em que o mob recuaria
exatamente onde deveria atacar, que e a classe de bug que a nona e a decima
segunda rodada ja pagaram para aprender. O `Math.floorDiv(timeOfDay, 24000)`
estava em tres lugares e virou `NexusManager#currentDay`. E
`NexusPathing#center` passou a delegar para `Vec3d.ofCenter`, que faz
exatamente a mesma conta.

Simplificacoes menores: `announceCountdown` tinha tres condicoes onde uma
basta (passado o dia da estreia, a carencia acabou e a contagem ja e zero por
definicao); `YieldToWorkerGoal.shouldContinue` chamava `clearance()` duas
vezes na mesma expressao; e `InvasionData.getPlacedBlocks()` devolvia o
conjunto vivo, o que deixava qualquer chamador furar em silencio o teto de
20000 posicoes e o `markDirty()` de `addPlacedBlock`/`clearPlacedBlocks` —
agora e uma view imutavel.

Verificacao desta rodada: `javac` estrutural nas duas versoes, sem erro, sem
simbolo removido ainda referenciado e sem import orfao. **Igual as rodadas
anteriores, nao passou por `buildAll` nem por teste em jogo.** Como o commit 2
mexe em IA, o que conferir em jogo e: torreta com outra torreta na frente
continua sem atirar atraves dela e volta a atirar quando a da frente e
destruida; mob empacado continua contornando parede; Nexus emparedado continua
sem tomar dano; e a horda continua abrindo espaco para quem trabalha sem
deixar de bater no bloco quando ja esta em alcance.

**Decima quarta rodada — a rampa de status.** O pedido foi trocar "contagem
fixa de mobs por onda" por "teto de mobs vivos que sobe ate 100, junto com os
modificadores de status".

Metade disso **ja existia** e vale registrar para nao se perder: a invasao
nunca teve contagem fixa. Desde a rodada em que o spawn virou continuo, ela
spawna do anoitecer ate amanhecer limitada so pelo teto de vivos
(`activeInvaders.size() >= cap`), a onda so fecha no amanhecer, o spawn ja
acontece dentro do anel delimitado, e `maxConcurrentInvaders` ja era **100**,
alcancado na invasao 24. A tabela da secao "A horda" estava com numeros de
lote errados (dizia 24 por lote na invasao 25; o real e 9) — corrigida agora
com os valores calculados a partir das formulas, nao de memoria.

O que **de fato faltava** era o outro eixo: nao havia **nenhuma** escalada de
status. Nada tocava `GENERIC_MAX_HEALTH` nem `GENERIC_ATTACK_DAMAGE` de
invasor; o unico atributo mexido era a velocidade do zumbi. Na pratica um
zumbi da noite 30 era identico ao da noite 1, so que em maior numero — o que e
exatamente a critica do pedido. Agora `InvaderEquipment#scaleStats` escala
vida (ate 3x, saturando na invasao 26) e dano corpo a corpo (ate 2x, saturando
na 21), de modo que as tres rampas — quantidade, vida, dano — terminam quase
juntas.

A vida sobe mais rapido que o dano de proposito: vida a mais alonga a luta e o
jogador ainda tem tempo de reagir, dano a mais simplesmente mata. Dobrar o
dano de 100 mobs e muito mais violento do que triplicar a vida deles.

Limitacao honesta da abordagem: mexer no valor base do atributo (em vez de
`EntityAttributeModifier`, pela mesma incompatibilidade 1.20.1/1.21 de sempre)
significa que a escalada de dano **nao atinge o esqueleto** — o dano da flecha
vem do projetil, nao do atributo — nem creeper e ghast, que nao tem atributo
de ataque. A vida vale para todos. Se o esqueleto precisar escalar tambem, o
caminho e mexer no dano da flecha, nao aqui.

**Trabalho de performance junto, e nao por perfeccionismo:** 100 mobs
simultaneos so e um numero utilizavel se o custo por tick nao explodir. Duas
goals comecavam os contadores em zero, e um lote inteiro de invasores nasce e
inicia a goal no mesmo tick — ou seja, a horda toda recalculava rota **no mesmo
tick**, para sempre. Busca de caminho e de longe a coisa mais cara que um mob
faz; no teto isso seria um pico de 100 buscas num tick e zero nos 19 seguintes.
`AttackNexusGoal#start` agora sorteia o deslocamento inicial de `repathTimer` e
`stuckTimer`, e `YieldToWorkerGoal` faz o mesmo com `scanCooldown` (cada
`canStart` dele e uma busca de entidades). Mesma carga total, diluida em vez de
concentrada. Isso soma a otimizacao de `findCover` e da `TurretShootGoal` da
rodada anterior, que ja tinham sido feitas pensando em horda grande.

Verificacao: `javac` estrutural nas duas versoes, sem erro. A rampa da tabela
foi **calculada a partir das formulas do codigo**, nao estimada. `GENERIC_MAX_HEALTH`
ja e usado neste projeto (na torreta) e compila nas duas versoes;
`GENERIC_ATTACK_DAMAGE` e a primeira aparicao, mas e da mesma familia de
constantes e so mudou de nome no 1.21.5, fora do alcance deste mod. **Nao
passou por `buildAll` nem por teste em jogo.** O que conferir: se a noite alta
realmente enche e mantem o teto, se o servidor aguenta 100 vivos, e se 3x de
vida com 90% de armadura de diamante nao deixou a torreta incapaz de matar
qualquer coisa — esse ultimo e o risco de balanceamento mais provavel, e
`invaderHealthMultiplierMax` e o botao para ajustar.

**Decima quinta rodada — modulos da torreta.** O pedido foi separar
*personalizacao* de *upgrade*: alem de subir de nivel, poder escolher atributos
isolados via livro, com graus e receita propria por tipo, ate 2 por torreta.

O desenho central e o **teto de 2 tipos**. Sem ele o sistema viraria mais uma
barra de progresso — uma torreta de fim de jogo simplesmente teria todos os
nove modulos. Com ele, montar torreta vira decisao, e duas torretas de
esmeralda lado a lado podem ter papeis diferentes.

Duas escolhas de modelagem que mereciam alternativa e nao a tem por bons
motivos:

- **O grau nao e um item.** Aplicar o mesmo livro de novo sobe o grau. A
  alternativa (um item por combinacao tipo+grau) daria 25 itens, 25 receitas e
  25 texturas para expressar a mesma coisa. Assim sao 9 de cada, e a progressao
  de grau reaproveita o gesto que o jogador ja usa para dar material de upgrade
  a torreta.
- **O livro nao e um `EnchantedBookItem` de verdade.** Encantamento proprio
  exigiria registrar `Enchantment`s, e o registro mudou completamente entre
  1.20.1 (registro simples) e 1.21 (registry dinamico via datapack) — dois
  caminhos inteiramente diferentes para um sistema que a torreta ja consegue
  ler por conta propria. Aqui o livro e um item comum e quem entende o efeito e
  a torreta.

Detalhe que ja estava certo e so foi generalizado: o disparo **sempre** custou
uma flecha so, independente de quantas saem — o Multitiro vanilla ja se
comportava assim porque `consumeAmmo()` e chamado uma vez por disparo, nao uma
por flecha. O modulo de Salva entra nessa mesma regra, entao "atirar mais de
uma flecha sem gastar mais de uma" nao precisou de nenhuma excecao.

Foi preciso um caminho de saida: sem ele, instalar o modulo errado ocuparia um
dos dois slots para sempre e a unica solucao seria destruir a torreta. Clicar
com **rebolo** desmonta tudo (os livros nao voltam, igual ao vanilla).

Riscos e mitigacoes, na ordem:

- **`ArrowEntity#addEffect` e a maior aposta desta rodada.** E a primeira vez
  que o mod faz uma flecha carregar efeito, e `StatusEffects.X` e
  `StatusEffect` no 1.20.1 mas `RegistryEntry<StatusEffect>` no 1.21 — a mesma
  divergencia que ja derrubou este mod com os sons de besta. Por isso a
  chamada vive em `DtbCompat.applyArrowEffect`, uma por versao: se a assinatura
  estiver errada e **erro de compilacao numa versao so**, nao crash de mixin em
  jogo. O codigo compartilhado passa apenas o nome do efeito como texto e nunca
  toca nesses tipos.
- **Brilho de encantado nao e forcado por codigo.** `Item#hasGlint` existia no
  1.20.1 e sumiu no 1.20.5, que passou a usar o componente
  `enchantment_glint_override`. Sao dois caminhos incompativeis para um detalhe
  cosmetico, entao quem faz o livro parecer encantado e a textura.
- **Som do rebolo trocado por precaucao.** `BLOCK_GRINDSTONE_USE` seria o
  natural, mas parte dos campos de `SoundEvents` virou `RegistryEntry` no 1.21
  e nao da para saber quais sem compilar. Usei `BLOCK_ANVIL_USE` grave, que ja
  e usado neste mesmo arquivo e portanto e comprovadamente seguro nas duas.
- **As nove texturas sao geradas por script e foram inspecionadas** (livro de
  capa escura, lombada e brasao central na cor do modulo). Passaram por duas
  revisoes: a segunda versao ficou pior que a primeira — virou listras
  coloridas em vez de livro — e foi refeita.

Verificacao: `javac` estrutural nas duas versoes sem erro, sem import orfao, os
45 JSON validos, e conferido que os 9 modulos tem lang (3 chaves cada), modelo,
textura e receita nas duas versoes — com o campo de resultado certo em cada
(`item` no 1.20.1, `id` no 1.21.1). `en_us` e `pt_br` tem exatamente o mesmo
conjunto de chaves. **Nao passou por `buildAll` nem por teste em jogo.**

**Decima sexta rodada — a documentacao virou duas.** O README tinha crescido
para 1462 linhas, e mais da metade delas eram *este* arquivo: status de
verificacao, historico de playtest e razao de cada decisao. Isso servia a quem
mexe no codigo e atrapalhava quem so quer jogar — as duas audiencias liam a
mesma parede de texto.

Agora sao dois documentos com contratos diferentes:

- **README.md** responde "o que o mod faz e como jogar": o que e, versoes
  compativeis, instalacao, o aviso de que o save e apagado, primeiros passos,
  cada recurso, e as **64 chaves de configuracao** agrupadas por assunto em
  blocos recolhiveis, com receitas prontas para os casos comuns.
- **docs/DESENVOLVIMENTO.md** (este arquivo) responde "por que esta assim e o
  que ainda pode quebrar".

Nada do conteudo tecnico foi descartado — o log inteiro foi movido, nao
resumido.

Sobre os espacos de imagem: cada um e um bloco visivel com o caminho e a
descricao do que a captura deve mostrar, **em vez de um `![](...)` apontando
para arquivo inexistente** — que renderizaria icone de imagem quebrada no
GitHub. `docs/images/README.md` repete a lista como checklist, com dicas de
como tirar cada foto (`/dtb forcewave` para nao esperar anoitecer,
`/dtb removenexus` para remontar cenario no mesmo save).

**Um buraco fechado de passagem:** as 9 texturas dos livros de modulo tinham
sido geradas por um script que ficou fora do repositorio. O README afirmava
que "as texturas sao geradas por script e so precisam ser refeitas se voce
mexer na arte", e para aquelas nove isso era falso — eram PNGs commitados sem
gerador. O script virou `tools/generate_turret_modules.py`, com o caminho raiz
derivado do proprio arquivo em vez de absoluto, e reexecuta-lo produz **zero
diff**. Ele tambem nao depende de Pillow, ao contrario dos outros dois: o PNG e
escrito na mao com `struct` + `zlib`.

Verificacao desta rodada: os 11 links internos do indice conferidos contra os
31 titulos, todos os links de arquivo dos tres documentos resolvem, os 9
espacos de imagem do README batem exatamente com as 9 linhas da tabela de
`docs/images/`, e **as 64 chaves de `DtbConfig` estao documentadas — nenhuma
sobrando nem faltando**, comparadas por script contra o codigo. A tabela de
receitas dos modulos foi conferida contando os ingredientes nos JSON gerados,
e pegou um erro: eu tinha escrito "2x luneta" para o modulo de Alcance quando
o padrao usa 3.

**Decima setima rodada — recolher a torreta deixou de ser destrutivo.**
Encontrado ao revisar o estado do projeto, nao relatado em playtest.

`TurretEntity#damage` manda **qualquer** ataque corpo a corpo de jogador para
`pickUp`, que devolvia `new ItemStack(ModItems.ARROW_TURRET)` — um item cru. Ou
seja, um clique esquerdo sem querer apagava nivel, encantamentos e modulos.

Nao era regressao: o nivel ja se perdia assim antes dos modulos existirem. Mas
a decima quinta rodada colocou um sistema caro em cima de um recipiente furado
— um modulo de Alcance III custa 9 lunetas e 3 perolas do ender, e sumia num
clique. Reposicionar torreta e acao rotineira e nao pode custar isso.

Agora `toItemStack()` guarda nivel, os seis encantamentos vanilla, os modulos,
o progresso de upgrade e a **vida atual** dentro do item, e
`ArrowTurretItem#useOnBlock` chama `applyFromStack` antes de spawnar.

Tres decisoes que mereciam nota:

- **A vida entra no pacote.** Se o item voltasse sempre com vida cheia,
  recolher e recolocar seria um reparo gratuito e o custo de material de
  `turretRepairHealthPerItem` deixaria de significar qualquer coisa.
- **Torreta de fabrica devolve item limpo.** Item com NBT nao empilha com item
  sem NBT; sem essa checagem, colocar e recolher uma torreta recem-fabricada
  quebraria a pilha do inventario sem motivo.
- **Morte continua limpando tudo.** Recolher com as maos e desmontar; ser
  derrubada pela horda e perder o investimento. Se a morte devolvesse o item
  completo, defender a torreta perderia o peso. E a unica parte do
  comportamento antigo que ficou de proposito.

Compatibilidade: guardar dados num `ItemStack` e mais uma divergencia dura —
NBT direto no 1.20.1, componente `CUSTOM_DATA` no 1.21, que nem tem mais NBT de
item. Virou o par `DtbCompat.putStackTag` / `getStackTag`, contido como sempre:
se a assinatura estiver errada e erro de compilacao numa versao, nao crash em
jogo. `NbtComponent.of` / `copyNbt` sao de primeiro uso no projeto; evitei
`NbtComponent.DEFAULT` usando `get` + checagem de null, que e um simbolo a
menos para dar errado.

De quebra, o algarismo romano do grau tinha **tres** copias (mensagem de chat,
aba de estatisticas e agora o tooltip do item). Virou `TurretModifiers.grade`,
fonte unica — conferido por script que so resta uma ocorrencia de `"III"` no
codigo.

Verificacao: `javac` estrutural nas duas versoes sem erro, sem import orfao,
`en_us` e `pt_br` identicas em conjunto de chaves. **Continua sem `buildAll` e
sem teste em jogo.**
