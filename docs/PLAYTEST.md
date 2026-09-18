# Roteiro de playtest

Lista do que **ainda nao foi verificado em jogo**, com o passo a passo exato de
cada teste. Marque conforme for passando.

O corte e claro: o ultimo playtest relatado foi o que gerou a **decima segunda
rodada** do [diario](DESENVOLVIMENTO.md). Tudo dali para frente — as correcoes
daquela rodada inclusive — nunca rodou. Sao 5 rodadas de mudanca acumulada:

| Rodada | O que entrou | Risco |
|---|---|---|
| 12 | Nexus emparedado, pavio de 3s, rampa, esqueleto | correcoes nunca confirmadas |
| 13 | otimizacoes de IA e torreta | **regressao** — comportamento deveria ser identico |
| 14 | escalada de vida/dano, teto de 100 | novo |
| 15 | 9 modulos de torreta | **inteiramente novo** |
| 17 | recolher torreta preserva estado | novo |

`./gradlew buildAll` passa nas duas versoes (commit `28833d8`). Isso prova
assinatura de API, **nao** comportamento — e o que esta lista existe para
cobrir.

---

## 1. Como rodar

Tudo no PowerShell, a partir da pasta do projeto:

```powershell
cd C:\caminho\para\Defend-The-Block-Minecraft-Mod-1.21-
```

| Comando | O que faz |
|---|---|
| `.\gradlew :fabric-1.21.1:runClient` | Abre o Minecraft 1.21.1 com o mod. **Comece por aqui.** |
| `.\gradlew :fabric-1.20.1:runClient "-Dorg.gradle.java.home=C:\Program Files\Java\jdk-17"` | O mesmo na 1.20.1 (essa versao espera Java 17) |
| `.\gradlew :fabric-1.21.1:runServer` | Servidor dedicado, se quiser testar multiplayer |
| `.\gradlew buildAll` | So compila, sem abrir o jogo |

A primeira execucao baixa Minecraft, mappings e Fabric API — demora e precisa de
internet. As seguintes sao rapidas.

### Onde o mundo de teste fica

`fabric-1.21.1\run\saves\` — essa pasta e **gitignored**, ou seja, descartavel e
fora do repositorio. E o lugar certo para testar um mod que apaga saves. Nao
teste no seu Minecraft de verdade.

### Preparar o mundo

Crie um mundo novo **Criativo**, semente plana se preferir, e:

```
/gamemode creative
/difficulty hard
/gamerule doDaylightCycle true
```

Todos os itens estao na aba **Defend The Block** do inventario criativo. Se
preferir comando:

```
/give @s defendtheblock:nexus_block
/give @s defendtheblock:arrow_turret 16
/give @s minecraft:arrow 256
/give @s defendtheblock:turret_module_range 8
/give @s defendtheblock:turret_module_damage 8
/give @s defendtheblock:turret_module_fortitude 8
/give @s defendtheblock:turret_module_rapid 8
/give @s defendtheblock:turret_module_quiver 8
/give @s defendtheblock:turret_module_volley 8
/give @s defendtheblock:turret_module_scavenger 8
/give @s defendtheblock:turret_module_frost 8
/give @s defendtheblock:turret_module_venom 8
/give @s minecraft:grindstone
/give @s minecraft:iron_ingot 64
/give @s minecraft:gold_ingot 64
/give @s minecraft:diamond 64
/give @s minecraft:emerald 64
```

### Dois atalhos que economizam horas

**Pular a carencia:** `/dtb forcewave` **ignora** os 3 dias de carencia. Nao
precisa esperar.

**Avancar de invasao rapido:** `/dtb forcewave` **durante o dia** deve fechar a
onda no tick seguinte e contar como vencida, subindo o contador. Repetir leva ao
numero de invasao que voce quiser sem jogar 24 noites.

```
/time set day
/dtb forcewave      (repita; confira com /dtb status a cada poucas vezes)
```

> Isso foi **deduzido do codigo**, nao testado. Confirme com `/dtb status` que o
> contador realmente sobe. Se nao subir, o caminho e sobreviver as noites mesmo,
> ou mexer na config (secao 6).

---

## 2. Fase 0 — o mod carrega?

- [ ] **0.1 — Boot 1.21.1.** `.\gradlew :fabric-1.21.1:runClient` abre ate o menu
      principal sem crash.
      *Falha:* crash na inicializacao. Procure `Mixin` no log — foi assim que o
      crash da oitava rodada apareceu.
- [ ] **0.2 — Aviso de mixin opcional.** No log, procure por
      `defendtheblock-drops`. Um aviso ali e **esperado e tolerado** (o config e
      `"required": false`); um crash nao e.
- [ ] **0.3 — Boot 1.20.1.** Mesmo teste na outra versao.
- [ ] **0.4 — Aba do criativo.** A aba "Defend The Block" existe e mostra
      **17 itens**: Nexus, torreta, totem, 5 ovos e **9 livros de modulo**.
- [ ] **0.5 — Texturas.** Nenhum item aparece como cubo roxo/preto. Os 9 livros
      tem cores distintas entre si.
- [ ] **0.6 — Idioma.** Nomes e tooltips em portugues (ou ingles, conforme o
      idioma do jogo) — nada mostrando a chave crua tipo
      `item.defendtheblock.turret_module_range`.

---

## 3. Fase 1 — o laco basico

- [ ] **1.1 — Nexus coloca e ativa.** Som, particulas, mensagem no chat, HUD no
      canto superior direito com a barra de vida.
- [ ] **1.2 — Nexus e indestrutivel.** Tente quebrar no criativo, com TNT, e
      empurrar com pistao. Nada deve funcionar.
- [ ] **1.3 — Segundo Nexus volta.** Coloque outro: ele deve retornar ao
      inventario.
- [ ] **1.4 — Aviso de carencia.** Com `/time set day` repetido, o aviso na tela
      aparece uma vez por dia com a contagem, e no dia da estreia muda de
      mensagem (com trompa).
- [ ] **1.5 — Invasao comeca.** `/time set night` e espere, ou `/dtb forcewave`.
      Mobs nascem **fora** do quintal e marcham ate o Nexus.
- [ ] **1.6 — Quintal livre de spawn.** Nada hostil nasce no chunk do Nexus nem
      nos 8 adjacentes. Animais **devem** continuar nascendo (a regra e so para
      hostis).
- [ ] **1.7 — HUD acompanha.** Invasao atual, mobs vivos e nascidos batem com
      `/dtb status`.
- [ ] **1.8 — Amanhecer fecha a onda.** `/time set day`: a horda some, aparece a
      mensagem de onda vencida e o **saque** cai em volta do Nexus.
- [ ] **1.9 — Faxina.** Cobblestone, escadas e teias que os mobs colocaram
      **somem** ao amanhecer, sem dropar item. O que **voce** construiu fica.

---

## 4. Fase 2 — regressoes da rodada de otimizacao

> Esta fase e a mais chata e a mais importante. A rodada 13 reescreveu caminhos
> quentes prometendo **comportamento identico**. Se algo aqui falhar, e
> regressao minha.

- [ ] **2.1 — Nexus emparedado nao toma dano.** Feche o Nexus numa caixa 100%
      solida de pedra. Rode uma invasao inteira. `/dtb status` antes e depois: a
      vida do Nexus **nao pode cair**.
      *Este e o teste que mais quero ver* — `findCover` foi reescrito de `Vec3d`
      para `BlockPos.Mutable` na rodada 13.
- [ ] **2.2 — Emparedar nao e imunidade.** Na mesma caixa, a horda deve
      **minerar / explodir / contornar** e acabar entrando. Se ficarem parados
      encostados na parede sem fazer nada, e o bug de horda congelada voltando.
- [ ] **2.3 — Mob empacado contorna.** Faca um muro em L deixando uma passagem 3
      blocos ao lado da linha reta. Os mobs devem **achar a passagem**, nao
      martelar a parede.
- [ ] **2.4 — Torreta bloqueia torreta.** Ponha duas torretas em fila, olhando
      para o mesmo corredor, e um mob no fim.
      - A da frente atira.
      - A de tras **nao** atira atraves dela.
      - Destrua a da frente: a de tras deve voltar a atirar **em ate ~5 ticks**.
- [ ] **2.5 — Sem fogo amigo.** Nenhuma flecha de torreta fere outra torreta.
- [ ] **2.6 — Espaco para quem trabalha.** Com um zumbi mineiro cavando, os
      outros recuam. Mas um mob **em alcance de golpe do Nexus nao recua** — ele
      bate no bloco mesmo com um trabalhador do lado.
- [ ] **2.7 — Torreta acerta abaixo dela.** Torre de 15 blocos, mobs no chao a
      uns 10 de distancia horizontal. Ela deve acertar.
- [ ] **2.8 — Pontos cegos existem.** Mob **exatamente** embaixo da torreta nao
      e alvo (cone de 60 graus) — e correto, nao e bug.
- [ ] **2.9 — Esqueleto mantem distancia.** Com torreta ou voce a vista, ele
      recua e atira. Sem alvo, avanca para o Nexus.
- [ ] **2.10 — Creeper.** Explode em obstaculo com pavio de ~3s depois de
      encostar, e **nao** tira vida do Nexus.

---

## 5. Fase 3 — o que e novo

### 5a. Modulos de torreta (rodada 15 — nada disso rodou)

- [ ] **3.1 — Receitas.** Os 9 livros aparecem no livro de receitas e craftam.
      Confira a do **Alcance**: sao **3 lunetas**, nao 2.
- [ ] **3.2 — Instalar.** Clique com um livro numa torreta: mensagem de
      instalado, e o modulo aparece na **aba de estatisticas** (clique direito de
      mao vazia).
- [ ] **3.3 — Subir de grau.** Aplique o mesmo livro 3 vezes: deve ir I → II →
      III.
- [ ] **3.4 — Grau maximo recusa.** Na 4a vez: mensagem de grau maximo e o livro
      **continua no inventario** (confira a contagem).
- [ ] **3.5 — Teto de 2 tipos.** Instale dois tipos diferentes, tente um
      terceiro: mensagem de recusa e o livro **nao e consumido**.
- [ ] **3.6 — Rebolo desmonta.** Clique com rebolo: todos os modulos somem, o
      rebolo **nao** e gasto. Os livros nao voltam (correto).
- [ ] **3.7 — Rebolo em torreta limpa.** Mensagem de "nenhum modulo", sem efeito.
- [ ] **3.8 — Alcance.** Anote o alcance na aba antes e depois. Grau 1 deve dar
      +15%.
- [ ] **3.9 — Dano.** Idem, +20% por grau na aba.
- [ ] **3.10 — Fortificacao.** A vida **maxima** sobe 25% por grau, e a torreta
      ganha a vida extra na hora (nao fica com a barra pela metade).
- [ ] **3.11 — Cadencia.** O tempo de recarga na aba cai ~15% por grau.
- [ ] **3.12 — Aljava.** A capacidade sobe 50% por grau (64 → 96 no grau 1, numa
      torreta de madeira).
- [ ] **3.13 — Salva.** Saem 2 flechas por disparo no grau 1, e a municao cai
      **so 1**.
- [ ] **3.14 — Catador.** Dispare ~20 vezes no grau 3: a municao deve cair bem
      menos que 20. Nao deve ser infinita.
- [ ] **3.15 — Gelo.** Um mob acertado fica com **Lentidao**. Confirme com:
      `/data get entity @e[type=zombie,limit=1,sort=nearest] active_effects`
- [ ] **3.16 — Veneno.** Idem, com **Veneno**.
- [ ] **3.17 — Efeito soma com flecha de pocao.** Carregue flecha de lentidao
      numa torreta com Veneno: o mob deve pegar **os dois**.
- [ ] **3.18 — Persistencia.** Saia do mundo e volte: os modulos continuam
      instalados e a aba mostra os mesmos graus.

### 5b. Recolher a torreta (rodada 17)

- [ ] **3.19 — Estado preservado.** Suba uma torreta para nivel Ferro, instale 2
      modulos, deixe ela ferida. **Soque** ela. O tooltip do item deve mostrar
      nivel, vida e os modulos.
- [ ] **3.20 — Restaura ao recolocar.** Coloque de volta: nivel, modulos e
      **vida ferida** voltam iguais. A vida **nao** pode voltar cheia (seria
      reparo gratis).
- [ ] **3.21 — Torreta nova empilha.** Coloque uma torreta recem-fabricada e
      soque na hora: o item deve **empilhar** com as outras novas do inventario.
- [ ] **3.22 — Morte limpa.** Deixe a horda destruir uma torreta melhorada: o
      item que cai deve ser **cru**, sem nivel nem modulos (comportamento
      intencional).

### 5c. Escalada de dificuldade (rodada 14)

- [ ] **3.23 — Noite 1 e facil.** Primeira invasao: no maximo **10 mobs vivos**,
      um a cada ~7s, sem armadura na maioria.
- [ ] **3.24 — Vida escala de verdade.** Avance ate a invasao ~11 e meca:
      ```
      /data get entity @e[type=zombie,limit=1,sort=nearest] Health
      ```
      Zumbi vanilla tem 20. Na invasao 11 deve dar **~34** (1.72x).
- [ ] **3.25 — Dano escala.** Mesmo mob:
      ```
      /data get entity @e[type=zombie,limit=1,sort=nearest] Attributes
      ```
      Procure `generic.attack_damage`. Deve estar acima do base.
- [ ] **3.26 — Esqueleto nao escala dano** (esperado, nao e bug): o dano da
      flecha vem do projetil.
- [ ] **3.27 — Teto para de crescer.** Na invasao 30, a vida nao passa de 3x e o
      teto nao passa de 100.
- [ ] **3.28 — A torreta ainda mata.** *O risco de balanceamento mais provavel.*
      Numa invasao alta, uma torreta de esmeralda com modulo de Dano ainda derruba
      zumbis num tempo razoavel? Se virou esponja, o botao e
      `invaderHealthMultiplierMax`.

---

## 6. Fase 4 — carga (o teste que eu mais quero ver)

Para chegar aos 100 mobs sem jogar 24 noites, edite
`fabric-1.21.1\run\config\defendtheblock.json`:

```json
{
  "baseConcurrentInvaders": 100,
  "baseSpawnBatch": 10,
  "baseSpawnInterval": 20
}
```

Reinicie o mundo e rode `/dtb forcewave` a noite.

- [ ] **4.1 — O teto e atingido.** `/dtb status` chega perto de 100 vivos.
- [ ] **4.2 — O teto e respeitado.** Nao passa de 100.
- [ ] **4.3 — Reabastece.** Mate alguns: nascem outros, mantendo o teto.
- [ ] **4.4 — TPS aguenta.** F3 e olhe o grafico de ms por tick. Acima de 50ms o
      servidor esta atrasando.
- [ ] **4.5 — Sem pico periodico.** A rodada 14 espalhou os timers justamente
      para a horda nao recalcular rota toda no mesmo tick. Se o grafico mostrar
      um **pico regular a cada ~1 segundo**, o espalhamento nao funcionou.
- [ ] **4.6 — Devolva a config aos padroes** depois do teste.

---

## 7. Fase 5 — o resto

- [ ] **5.1 — Ovos de invasor.** Cada um dos 5 nasce com a habilidade certa e
      **nao** entra na contagem do HUD.
- [ ] **5.2 — Zumbi mineiro** cava parede. **Escadas** monta coluna. **TNT**
      arremessa em arco. **Isqueiro** ateia fogo em madeira. **Construtor** ergue
      pilar.
- [ ] **5.3 — Nexus suspenso.** Ponha o Nexus a 10 blocos do chao: a horda deve
      construir ponte/escada ate ele.
- [ ] **5.4 — Portas sao arrombadas**, nao apenas abertas.
- [ ] **5.5 — Aranha** escala e cospe teia.
- [ ] **5.6 — Aba de estatisticas.** Abre nitida (sem borrao), texto nao vaza da
      moldura, botao Fechar funciona.
- [ ] **5.7 — Reparo.** Material do nivel atual cura; do proximo nivel sobe de
      nivel, acumulando por unidade.
- [ ] **5.8 — Totem** puxa quem carrega totem, com cooldown de 30s.
- [ ] **5.9 — `/dtb removenexus`** tira o Nexus **sem apagar o mundo**, e os mobs
      antigos **nao** ficam atacando o vazio nem ferem um Nexus novo.
- [ ] **5.10 — `/dtb stopwave`** cancela sem contar como vencida.
- [ ] **5.11 — Multiplayer** (opcional): `runServer`, dois jogadores, HUD
      sincronizado nos dois.

---

## 8. Fase 6 — a derrota (por ultimo, de proposito)

> Este teste **apaga a pasta do mundo**. Faca so no mundo de `run/`, que e
> descartavel, e so depois de tudo o mais.

- [ ] **6.1 — Derrota sem apagar.** Com `"deleteWorldOnNexusDestroyed": false`,
      deixe o Nexus cair: mensagem de derrota, mundo **intacto**.
- [ ] **6.2 — Derrota apagando.** Com `true` (o padrao), deixe cair: desconecta,
      desliga e a pasta em `run\saves\` some.

---

## 9. Como relatar

Para cada falha, o que mais ajuda:

1. **Numero do teste** desta lista
2. **O que voce esperava e o que aconteceu**
3. **Log**: `fabric-1.21.1\run\logs\latest.log` — ou o arquivo de crash em
   `run\crash-reports\`
4. **Versao**: 1.20.1 ou 1.21.1
5. Se for comportamento de mob ou torreta, a saida de `/dtb status`

Bug de mixin aparece no **boot**; bug de logica aparece **jogando**. Os dois
tipos ja apareceram neste projeto.
