package com.defendtheblock.config;

import com.defendtheblock.DefendTheBlock;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuracao do mod, salva em {@code config/defendtheblock.json}.
 *
 * <p>Todos os valores tem um padrao seguro, entao o arquivo pode ser apagado a
 * qualquer momento que ele e recriado no proximo boot.
 */
public final class DtbConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DtbConfig instance;

    // ------------------------------------------------------------ o Nexus
    /** Vida do bloco Nexus. Cada golpe de mob tira uma fracao disso. */
    public int nexusMaxHealth = 400;
    /** Dano que um mob comum causa por golpe no Nexus. */
    public int nexusDamagePerHit = 3;
    /** Intervalo (ticks) entre golpes de um mesmo mob no Nexus. */
    public int nexusHitCooldown = 20;
    /**
     * Apaga a pasta do mundo quando o Nexus e destruido.
     *
     * <p><b>ATENCAO:</b> isso remove o save permanentemente. Coloque em
     * {@code false} se voce so quer a mensagem de derrota.
     */
    public boolean deleteWorldOnNexusDestroyed = true;
    /**
     * Mantem os chunks em volta do Nexus sempre carregados, mesmo sem nenhum
     * jogador por perto (usa o forceload do proprio Minecraft).
     */
    public boolean keepNexusChunksLoaded = true;
    /**
     * Raio, <b>em chunks</b>, da area mantida carregada em volta do Nexus.
     *
     * <p>0 = so o chunk do Nexus, 1 = 3x3, 4 = 9x9 (o padrao). O padrao cobre
     * exatamente a area de spawn e de atracao, para que a invasao rode inteira
     * mesmo sem jogador por perto.
     */
    public int forcedChunkRadius = 4;

    // ----------------------------------------------------------- invasoes
    /** Multiplicador global do ritmo de spawn da invasao. */
    public double mobMultiplier = 1.0D;
    /**
     * Raio, <b>em chunks</b>, da <b>zona livre de spawn</b> em volta do Nexus.
     *
     * <p>Medido em distancia de Chebyshev (quadrada), que e como o jogo enxerga
     * vizinhanca de chunk: 0 = so o chunk do Nexus, <b>1 = o chunk do Nexus
     * mais todos os adjacentes (3x3)</b>, que e o padrao.
     *
     * <p>Dentro dessa area <b>nenhum mob hostil nasce</b> — nem a invasao
     * spawna ali, nem o spawn natural do vanilla. E o quintal seguro da base:
     * o que aparecer perto do Nexus veio marchando de fora, nao brotou do lado.
     */
    public int noSpawnChunkRadius = 1;
    /**
     * Largura, <b>em chunks</b>, do anel onde a invasao nasce, contada a partir
     * da borda da zona livre de spawn.
     *
     * <p>Com os padroes (zona livre de raio 1 + anel de 3), os invasores nascem
     * entre o chunk 2 e o chunk 4 a partir do Nexus. O limite externo e
     * <b>circular</b> (distancia euclidiana), entao a area de spawn e uma coroa
     * em volta do quadrado seguro, nao outro quadrado.
     */
    public int spawnRingChunks = 3;
    /**
     * Raio, <b>em chunks</b>, no qual qualquer mob hostil que nascer (spawn
     * natural, spawner ou ovo) e atraido pelo Nexus e ganha as habilidades de
     * invasor. Endermen sao a unica excecao.
     *
     * <p>Vale so fora da zona livre de spawn: dentro dela nada nasce para ser
     * recrutado.
     */
    public int attractionChunkRadius = 4;
    /** Ticks entre cada lote de spawn na invasao 1. Diminui a cada invasao. */
    public int baseSpawnInterval = 60;
    /** Quanto o intervalo entre lotes encolhe a cada invasao, em ticks. */
    public int spawnIntervalStepPerWave = 4;
    /** Menor intervalo de spawn possivel, em ticks. */
    public int minSpawnInterval = 10;
    /** Quantos invasores nascem por lote na invasao 1. */
    public double baseSpawnBatch = 2.0D;
    /** Quanto o lote cresce a cada invasao. */
    public double spawnBatchGrowthPerWave = 0.5D;
    /** Teto de invasores vivos ao mesmo tempo na invasao 1. */
    public int baseConcurrentInvaders = 30;
    /** Quanto esse teto sobe a cada invasao. */
    public int concurrentInvadersPerWave = 5;
    /**
     * Teto absoluto de invasores vivos ao mesmo tempo.
     *
     * <p>A invasao nao tem numero fixo de mobs: ela spawna sem parar ate o fim
     * da noite, entao e este valor que segura a carga do servidor. Baixe se o
     * servidor sofrer nas invasoes altas.
     */
    public int maxConcurrentInvaders = 100;
    /**
     * Quantos zumbis <b>extras</b> nascem junto sempre que o sorteio tira um
     * zumbi.
     *
     * <p>E diferente de so aumentar o peso no sorteio: o peso muda a
     * proporcao, isto aqui muda a <i>quantidade</i>. O zumbi e a base da
     * horda, entao ele chega em grupo enquanto os outros tipos chegam um a um.
     */
    public int zombieExtraSpawnCount = 2;
    /**
     * Invasores dropam item quando morrem?
     *
     * <p>Padrao false: a invasao spawna centenas de mobs por noite, entao os
     * drops viravam uma montanha de itens (e de lag) que nao tem nada a ver
     * com o desafio. A recompensa da noite vem do saque que o Nexus solta ao
     * amanhecer, nao de farmar a horda.
     */
    public boolean invadersDropLoot = false;

    // ------------------------------------------------- chances de habilidade
    // Sempre abaixo dos mobs "normais": a soma das habilidades especiais de um
    // tipo de mob nunca chega perto de 100%.
    /**
     * Chance de um creeper receber a habilidade de se explodir no obstaculo.
     *
     * <p>Padrao 1.0 (todos): desde que o peso de spawn do creeper caiu muito,
     * um creeper que nao sabe abrir passagem simplesmente nao contribui em
     * nada quando a horda encontra um muro.
     */
    public double creeperBreachChance = 1.0D;
    /**
     * Tempo maximo, em ticks, entre um creeper marcar um obstaculo e acender.
     *
     * <p>Ele acende na hora se ja estiver encostado no obstaculo; este valor e
     * o teto para o caso de ficar preso tentando chegar la. 100 ticks = 5s.
     */
    public int creeperBreachTimeoutTicks = 100;
    public double spiderWebChance = 0.25D;
    public double zombiePickaxeChance = 0.18D;
    public double zombieLadderChance = 0.16D;
    public double zombieTntChance = 0.07D;
    /**
     * Chance de um zumbi nascer com isqueiro. Ele nao depende de haver
     * construcao de madeira por perto para nascer — a habilidade e sorteada
     * como qualquer outra, e so o <i>uso</i> dela e que precisa de um
     * obstaculo de madeira.
     */
    public double zombieFireStarterChance = 0.10D;
    /** Chance de um zumbi nascer construtor (ergue caminho/pilar ate o Nexus). */
    public double zombieBuilderChance = 0.12D;
    /**
     * Multiplicador aplicado as chances de zumbi com escada e de zumbi
     * construtor quando o Nexus esta suspenso no ar ou bem acima do chao.
     *
     * <p>Sao justamente as duas habilidades que resolvem esse cenario, entao
     * faz sentido a horda trazer mais delas quando ele acontece.
     */
    public double elevatedNexusBuilderBonus = 2.5D;

    /** Dureza maxima de bloco que um zumbi mineiro consegue quebrar. */
    public double maxMineHardness = 30.0D;
    /** Ticks que o zumbi leva minerando um bloco (multiplicado pela dureza). */
    public int mineTicksPerHardness = 14;
    /**
     * Multiplicador de tempo para quem quebra bloco <b>sem picareta</b>.
     *
     * <p>Todo invasor consegue abrir buraco numa parede — a picareta nao e mais
     * requisito, so vantagem. Com 3.0, o zumbi mineiro leva um terco do tempo
     * que os outros levam no mesmo bloco.
     */
    public double unarmedMineTicksMultiplier = 3.0D;
    /**
     * Angulo total do campo de visao do invasor, em graus.
     *
     * <p>Usado para decidir se ele <i>enxerga</i> um jogador ou uma torreta e
     * pode escolher como alvo. Fora do cone, ele simplesmente segue para o
     * Nexus. 120 graus e o cone humano aproximado: 60 para cada lado do rumo
     * em que a cabeca esta virada.
     */
    public double invaderFieldOfViewDegrees = 120.0D;
    /**
     * Deixa qualquer invasor construir um caminho de blocos (pilar/pontilhar)
     * quando fica preso e o Nexus esta visivelmente acima dele. E uma
     * heuristica simples, nao um pathfinder — desligue se nao quiser mobs
     * colocando bloco no mundo.
     */
    public boolean invadersCanBridge = true;
    /**
     * Alcance, em blocos, dentro do qual um invasor mantem o alvo de combate
     * atual (jogador ou torreta) em vez de esquece-lo e voltar para o Nexus.
     *
     * <p>E o que garante que o Nexus continua sendo a prioridade: um alvo mais
     * longe que isso e "esquecido" a cada tick, entao o mob nunca sai do
     * caminho para cacar algo distante — so briga com o que realmente estiver
     * bloqueando a passagem. Com varias torretas espalhadas, isso evita que o
     * invasor tenha que mata-las todas em sequencia antes de sequer tentar o
     * bloco.
     */
    public double nexusPriorityEngageRange = 6.0D;
    /**
     * Alcance, em blocos, no qual um invasor <b>de ataque a distancia</b>
     * (esqueleto e afins) prioriza atirar numa torreta.
     *
     * <p>E bem maior que {@code nexusPriorityEngageRange} de proposito: ele
     * atira parado, sem sair do caminho, entao priorizar a torreta de longe
     * nao atrapalha a marcha ate o Nexus.
     */
    public double rangedTurretPriorityRange = 20.0D;
    /**
     * Teto de tempo, em ticks, que qualquer invasor fica focado numa torreta
     * antes de desistir e voltar para o Nexus.
     */
    public int maxTurretEngageTicks = 200;
    /** Carencia, em ticks, antes do invasor poder mirar em outra torreta. */
    public int turretIgnoreTicksAfterGiveUp = 120;
    /** Ticks por ponto de dureza para arrombar uma porta fechada. */
    public int doorBreakTicksPerHardness = 10;

    // ------------------------------------------------------------- torreta
    /** Multiplicador de dano global das torretas. */
    public double turretDamageMultiplier = 1.0D;
    /** Torretas gastam municao? Deixe false para municao infinita. */
    public boolean turretConsumesAmmo = true;
    /** Vida recuperada por unidade de material usada no reparo (botao direito). */
    public double turretRepairHealthPerItem = 8.0D;
    /**
     * Abertura vertical do cone de visao da torreta, em graus para cima e para
     * baixo.
     *
     * <p>Ela gira 360 graus na horizontal, mas so inclina ate este limite —
     * o que deixa dois pontos cegos, um logo acima e outro logo abaixo dela.
     * Mobs fora do cone nunca sao escolhidos como alvo, entao ela nunca trava
     * mirando algo que jamais conseguiria apontar. Suba para 80+ se quiser
     * pontos cegos bem pequenos.
     */
    public double turretVerticalFovDegrees = 60.0D;

    // --------------------------------------------------------------- totem
    /** Cooldown do Totem de Reuniao, em ticks. */
    public int totemCooldown = 600;

    public static DtbConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(DefendTheBlock.MOD_ID + ".json");
    }

    private static DtbConfig load() {
        Path file = path();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                DtbConfig loaded = GSON.fromJson(reader, DtbConfig.class);
                if (loaded != null) {
                    loaded.save();
                    return loaded;
                }
            } catch (Exception e) {
                DefendTheBlock.LOGGER.error("Falha ao ler {}, usando os valores padrao.", file, e);
            }
        }
        DtbConfig fresh = new DtbConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            DefendTheBlock.LOGGER.error("Nao foi possivel salvar {}", file, e);
        }
    }
}
