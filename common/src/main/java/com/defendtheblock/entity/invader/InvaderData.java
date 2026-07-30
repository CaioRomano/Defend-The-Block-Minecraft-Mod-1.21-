package com.defendtheblock.entity.invader;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

/**
 * Dados de invasor anexados a cada {@code MobEntity} pelo mixin
 * {@code MobEntityMixin}. Persistido no NBT da propria entidade, para que o mob
 * continue sendo um invasor depois de recarregar o chunk.
 */
public class InvaderData {

    public static final String NBT_KEY = "DtbInvader";

    private boolean invader;
    private int abilityMask;
    private int wave;
    /** true quando o mob faz parte da contagem oficial da onda. */
    private boolean countsForWave;

    private BlockPos nexusPos;

    // ---- estado apenas de runtime (nao persistido)
    private boolean goalsInstalled;
    private BlockPos obstacle;
    private int breachCooldown;
    private int nexusHitCooldown;
    private int stuckTicks;
    private double lastDistanceToNexus = Double.MAX_VALUE;

    public boolean isInvader() {
        return invader;
    }

    public void setInvader(boolean invader) {
        this.invader = invader;
    }

    public boolean hasAbility(InvaderAbility ability) {
        return (abilityMask & ability.bit()) != 0;
    }

    public void addAbility(InvaderAbility ability) {
        this.abilityMask |= ability.bit();
    }

    public int getAbilityMask() {
        return abilityMask;
    }

    public void setAbilityMask(int abilityMask) {
        this.abilityMask = abilityMask;
    }

    public int getWave() {
        return wave;
    }

    public void setWave(int wave) {
        this.wave = wave;
    }

    public boolean countsForWave() {
        return countsForWave;
    }

    public void setCountsForWave(boolean countsForWave) {
        this.countsForWave = countsForWave;
    }

    public BlockPos getNexusPos() {
        return nexusPos;
    }

    public void setNexusPos(BlockPos nexusPos) {
        this.nexusPos = nexusPos == null ? null : nexusPos.toImmutable();
    }

    public boolean areGoalsInstalled() {
        return goalsInstalled;
    }

    public void setGoalsInstalled(boolean goalsInstalled) {
        this.goalsInstalled = goalsInstalled;
    }

    public BlockPos getObstacle() {
        return obstacle;
    }

    public void setObstacle(BlockPos obstacle) {
        this.obstacle = obstacle == null ? null : obstacle.toImmutable();
    }

    public int getBreachCooldown() {
        return breachCooldown;
    }

    public void setBreachCooldown(int breachCooldown) {
        this.breachCooldown = breachCooldown;
    }

    public void tickCooldowns() {
        if (breachCooldown > 0) {
            breachCooldown--;
        }
        if (nexusHitCooldown > 0) {
            nexusHitCooldown--;
        }
    }

    public boolean canHitNexus() {
        return nexusHitCooldown <= 0;
    }

    public void setNexusHitCooldown(int ticks) {
        this.nexusHitCooldown = ticks;
    }

    public int getStuckTicks() {
        return stuckTicks;
    }

    public void setStuckTicks(int stuckTicks) {
        this.stuckTicks = stuckTicks;
    }

    public double getLastDistanceToNexus() {
        return lastDistanceToNexus;
    }

    public void setLastDistanceToNexus(double lastDistanceToNexus) {
        this.lastDistanceToNexus = lastDistanceToNexus;
    }

    public void writeNbt(NbtCompound root) {
        if (!invader) {
            return;
        }
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("Invader", true);
        nbt.putInt("Abilities", abilityMask);
        nbt.putInt("Wave", wave);
        nbt.putBoolean("Counts", countsForWave);
        if (nexusPos != null) {
            nbt.putInt("NexusX", nexusPos.getX());
            nbt.putInt("NexusY", nexusPos.getY());
            nbt.putInt("NexusZ", nexusPos.getZ());
        }
        root.put(NBT_KEY, nbt);
    }

    public void readNbt(NbtCompound root) {
        if (!root.contains(NBT_KEY)) {
            return;
        }
        NbtCompound nbt = root.getCompound(NBT_KEY);
        invader = nbt.getBoolean("Invader");
        abilityMask = nbt.getInt("Abilities");
        wave = nbt.getInt("Wave");
        countsForWave = nbt.getBoolean("Counts");
        if (nbt.contains("NexusX")) {
            nexusPos = new BlockPos(nbt.getInt("NexusX"), nbt.getInt("NexusY"), nbt.getInt("NexusZ"));
        }
    }
}
