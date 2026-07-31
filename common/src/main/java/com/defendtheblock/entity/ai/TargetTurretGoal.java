package com.defendtheblock.entity.ai;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.turret.TurretEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Box;

import java.util.EnumSet;
import java.util.List;

/**
 * Faz o invasor enxergar a torreta como um alvo valido a distancia, do mesmo
 * jeito que enxergaria um jogador — em vez de so reagir depois de ser
 * atingido por uma flecha dela.
 *
 * <p>O {@code ActiveTargetGoal} vanilla usa o proprio alcance de perseguicao
 * do mob (geralmente 16 blocos) para procurar alvos, o que e menor que a
 * distancia normal de spawn dos invasores. Este goal ignora esse atributo e
 * varre um raio fixo e generoso em volta do mob, independente do tipo.
 *
 * <p>Nao reivindica nenhum {@link Control}: ele so define {@code mob.setTarget}
 * como efeito colateral de {@link #canStart()} e nunca "roda" de verdade, entao
 * nunca disputa controle com nenhuma outra goal. Uma vez definido o alvo, as
 * IAs de combate vanilla (corpo a corpo ou de arco) cuidam do resto sozinhas,
 * porque elas atacam o que estiver em {@code mob.getTarget()}, seja jogador ou
 * torreta.
 */
public class TargetTurretGoal extends Goal {

    private static final int SCAN_INTERVAL = 20;

    private final MobEntity mob;
    private int cooldown;

    public TargetTurretGoal(MobEntity mob) {
        this.mob = mob;
        setControls(EnumSet.noneOf(Control.class));
    }

    @Override
    public boolean canStart() {
        if (cooldown-- > 0) {
            return false;
        }
        cooldown = SCAN_INTERVAL;

        // Ja tem um alvo vivo (jogador, por exemplo): nao atropela.
        if (mob.getTarget() != null && mob.getTarget().isAlive()) {
            return false;
        }

        TurretEntity turret = findNearestTurret();
        if (turret != null) {
            mob.setTarget(turret);
        }
        return false;
    }

    private TurretEntity findNearestTurret() {
        double radius = DtbConfig.get().turretDetectionRadius;
        Box box = mob.getBoundingBox().expand(radius);
        List<TurretEntity> turrets = mob.getWorld().getEntitiesByClass(TurretEntity.class, box, TurretEntity::isAlive);

        TurretEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (TurretEntity turret : turrets) {
            if (!mob.getVisibilityCache().canSee(turret)) {
                continue;
            }
            double distance = mob.squaredDistanceTo(turret);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = turret;
            }
        }
        return best;
    }
}
