package com.defendtheblock.entity.invader;

import net.minecraft.entity.Entity;
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

    // Rastreia o "engajamento" contra uma torreta especifica, para que o
    // invasor possa desistir dela depois de um tempo sem progresso (ver
    // InvaderCombatPriority) em vez de ficar preso nela para sempre.
    private Entity engagedTurret;
    private int turretEngageTicks;
    private double lastDistanceToTurret = Double.MAX_VALUE;
    /** Carencia depois de desistir de uma torreta, para nao remirar na mesma na hora. */
    private int turretIgnoreTicks;

    /**
     * Este invasor esta executando uma <b>tarefa</b> que precisa de espaco:
     * minerar um obstaculo ou construir escada/pilar.
     *
     * <p>Os outros invasores leem esta flag (via {@link InvaderAccess}) e se
     * afastam, para nao empurrarem quem esta trabalhando para fora do lugar.
     * Runtime puro: se o mob morre ou o chunk descarrega, a flag vai junto —
     * que e exatamente o desejado, ninguem deve abrir espaco para um
     * trabalhador que nao existe mais.
     */
    private boolean working;

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
        if (turretIgnoreTicks > 0) {
            turretIgnoreTicks--;
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

    public Entity getEngagedTurret() {
        return engagedTurret;
    }

    public int getTurretEngageTicks() {
        return turretEngageTicks;
    }

    public void setTurretEngageTicks(int turretEngageTicks) {
        this.turretEngageTicks = turretEngageTicks;
    }

    public double getLastDistanceToTurret() {
        return lastDistanceToTurret;
    }

    public void setLastDistanceToTurret(double lastDistanceToTurret) {
        this.lastDistanceToTurret = lastDistanceToTurret;
    }

    public void engageTurret(Entity turret, double distance) {
        this.engagedTurret = turret;
        this.turretEngageTicks = 0;
        this.lastDistanceToTurret = distance;
    }

    public void clearTurretEngagement() {
        this.engagedTurret = null;
        this.turretEngageTicks = 0;
        this.lastDistanceToTurret = Double.MAX_VALUE;
    }

    public boolean isWorking() {
        return working;
    }

    public void setWorking(boolean working) {
        this.working = working;
    }

    public int getTurretIgnoreTicks() {
        return turretIgnoreTicks;
    }

    /** Desiste da torreta atual e nao mira em nenhuma outra pelos proximos ticks. */
    public void giveUpOnTurret(int ignoreTicks) {
        clearTurretEngagement();
        this.turretIgnoreTicks = ignoreTicks;
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
