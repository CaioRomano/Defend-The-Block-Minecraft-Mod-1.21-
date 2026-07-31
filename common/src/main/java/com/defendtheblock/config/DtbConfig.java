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
     * <p>0 = so o chunk do Nexus, 1 = 3x3, 3 = 7x7 (o padrao). O padrao cobre
     * exatamente a area de spawn e de atracao, para que a invasao rode inteira
     * mesmo sem jogador por perto.
     */
    public int forcedChunkRadius = 3;

    // ----------------------------------------------------------- invasoes
    /** Multiplicador global do ritmo de spawn da invasao. */
    public double mobMultiplier = 1.0D;
    /**
     * Distancia minima de spawn, <b>em chunks</b>, contada a partir do chunk do
     * Nexus. Com 1, toda a area de ativacao e usada menos o chunk do Nexus.
     *
     * <p>O chunk do Nexus (distancia 0) <b>nunca</b> spawna invasor, mesmo que
     * este valor seja colocado em 0: e uma regra fixa do mod.
     */
    public int spawnChunkRadiusMin = 1;
    /** Distancia maxima de spawn, em chunks, a partir do chunk do Nexus. */
    public int spawnChunkRadiusMax = 3;
    /**
     * Raio, <b>em chunks</b>, no qual qualquer mob hostil que nascer (spawn
     * natural, spawner ou ovo) e atraido pelo Nexus e ganha as habilidades de
     * invasor. Endermen sao a unica excecao.
     */
    public int attractionChunkRadius = 3;
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

    // ------------------------------------------------- chances de habilidade
    // Sempre abaixo dos mobs "normais": a soma das habilidades especiais de um
    // tipo de mob nunca chega perto de 100%.
    public double creeperBreachChance = 0.35D;
    public double spiderWebChance = 0.25D;
    public double zombiePickaxeChance = 0.12D;
    public double zombieLadderChance = 0.10D;
    public double zombieTntChance = 0.05D;

    /** Dureza maxima de bloco que um zumbi mineiro consegue quebrar. */
    public double maxMineHardness = 30.0D;
    /** Ticks que o zumbi leva minerando um bloco (multiplicado pela dureza). */
    public int mineTicksPerHardness = 14;
    /**
     * Deixa qualquer invasor construir um caminho de blocos (pilar/pontilhar)
     * quando fica preso e o Nexus esta visivelmente acima dele. E uma
     * heuristica simples, nao um pathfinder — desligue se nao quiser mobs
     * colocando bloco no mundo.
     */
    public boolean invadersCanBridge = true;
    /**
     * Raio, em blocos, no qual um invasor enxerga a torreta como um alvo valido
     * a distancia — nao depende do alcance de perseguicao proprio do mob.
     */
    public double turretDetectionRadius = 64.0D;

    // ------------------------------------------------------------- torreta
    /** Multiplicador de dano global das torretas. */
    public double turretDamageMultiplier = 1.0D;
    /** Torretas gastam municao? Deixe false para municao infinita. */
    public boolean turretConsumesAmmo = true;

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
