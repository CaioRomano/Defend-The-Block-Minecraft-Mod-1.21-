package com.defendtheblock.entity.ai;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.turret.TurretEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;

import java.util.EnumSet;
import java.util.List;

/**
 * Mira, dispara <b>e escolhe o proprio alvo</b> da torreta.
 *
 * <p>Antes, quem escolhia o alvo era um {@code ActiveTargetGoal} separado no
 * target selector: uma vez que ele pegava um alvo "vivo e dentro do
 * follow range", o vanilla so soltava esse alvo quando ele morria ou saia do
 * follow range (bem maior que o alcance de tiro real da torreta) — nunca
 * porque ficou fora de alcance de tiro, sem visada, ou porque outro mob mais
 * perto passou na frente. Na pratica a torreta "grudava" no primeiro alvo que
 * pegasse e ficava girada pra ele parada, ignorando tudo o mais.
 *
 * <p>Agora essa goal reavalia o alvo periodicamente e so troca quando o atual
 * deixa de servir (fora de alcance, sem linha de visao, ou exatamente no eixo
 * vertical da torreta) ou quando aparece um candidato bem mais perto — assim
 * ela nao fica flutuando entre alvos parecidos a toda hora, mas tambem nao
 * fica presa para sempre num alvo que nao consegue mais atacar.
 */
public class TurretShootGoal extends Goal {

    /** Ticks entre cada reavaliacao do alvo atual. */
    private static final int RESCAN_INTERVAL = 5;
    /**
     * Um candidato so substitui o alvo atual (quando o atual ainda e valido)
     * se estiver pelo menos essa distancia (ao quadrado) mais perto — evita
     * ficar trocando de alvo a toda hora entre dois mobs quase equidistantes.
     */
    private static final double SWITCH_MARGIN_SQ = 4.0D;
    /** Folga vertical da varredura: cobre torres bem altas, o alcance real e so no plano horizontal. */
    private static final double VERTICAL_SCAN_MARGIN = 64.0D;

    private final TurretEntity turret;
    private int rescanTimer;

    public TurretShootGoal(TurretEntity turret) {
        this.turret = turret;
        setControls(EnumSet.of(Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return true;
    }

    @Override
    public boolean shouldContinue() {
        return true;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (--rescanTimer <= 0) {
            rescanTimer = RESCAN_INTERVAL;
            retarget();
        }

        LivingEntity target = turret.getTarget();
        if (target == null || !(turret.getWorld() instanceof ServerWorld world) || !isEngageable(target)) {
            return;
        }

        // So atira quando a besta ja estiver de fato apontada para o alvo: sem
        // isso o tiro saia primeiro e a rotacao seguia depois, visivelmente errado.
        if (!turret.aimAt(target)) {
            return;
        }
        if (turret.getCooldown() > 0 || !turret.hasAmmo()) {
            return;
        }

        turret.setCooldown(turret.getReloadTicks());
        int shots = turret.getMultishot() > 0 ? 3 : 1;
        for (int i = 0; i < shots; i++) {
            fire(world, target, i, shots);
        }
        turret.consumeAmmo();

        world.playSound(null, turret.getBlockPos(), SoundEvents.ITEM_CROSSBOW_SHOOT, SoundCategory.NEUTRAL,
                1.0F, 1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + 0.3F);
    }

    private void retarget() {
        LivingEntity current = turret.getTarget();
        boolean currentOk = isEngageable(current);
        LivingEntity best = findBestTarget();

        if (best == null) {
            if (!currentOk) {
                turret.setTarget(null);
            }
            return;
        }
        if (!currentOk) {
            turret.setTarget(best);
            return;
        }
        if (best != current
                && turret.horizontalSquaredDistanceTo(best) + SWITCH_MARGIN_SQ < turret.horizontalSquaredDistanceTo(current)) {
            turret.setTarget(best);
        }
    }

    /** Alvo valido, dentro de alcance (no plano horizontal), fora do proprio eixo vertical e visivel. */
    private boolean isEngageable(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        double range = turret.getRange();
        return turret.horizontalSquaredDistanceTo(target) <= range * range
                && !turret.isDegenerateAngle(target)
                && turret.getVisibilityCache().canSee(target);
    }

    private LivingEntity findBestTarget() {
        double range = turret.getRange();
        Box box = new Box(turret.getX() - range, turret.getY() - VERTICAL_SCAN_MARGIN, turret.getZ() - range,
                turret.getX() + range, turret.getY() + VERTICAL_SCAN_MARGIN, turret.getZ() + range);
        List<LivingEntity> candidates = turret.getWorld().getEntitiesByClass(LivingEntity.class, box,
                entity -> entity.isAlive() && (entity instanceof Monster || InvaderAccess.isInvader(entity)));

        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            if (!isEngageable(candidate)) {
                continue;
            }
            double distance = turret.horizontalSquaredDistanceTo(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private void fire(ServerWorld world, LivingEntity target, int index, int shots) {
        PersistentProjectileEntity arrow = DtbCompat.createArrow(world, turret, turret.getAmmoStack());
        arrow.setPosition(turret.getX(), turret.getEyeY() + 0.25D, turret.getZ());

        double dx = target.getX() - arrow.getX();
        double dy = target.getBodyY(0.4D) - arrow.getY();
        double dz = target.getZ() - arrow.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        // Multishot abre um leque de 3 flechas, igual a besta do jogador.
        float spread = shots > 1 ? (index - (shots - 1) / 2.0F) * 10.0F : 0.0F;
        arrow.setVelocity(dx, dy + horizontal * 0.06D, dz, 2.6F, spread);

        arrow.setDamage(turret.getArrowDamage());
        arrow.setCritical(true);
        DtbCompat.applyPunch(arrow, turret.getPunch());
        DtbCompat.applyPiercing(arrow, turret.getPiercing());
        if (turret.getFlame() > 0) {
            arrow.setFireTicks(100);
        }
        world.spawnEntity(arrow);
    }
}
