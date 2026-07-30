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

    private int mobsTotal;
    private int mobsSpawned;
    private final Set<UUID> activeInvaders = new LinkedHashSet<>();

    private double multiplier = 1.0D;
    private long lastWaveDay = -1L;
    private boolean gameOver;

    /** Raio (em chunks) atualmente mantido carregado, ou -1 se nenhum. */
    private int forcedRadius = -1;

    /** Contador ate o proximo lote de spawn. Nao precisa ser persistido, mas e barato. */
    private int spawnTimer;

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

    public int getMobsTotal() {
        return mobsTotal;
    }

    public int getMobsSpawned() {
        return mobsSpawned;
    }

    /** Mobs que ainda faltam ser derrotados: os vivos mais os que faltam nascer. */
    public int getMobsRemaining() {
        return activeInvaders.size() + Math.max(0, mobsTotal - mobsSpawned);
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

    public void startWave(int wave, int total) {
        this.currentWave = wave;
        this.mobsTotal = total;
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
        mobsTotal = 0;
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
        mobsTotal = nbt.getInt("MobsTotal");
        mobsSpawned = nbt.getInt("MobsSpawned");
        multiplier = nbt.contains("Multiplier") ? nbt.getDouble("Multiplier") : DtbConfig.get().mobMultiplier;
        lastWaveDay = nbt.contains("LastWaveDay") ? nbt.getLong("LastWaveDay") : -1L;
        gameOver = nbt.getBoolean("GameOver");
        forcedRadius = nbt.contains("ForcedRadius") ? nbt.getInt("ForcedRadius") : -1;

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
        nbt.putInt("MobsTotal", mobsTotal);
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
        return nbt;
    }
}
