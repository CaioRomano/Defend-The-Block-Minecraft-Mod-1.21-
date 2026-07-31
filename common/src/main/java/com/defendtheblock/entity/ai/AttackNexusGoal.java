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
 * vivo por perto <b>e ainda nao chegou no Nexus</b> — assim os mobs continuam
 * caçando o jogador/torreta que estiver no caminho. Mas uma vez que o mob
 * esta mesmo dentro do alcance de ataque do Nexus, {@link #canStart()} volta
 * a valer true incondicionalmente: o Nexus e sempre a prioridade final,
 * mesmo que uma torreta ou o jogador estejam bem do lado. Essa goal e
 * instalada com prioridade numerica bem abaixo das goals vanilla de combate
 * (ver {@code InvaderGoals#install}), entao quando ela quer rodar, ela ganha
 * o controle de movimento/olhar de qualquer goal vanilla que tambem queira.
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
        return isAtNexus() || !hasCloseTarget();
    }

    @Override
    public boolean shouldContinue() {
        return canStart();
    }

    /** Ja esta dentro do alcance de golpe do Nexus — nesse caso nada mais tem prioridade. */
    private boolean isAtNexus() {
        return mob.squaredDistanceTo(NexusPathing.center(data.getNexusPos())) <= ATTACK_RANGE * ATTACK_RANGE;
    }

    /**
     * Alvo vivo e perto o bastante para o mob preferir mata-lo primeiro.
     *
     * <p>Na pratica {@code mob.getTarget()} ja e limpo pelo
     * {@code InvaderCombatPriority} sempre que fica mais longe que
     * {@code nexusPriorityEngageRange}, entao esta checagem e mais uma garantia
     * redundante do que o unico portao — mas usa o mesmo raio para nao haver
     * dois numeros diferentes representando a mesma ideia.
     */
    private boolean hasCloseTarget() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double range = DtbConfig.get().nexusPriorityEngageRange;
        return mob.squaredDistanceTo(target) < range * range;
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

        // Uma porta fechada bem na frente sempre vira obstaculo marcado, mesmo
        // que a navegacao vanilla conseguisse abri-la sozinha e passar: o
        // invasor tem que arrombar (BreachObstacleGoal, prioridade mais alta),
        // nunca so atravessar.
        if (data.getObstacle() == null) {
            BlockPos door = NexusPathing.findClosedDoorAhead(mob, nexus);
            if (door != null) {
                data.setObstacle(door);
            }
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
        // O mob guarda a SUA PROPRIA copia da posicao do Nexus, tirada quando
        // foi recrutado. Se o Nexus foi removido/recolocado nesse meio tempo
        // (por exemplo via /dtb removenexus), essa copia fica desatualizada —
        // sem essa checagem, o dano cairia no Nexus atual (estado global) mesmo
        // com o mob fisicamente parado no lugar do Nexus antigo, "atacando o
        // vento" e ainda assim ferindo um Nexus novo em outro lugar.
        if (!nexus.equals(NexusManager.getData(world).getNexusPos())) {
            data.setInvader(false);
            mob.setTarget(null);
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
