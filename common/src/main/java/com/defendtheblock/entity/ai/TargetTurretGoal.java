package com.defendtheblock.entity.ai;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import com.defendtheblock.entity.invader.InvaderVision;
import com.defendtheblock.entity.turret.TurretEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Box;

import java.util.EnumSet;
import java.util.List;

/**
 * Faz o invasor tratar a torreta como alvo — mas <b>so o que ele enxerga</b>.
 *
 * <p>Toda a historia desta goal e sobre nao deixar o combate roubar a invasao:
 *
 * <ul>
 *   <li>a primeira versao varria 64 blocos e forcava o alvo para a torreta mais
 *       proxima <i>visivel por linha de visada</i>. A horda inteira ficava
 *       grudada nas torretas e nunca voltava ao Nexus;</li>
 *   <li>a segunda foi removida por completo, e a torreta so virava alvo por
 *       revide (o {@code RevengeGoal} do vanilla);</li>
 *   <li>a terceira voltou restrita a quem ataca a distancia, que nao precisa
 *       sair do lugar para atirar.</li>
 * </ul>
 *
 * <p>Agora ela vale para <b>todo invasor</b>, e o que segura o comportamento e
 * o <b>campo de visao</b> ({@link InvaderVision}): se a torreta nao esta no
 * cone que o mob esta olhando, ele nem considera — segue para o Nexus. Isso e
 * bem mais restritivo que a linha de visada sozinha, que era justamente o que
 * deixava a primeira versao tao agressiva.
 *
 * <p>O alcance depende de como o mob luta: quem atira usa
 * {@code rangedTurretPriorityRange} (ele resolve de longe, parado); quem e
 * corpo a corpo usa {@code nexusPriorityEngageRange}, o mesmo raio curto que o
 * {@code InvaderCombatPriority} respeita — sem isso ele escolheria um alvo
 * distante que seria descartado no tick seguinte.
 */
public class TargetTurretGoal extends Goal {

    private static final int SCAN_INTERVAL = 20;

    private final MobEntity mob;
    private final InvaderData data;
    private int cooldown;

    public TargetTurretGoal(MobEntity mob) {
        this.mob = mob;
        this.data = InvaderAccess.of(mob);
        // Nao toma nenhum Control: so define o alvo e deixa as IAs de combate
        // do proprio vanilla cuidarem do ataque.
        setControls(EnumSet.noneOf(Control.class));
    }

    private double range() {
        DtbConfig config = DtbConfig.get();
        return mob instanceof RangedAttackMob
                ? config.rangedTurretPriorityRange
                : config.nexusPriorityEngageRange;
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

        TurretEntity turret = findVisibleTurret();
        if (turret != null) {
            mob.setTarget(turret);
        }
        // Nunca "roda" de verdade: o trabalho todo e escolher o alvo.
        return false;
    }

    private TurretEntity findVisibleTurret() {
        double radius = range();
        Box box = mob.getBoundingBox().expand(radius);
        List<TurretEntity> turrets = mob.getWorld().getEntitiesByClass(TurretEntity.class, box, TurretEntity::isAlive);

        TurretEntity best = null;
        double bestDistance = radius * radius;
        for (TurretEntity turret : turrets) {
            // Cone de visao + linha de visada: fora disso o mob nem sabe que
            // a torreta existe, e segue marchando para o Nexus.
            if (!InvaderVision.sees(mob, turret)) {
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
