package com.defendtheblock.entity.ai;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.entity.turret.TurretEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.EnumSet;

/**
 * Mira e dispara da torreta. Ela roda para o alvo, respeita o tempo de recarga
 * do nivel atual e aplica os encantamentos que recebeu.
 */
public class TurretShootGoal extends Goal {

    private final TurretEntity turret;

    public TurretShootGoal(TurretEntity turret) {
        this.turret = turret;
        setControls(EnumSet.of(Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = turret.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double range = turret.getRange();
        return turret.squaredDistanceTo(target) <= range * range;
    }

    @Override
    public boolean shouldContinue() {
        return canStart();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = turret.getTarget();
        if (target == null || !(turret.getWorld() instanceof ServerWorld world)) {
            return;
        }

        turret.aimAt(target);

        if (turret.getCooldown() > 0 || !turret.hasAmmo()) {
            return;
        }
        if (!turret.getVisibilityCache().canSee(target)) {
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
        if (turret.getPunch() > 0) {
            arrow.setPunch(turret.getPunch());
        }
        if (turret.getPiercing() > 0) {
            arrow.setPierceLevel((byte) turret.getPiercing());
        }
        if (turret.getFlame() > 0) {
            arrow.setFireTicks(100);
        }
        world.spawnEntity(arrow);
    }
}
