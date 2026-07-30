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

    // ----------------------------------------------------------- invasoes
    /** Multiplicador global da quantidade de mobs por invasao. */
    public double mobMultiplier = 1.0D;
    /** Raio (blocos) em volta do Nexus onde os mobs sao spawnados. */
    public int spawnRadius = 44;
    /** Raio minimo de spawn, para os mobs nao nascerem em cima do jogador. */
    public int minSpawnRadius = 22;
    /**
     * Raio no qual qualquer mob hostil que nascer no mundo (spawn natural ou
     * spawner) e atraido pelo Nexus e ganha as habilidades de invasor.
     */
    public int attractionRadius = 120;
    /** Ticks entre cada lote de spawn na invasao 1. Diminui a cada invasao. */
    public int baseSpawnInterval = 100;
    /** Menor intervalo de spawn possivel, em ticks. */
    public int minSpawnInterval = 20;
    /** Numero maximo de invasores vivos ao mesmo tempo. */
    public int maxConcurrentInvaders = 90;

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
