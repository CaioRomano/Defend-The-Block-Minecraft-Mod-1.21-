package com.defendtheblock.entity.ai;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import com.defendtheblock.invasion.NexusManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/**
 * Goal principal do invasor: caminhar ate o Nexus e bater nele.
 *
 * <p>Cede a vez para as IAs vanilla de combate sempre que o mob tem um alvo
 * vivo por perto, entao os mobs continuam caçando o jogador normalmente e so
 * voltam para o Nexus quando ninguem esta por perto.
 */
public class AttackNexusGoal extends Goal {

    private static final double ATTACK_RANGE = 2.8D;
    private static final int REPATH_INTERVAL = 20;
    private static final int STUCK_CHECK_INTERVAL = 40;

    private final MobEntity mob;
    private final InvaderData data;
    private final double speed;

    private int repathTimer;
    private int stuckTimer;

    public AttackNexusGoal(MobEntity mob, double speed) {
        this.mob = mob;
        this.data = InvaderAccess.of(mob);
        this.speed = speed;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (data == null || !data.isInvader() || data.getNexusPos() == null) {
            return false;
        }
        if (!(mob.getWorld() instanceof ServerWorld world) || NexusManager.getData(world).isGameOver()) {
            return false;
        }
        return !hasCloseTarget();
    }

    @Override
    public boolean shouldContinue() {
        return canStart();
    }

    /** Alvo vivo e perto o bastante para o mob preferir mata-lo primeiro. */
    private boolean hasCloseTarget() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.squaredDistanceTo(target) < 256.0D;
    }

    @Override
    public void start() {
        repathTimer = 0;
        stuckTimer = 0;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        BlockPos nexus = data.getNexusPos();
        Vec3d center = NexusPathing.center(nexus);
        double distance = Math.sqrt(mob.squaredDistanceTo(center));

        mob.getLookControl().lookAt(center.x, center.y, center.z);

        if (distance <= ATTACK_RANGE) {
            mob.getNavigation().stop();
            attackNexus(nexus);
            return;
        }

        if (--repathTimer <= 0) {
            repathTimer = REPATH_INTERVAL;
            boolean moving = mob.getNavigation().startMovingTo(center.x, center.y, center.z, speed);
            if (!moving) {
                // Sem caminho: voadores usam o move control direto, os de chao
                // marcam o obstaculo para as IAs de arrombamento.
                mob.getMoveControl().moveTo(center.x, center.y, center.z, speed);
                markBlocked(nexus);
            }
        }

        if (++stuckTimer >= STUCK_CHECK_INTERVAL) {
            stuckTimer = 0;
            double previous = data.getLastDistanceToNexus();
            if (distance > previous - 0.75D) {
                markBlocked(nexus);
                // Sinal compartilhado com BridgeToNexusGoal: quantos ciclos seguidos
                // o mob nao avancou. Um Nexus suspenso no ar e o caso tipico.
                data.setStuckTicks(data.getStuckTicks() + 1);
            } else {
                data.setObstacle(null);
                data.setStuckTicks(0);
            }
            data.setLastDistanceToNexus(distance);
        }
    }

    private void markBlocked(BlockPos nexus) {
        BlockPos obstacle = NexusPathing.findObstacle(mob, nexus);
        if (obstacle != null) {
            data.setObstacle(obstacle);
        }
    }

    private void attackNexus(BlockPos nexus) {
        if (!data.canHitNexus() || !(mob.getWorld() instanceof ServerWorld world)) {
            return;
        }
        DtbConfig config = DtbConfig.get();
        data.setNexusHitCooldown(config.nexusHitCooldown);
        data.setObstacle(null);
        data.setStuckTicks(0);

        if (mob instanceof CreeperEntity creeper) {
            // O creeper termina a viagem se explodindo em cima do Nexus.
            creeper.ignite();
            NexusManager.damage(world, config.nexusDamagePerHit * 8, mob);
            return;
        }

        mob.swingHand(Hand.MAIN_HAND);
        int damage = config.nexusDamagePerHit + data.getWave() / 4;
        world.playSound(null, nexus, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.HOSTILE, 0.8F, 1.0F);
        NexusManager.damage(world, damage, mob);
    }
}
