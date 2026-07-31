package com.defendtheblock.invasion;

import com.defendtheblock.config.DtbConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Estado da campanha de invasoes de um mundo.
 *
 * <p>Esta classe e um POJO puro de proposito: {@code PersistentState} mudou de
 * assinatura entre 1.20.1 e 1.21, entao cada versao tem o seu proprio wrapper
 * (veja {@code com.defendtheblock.compat.DtbCompat#getInvasionData}) e toda a
 * logica compartilhada mexe apenas neste objeto.
 */
public class InvasionData {

    private Runnable dirtyMarker = () -> {
    };

    private BlockPos nexusPos;
    private int nexusHealth;
    private int nexusMaxHealth;

    private int wavesCompleted;
    private int currentWave;
    private boolean waveActive;

    /** Quantos invasores ja nasceram nesta noite (a invasao nao tem teto fixo). */
    private int mobsSpawned;
    private final Set<UUID> activeInvaders = new LinkedHashSet<>();

    private double multiplier = 1.0D;
    private long lastWaveDay = -1L;
    private boolean gameOver;

    /** Raio (em chunks) atualmente mantido carregado, ou -1 se nenhum. */
    private int forcedRadius = -1;

    /** Contador ate o proximo lote de spawn. Nao precisa ser persistido, mas e barato. */
    private int spawnTimer;

    /**
     * Todo bloco que um invasor colocou no mundo nesta invasao.
     *
     * <p>Guardado para ser desfeito no fim da noite: cobblestone de
     * pilar/ponte, escadas dos zumbis carpinteiros, teia de aranha. Persistido
     * porque uma invasao atravessa reinicios de servidor, e sem isso o mundo
     * iria acumulando entulho invasao apos invasao.
     */
    private final Set<Long> placedBlocks = new LinkedHashSet<>();

    public void setDirtyMarker(Runnable dirtyMarker) {
        this.dirtyMarker = dirtyMarker;
    }

    public void markDirty() {
        dirtyMarker.run();
    }

    // ------------------------------------------------------------ o Nexus

    public boolean hasNexus() {
        return nexusPos != null;
    }

    public BlockPos getNexusPos() {
        return nexusPos;
    }

    public void placeNexus(BlockPos pos) {
        this.nexusPos = pos.toImmutable();
        this.nexusMaxHealth = DtbConfig.get().nexusMaxHealth;
        this.nexusHealth = this.nexusMaxHealth;
        this.wavesCompleted = 0;
        this.currentWave = 0;
        this.waveActive = false;
        this.gameOver = false;
        this.lastWaveDay = -1L;
        this.forcedRadius = -1;
        this.activeInvaders.clear();
        markDirty();
    }

    /** Raio (em chunks) que esta forcado agora em volta do Nexus, ou -1. */
    public int getForcedRadius() {
        return forcedRadius;
    }

    public void setForcedRadius(int forcedRadius) {
        this.forcedRadius = forcedRadius;
        markDirty();
    }

    public int getNexusHealth() {
        return nexusHealth;
    }

    public int getNexusMaxHealth() {
        return nexusMaxHealth <= 0 ? DtbConfig.get().nexusMaxHealth : nexusMaxHealth;
    }

    /** @return true se o golpe derrubou o Nexus. */
    public boolean damageNexus(int amount) {
        nexusHealth = Math.max(0, nexusHealth - amount);
        markDirty();
        return nexusHealth <= 0;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
        markDirty();
    }

    /**
     * Limpa o Nexus e toda a campanha sem passar pelo fluxo de derrota
     * (usado pelo comando admin {@code /dtb removenexus}): nao apaga o mundo
     * nem marca {@code gameOver}, apenas deixa o mundo pronto para um novo
     * Nexus ser colocado.
     */
    public void clearNexus() {
        this.nexusPos = null;
        this.nexusHealth = 0;
        this.nexusMaxHealth = 0;
        this.wavesCompleted = 0;
        this.currentWave = 0;
        this.waveActive = false;
        this.mobsSpawned = 0;
        this.activeInvaders.clear();
        this.multiplier = 1.0D;
        this.lastWaveDay = -1L;
        this.gameOver = false;
        this.forcedRadius = -1;
        markDirty();
    }

    // ------------------------------------------------------------ invasoes

    public int getWavesCompleted() {
        return wavesCompleted;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public boolean isWaveActive() {
        return waveActive;
    }

    public int getMobsSpawned() {
        return mobsSpawned;
    }

    /** Invasores vivos agora. */
    public int getMobsAlive() {
        return activeInvaders.size();
    }

    public Set<UUID> getActiveInvaders() {
        return activeInvaders;
    }

    public void addInvader(UUID uuid) {
        activeInvaders.add(uuid);
        markDirty();
    }

    public boolean removeInvader(UUID uuid) {
        boolean removed = activeInvaders.remove(uuid);
        if (removed) {
            markDirty();
        }
        return removed;
    }

    public void startWave(int wave) {
        this.currentWave = wave;
        this.mobsSpawned = 0;
        this.waveActive = true;
        this.spawnTimer = 0;
        this.activeInvaders.clear();
        markDirty();
    }

    public void countSpawn() {
        mobsSpawned++;
        markDirty();
    }

    public void finishWave(boolean completed) {
        if (completed) {
            wavesCompleted++;
        }
        waveActive = false;
        mobsSpawned = 0;
        activeInvaders.clear();
        markDirty();
    }

    public double getMultiplier() {
        return multiplier <= 0 ? 1.0D : multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = Math.max(0.1D, multiplier);
        markDirty();
    }

    public long getLastWaveDay() {
        return lastWaveDay;
    }

    public void setLastWaveDay(long day) {
        this.lastWaveDay = day;
        markDirty();
    }

    public int getSpawnTimer() {
        return spawnTimer;
    }

    public void setSpawnTimer(int spawnTimer) {
        this.spawnTimer = spawnTimer;
    }

    // ------------------------------------------------- blocos dos invasores

    /** Teto de posicoes guardadas, para uma invasao longa nao inchar o save. */
    private static final int MAX_PLACED_BLOCKS = 20000;

    /** Registra um bloco colocado por um invasor, para ser desfeito ao amanhecer. */
    public void addPlacedBlock(BlockPos pos) {
        if (placedBlocks.size() >= MAX_PLACED_BLOCKS) {
            return;
        }
        if (placedBlocks.add(pos.asLong())) {
            markDirty();
        }
    }

    public Set<Long> getPlacedBlocks() {
        return placedBlocks;
    }

    public void clearPlacedBlocks() {
        if (!placedBlocks.isEmpty()) {
            placedBlocks.clear();
            markDirty();
        }
    }

    // ----------------------------------------------------------------- nbt

    public void readNbt(NbtCompound nbt) {
        if (nbt.contains("NexusX")) {
            nexusPos = new BlockPos(nbt.getInt("NexusX"), nbt.getInt("NexusY"), nbt.getInt("NexusZ"));
        } else {
            nexusPos = null;
        }
        nexusHealth = nbt.getInt("NexusHealth");
        nexusMaxHealth = nbt.getInt("NexusMaxHealth");
        wavesCompleted = nbt.getInt("WavesCompleted");
        currentWave = nbt.getInt("CurrentWave");
        waveActive = nbt.getBoolean("WaveActive");
        mobsSpawned = nbt.getInt("MobsSpawned");
        multiplier = nbt.contains("Multiplier") ? nbt.getDouble("Multiplier") : DtbConfig.get().mobMultiplier;
        lastWaveDay = nbt.contains("LastWaveDay") ? nbt.getLong("LastWaveDay") : -1L;
        gameOver = nbt.getBoolean("GameOver");
        forcedRadius = nbt.contains("ForcedRadius") ? nbt.getInt("ForcedRadius") : -1;

        placedBlocks.clear();
        for (long packed : nbt.getLongArray("PlacedBlocks")) {
            placedBlocks.add(packed);
        }

        activeInvaders.clear();
        NbtList list = nbt.getList("ActiveInvaders", NbtElement.STRING_TYPE);
        Set<UUID> parsed = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            try {
                parsed.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
                // uuid corrompido: simplesmente descartamos a entrada
            }
        }
        activeInvaders.addAll(parsed);
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        if (nexusPos != null) {
            nbt.putInt("NexusX", nexusPos.getX());
            nbt.putInt("NexusY", nexusPos.getY());
            nbt.putInt("NexusZ", nexusPos.getZ());
        }
        nbt.putInt("NexusHealth", nexusHealth);
        nbt.putInt("NexusMaxHealth", nexusMaxHealth);
        nbt.putInt("WavesCompleted", wavesCompleted);
        nbt.putInt("CurrentWave", currentWave);
        nbt.putBoolean("WaveActive", waveActive);
        nbt.putInt("MobsSpawned", mobsSpawned);
        nbt.putDouble("Multiplier", multiplier);
        nbt.putLong("LastWaveDay", lastWaveDay);
        nbt.putBoolean("GameOver", gameOver);
        nbt.putInt("ForcedRadius", forcedRadius);

        NbtList list = new NbtList();
        for (UUID uuid : activeInvaders) {
            list.add(NbtString.of(uuid.toString()));
        }
        nbt.put("ActiveInvaders", list);

        long[] packed = new long[placedBlocks.size()];
        int i = 0;
        for (Long value : placedBlocks) {
            packed[i++] = value;
        }
        nbt.putLongArray("PlacedBlocks", packed);
        return nbt;
    }
}
