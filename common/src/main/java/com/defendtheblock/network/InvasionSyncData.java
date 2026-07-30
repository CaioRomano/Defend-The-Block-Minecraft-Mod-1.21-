package com.defendtheblock.network;

import com.defendtheblock.invasion.InvasionData;
import net.minecraft.network.PacketByteBuf;

/**
 * Instantaneo do estado da invasao enviado do servidor para o HUD do cliente.
 *
 * <p>A serializacao mora aqui (comum as duas versoes); apenas o registro do
 * pacote em si e feito pela camada de compatibilidade, porque 1.20.1 usa
 * {@code Identifier + PacketByteBuf} e 1.21 usa {@code CustomPayload}.
 */
public final class InvasionSyncData {

    public final boolean hasNexus;
    public final int nexusX;
    public final int nexusY;
    public final int nexusZ;
    public final int nexusHealth;
    public final int nexusMaxHealth;
    public final int wavesCompleted;
    public final int currentWave;
    public final boolean waveActive;
    public final int mobsAlive;
    public final int mobsSpawned;
    public final double multiplier;
    public final boolean gameOver;

    public InvasionSyncData(boolean hasNexus, int nexusX, int nexusY, int nexusZ, int nexusHealth,
                            int nexusMaxHealth, int wavesCompleted, int currentWave, boolean waveActive,
                            int mobsAlive, int mobsSpawned, double multiplier, boolean gameOver) {
        this.hasNexus = hasNexus;
        this.nexusX = nexusX;
        this.nexusY = nexusY;
        this.nexusZ = nexusZ;
        this.nexusHealth = nexusHealth;
        this.nexusMaxHealth = nexusMaxHealth;
        this.wavesCompleted = wavesCompleted;
        this.currentWave = currentWave;
        this.waveActive = waveActive;
        this.mobsAlive = mobsAlive;
        this.mobsSpawned = mobsSpawned;
        this.multiplier = multiplier;
        this.gameOver = gameOver;
    }

    public static InvasionSyncData of(InvasionData data) {
        boolean hasNexus = data.hasNexus();
        return new InvasionSyncData(
                hasNexus,
                hasNexus ? data.getNexusPos().getX() : 0,
                hasNexus ? data.getNexusPos().getY() : 0,
                hasNexus ? data.getNexusPos().getZ() : 0,
                data.getNexusHealth(),
                data.getNexusMaxHealth(),
                data.getWavesCompleted(),
                data.getCurrentWave(),
                data.isWaveActive(),
                data.getMobsAlive(),
                data.getMobsSpawned(),
                data.getMultiplier(),
                data.isGameOver());
    }

    public static InvasionSyncData empty() {
        return new InvasionSyncData(false, 0, 0, 0, 0, 0, 0, 0, false, 0, 0, 1.0D, false);
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(hasNexus);
        buf.writeVarInt(nexusX);
        buf.writeVarInt(nexusY);
        buf.writeVarInt(nexusZ);
        buf.writeVarInt(nexusHealth);
        buf.writeVarInt(nexusMaxHealth);
        buf.writeVarInt(wavesCompleted);
        buf.writeVarInt(currentWave);
        buf.writeBoolean(waveActive);
        buf.writeVarInt(mobsAlive);
        buf.writeVarInt(mobsSpawned);
        buf.writeDouble(multiplier);
        buf.writeBoolean(gameOver);
    }

    public static InvasionSyncData read(PacketByteBuf buf) {
        return new InvasionSyncData(
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readDouble(),
                buf.readBoolean());
    }
}
