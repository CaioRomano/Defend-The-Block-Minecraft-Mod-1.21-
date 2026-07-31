package com.defendtheblock.entity.invader;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.turret.TurretEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;

/**
 * Mantem o Nexus como prioridade de movimento do invasor.
 *
 * <p>Sem isso, o alvo de combate do mob (jogador ou torreta) tomaria conta do
 * controle de movimento por completo assim que fosse definido, e so devolveria
 * o controle quando o alvo morresse. Com varias torretas espalhadas no
 * caminho, isso viraria uma corrente de perseguicoes: o invasor mata uma,
 * encontra a proxima, mata essa, e so no final tenta chegar ao bloco.
 *
 * <p>A regra e simples: se o alvo atual esta mais longe que
 * {@code nexusPriorityEngageRange}, ele e esquecido a cada tick. Isso nao
 * impede o combate — se o jogador ou a torreta estiver genuinamente no caminho
 * (perto o bastante durante a caminhada ate o Nexus), o alvo permanece e as
 * IAs de combate (vanilla ou nossas) agem normalmente. So evita que o mob saia
 * do caminho para cacar algo distante.
 *
 * <p>Uma torreta so vira alvo por meio do {@code RevengeGoal} nativo do
 * vanilla — ou seja, so depois de acertar o mob primeiro (ver
 * {@link InvaderGoals#install}, que nao instala mais nenhuma goal de deteccao
 * a distancia). Mesmo assim, o foco nela nunca e permanente: se o mob passa
 * {@link #TURRET_STUCK_TIMEOUT} ticks engajado nela sem reduzir a distancia
 * (sinal de que o pathfinding nao consegue realmente alcanca-la — por
 * exemplo, uma torreta num terreno elevado), o alvo e liberado e o mob volta a
 * perseguir o Nexus.
 */
public final class InvaderCombatPriority {

    private static final int TURRET_STUCK_TIMEOUT = 60;
    private static final double PROGRESS_EPSILON = 0.3D;

    private InvaderCombatPriority() {
    }

    public static void tick(MobEntity mob, InvaderData data) {
        if (!data.isInvader() || data.getNexusPos() == null) {
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target == null) {
            data.clearTurretEngagement();
            return;
        }
        double range = DtbConfig.get().nexusPriorityEngageRange;
        if (mob.squaredDistanceTo(target) > range * range) {
            mob.setTarget(null);
            data.clearTurretEngagement();
            return;
        }

        if (target instanceof TurretEntity turret) {
            tickTurretEngagement(mob, data, turret);
        } else {
            data.clearTurretEngagement();
        }
    }

    private static void tickTurretEngagement(MobEntity mob, InvaderData data, TurretEntity turret) {
        double distance = mob.squaredDistanceTo(turret);

        if (data.getEngagedTurret() != turret) {
            data.engageTurret(turret, distance);
            return;
        }

        int ticks = data.getTurretEngageTicks() + 1;
        if (ticks < TURRET_STUCK_TIMEOUT) {
            data.setTurretEngageTicks(ticks);
            return;
        }

        if (distance > data.getLastDistanceToTurret() - PROGRESS_EPSILON) {
            // Sem progresso real em direcao a torreta por um tempo bom: o
            // pathfinding nao esta dando conta (provavelmente ela esta fora
            // de alcance real, num pilar ou pontilhao). Desiste e volta o
            // foco para o Nexus.
            mob.setTarget(null);
            data.clearTurretEngagement();
            return;
        }
        data.setTurretEngageTicks(0);
        data.setLastDistanceToTurret(distance);
    }
}
