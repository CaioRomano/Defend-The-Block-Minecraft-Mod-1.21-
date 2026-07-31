package com.defendtheblock.entity.ai;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import com.defendtheblock.entity.turret.TurretEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Box;

import java.util.EnumSet;
import java.util.List;

/**
 * Faz um invasor <b>de ataque a distancia</b> (esqueleto e afins) priorizar a
 * destruicao das torretas.
 *
 * <p>Uma versao anterior desta goal existia para <em>todo</em> invasor e foi
 * removida porque travava a horda inteira: mobs corpo a corpo saiam
 * perseguindo torretas que muitas vezes nem conseguiam alcancar, e nunca
 * voltavam para o Nexus. Aqui ela volta restrita a quem ataca de longe, onde
 * o problema nao existe — o esqueleto atira de onde esta, sem precisar de um
 * caminho ate a torreta, entao priorizar a torreta nao tira ele do lugar nem
 * o impede de seguir para o Nexus depois.
 *
 * <p>A trava de tempo continua valendo: quem cuida de nao deixar o foco virar
 * eterno e o {@code InvaderCombatPriority}, que solta a torreta depois de
 * {@code maxTurretEngageTicks} e ainda coloca um periodo de carencia antes de
 * poder mirar em outra.
 */
public class TargetTurretGoal extends Goal {

    private static final int SCAN_INTERVAL = 20;

    private final MobEntity mob;
    private final InvaderData data;
    private int cooldown;

    public TargetTurretGoal(MobEntity mob) {
        this.mob = mob;
        this.data = InvaderAccess.of(mob);
        // Nao toma nenhum Control: so define o alvo e deixa as goals de
        // combate a distancia do proprio vanilla cuidarem do tiro.
        setControls(EnumSet.noneOf(Control.class));
    }

    @Override
    public boolean canStart() {
        if (data == null || !data.isInvader()) {
            return false;
        }
        if (cooldown-- > 0) {
            return false;
        }
        cooldown = SCAN_INTERVAL;

        // Respeita a carencia imposta depois de desistir de uma torreta.
        if (data.getTurretIgnoreTicks() > 0) {
            return false;
        }
        // Ja esta ocupado com um alvo vivo: nao rouba o foco.
        if (mob.getTarget() != null && mob.getTarget().isAlive()) {
            return false;
        }

        TurretEntity turret = findNearestTurret();
        if (turret != null) {
            mob.setTarget(turret);
        }
        // Nunca "roda" de verdade: o trabalho todo e escolher o alvo.
        return false;
    }

    private TurretEntity findNearestTurret() {
        double radius = DtbConfig.get().rangedTurretPriorityRange;
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
