package com.defendtheblock.entity.ai;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;

/**
 * Abre espaco para o invasor que esta trabalhando.
 *
 * <p>Cavar uma parede ou montar uma coluna de escada exige o mob parado num
 * ponto exato. Com a horda inteira empurrando por tras, o trabalhador era
 * deslocado do lugar e a tarefa nunca terminava — ou pior, ele ficava
 * oscilando entre "chegar" e "ser empurrado", sem nunca completar o servico.
 *
 * <p>Entao quem <b>nao</b> esta trabalhando recua para fora de um raio minimo
 * em volta de quem esta ({@code workerClearanceRadius}), e volta a avancar
 * assim que a tarefa termina — a flag some sozinha quando o trabalhador
 * conclui, desiste ou morre (ver {@link InvaderData#isWorking()}).
 *
 * <p><b>Excecao importante:</b> se o mob ja esta em alcance de golpe do Nexus,
 * ele nunca recua. Chegar no bloco e o objetivo final de toda a invasao, e
 * ninguem deve abrir mao disso para dar passagem a um colega.
 */
public class YieldToWorkerGoal extends Goal {

    /** Ticks entre varreduras por trabalhadores por perto. */
    private static final int SCAN_INTERVAL = 10;
    /** Teto de tempo recuando, para nunca virar um mob parado para sempre. */
    private static final int MAX_YIELD_TICKS = 200;

    private final MobEntity mob;
    private final InvaderData data;
    private final double speed;

    private MobEntity worker;
    private int scanCooldown;
    private int yieldTicks;
    private int repathTimer;

    public YieldToWorkerGoal(MobEntity mob, double speed) {
        this.mob = mob;
        this.data = InvaderAccess.of(mob);
        this.speed = speed;
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (data == null || !data.isInvader() || data.isWorking()) {
            return false;
        }
        if (scanCooldown-- > 0) {
            return false;
        }
        scanCooldown = SCAN_INTERVAL;

        // Ja da para bater no Nexus: nada justifica recuar agora.
        if (isAtNexus()) {
            return false;
        }
        worker = findWorkerTooClose();
        return worker != null;
    }

    @Override
    public boolean shouldContinue() {
        if (worker == null || !worker.isAlive() || yieldTicks >= MAX_YIELD_TICKS) {
            return false;
        }
        if (data.isWorking() || isAtNexus() || !isWorking(worker)) {
            return false;
        }
        double clearance = clearance();
        return mob.squaredDistanceTo(worker) < clearance * clearance;
    }

    @Override
    public void start() {
        yieldTicks = 0;
        repathTimer = 0;
    }

    @Override
    public void stop() {
        worker = null;
        mob.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (worker == null) {
            return;
        }
        yieldTicks++;
        if (--repathTimer > 0) {
            return;
        }
        repathTimer = 10;

        // Anda para o lado oposto ao trabalhador, o suficiente para sair do
        // raio de trabalho dele.
        Vec3d away = mob.getPos().subtract(worker.getPos());
        if (away.lengthSquared() < 1.0E-4D) {
            // Exatamente em cima: escolhe uma direcao qualquer para desempatar.
            away = new Vec3d(1.0D, 0.0D, 0.0D);
        }
        Vec3d destination = mob.getPos().add(away.normalize().multiply(clearance()));
        mob.getNavigation().startMovingTo(destination.x, destination.y, destination.z, speed);
    }

    private double clearance() {
        return Math.max(1.0D, DtbConfig.get().workerClearanceRadius);
    }

    private boolean isAtNexus() {
        if (data.getNexusPos() == null) {
            return false;
        }
        return mob.squaredDistanceTo(NexusPathing.center(data.getNexusPos()))
                <= AttackNexusGoal.ATTACK_RANGE * AttackNexusGoal.ATTACK_RANGE;
    }

    private static boolean isWorking(MobEntity candidate) {
        InvaderData other = InvaderAccess.of(candidate);
        return other != null && other.isInvader() && other.isWorking();
    }

    /** O trabalhador mais proximo dentro do raio que precisa ser respeitado. */
    private MobEntity findWorkerTooClose() {
        double radius = clearance();
        Box box = mob.getBoundingBox().expand(radius);
        List<MobEntity> nearby = mob.getWorld().getEntitiesByClass(MobEntity.class, box,
                candidate -> candidate != mob && candidate.isAlive() && isWorking(candidate));

        MobEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (MobEntity candidate : nearby) {
            double distance = mob.squaredDistanceTo(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }
}
