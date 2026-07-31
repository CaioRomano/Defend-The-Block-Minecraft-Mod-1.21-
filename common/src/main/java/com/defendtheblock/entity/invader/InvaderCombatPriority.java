package com.defendtheblock.entity.invader;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.turret.TurretEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
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
 * <p>A regra e simples: se o alvo atual esta mais longe que o alcance de
 * engajamento, ele e esquecido a cada tick. Isso nao impede o combate — se o
 * jogador ou a torreta estiver genuinamente no caminho, o alvo permanece e as
 * IAs de combate agem normalmente. So evita que o mob saia do caminho para
 * cacar algo distante.
 *
 * <p>O alcance depende de quem esta mirando em que:
 * <ul>
 *   <li>mob <b>corpo a corpo</b>, qualquer alvo: {@code nexusPriorityEngageRange}
 *       (6 blocos) — ele precisa chegar perto para fazer qualquer coisa, entao
 *       um alvo distante so o tiraria do caminho;</li>
 *   <li>mob <b>de ataque a distancia</b> (esqueleto e afins) mirando uma
 *       <b>torreta</b>: {@code rangedTurretPriorityRange} (20 blocos) — ele
 *       atira de onde esta, sem sair do lugar, entao faz sentido deixar
 *       priorizar a torreta de longe (ver {@code TargetTurretGoal}).</li>
 * </ul>
 *
 * <p>Em nenhum dos casos o foco e eterno: passado
 * {@code maxTurretEngageTicks} engajado numa torreta — ou, para os de corpo a
 * corpo, ficando sem progresso em direcao a ela — o mob desiste, ganha um
 * periodo de carencia e volta a marchar para o Nexus.
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

        if (target instanceof TurretEntity turret) {
            tickTurretEngagement(mob, data, turret);
            return;
        }

        data.clearTurretEngagement();
        double range = DtbConfig.get().nexusPriorityEngageRange;
        if (mob.squaredDistanceTo(target) > range * range) {
            mob.setTarget(null);
        }
    }

    private static void tickTurretEngagement(MobEntity mob, InvaderData data, TurretEntity turret) {
        DtbConfig config = DtbConfig.get();
        boolean ranged = mob instanceof RangedAttackMob;
        double range = ranged ? config.rangedTurretPriorityRange : config.nexusPriorityEngageRange;
        double distance = mob.squaredDistanceTo(turret);

        if (distance > range * range) {
            mob.setTarget(null);
            data.clearTurretEngagement();
            return;
        }

        if (data.getEngagedTurret() != turret) {
            data.engageTurret(turret, distance);
            return;
        }

        int ticks = data.getTurretEngageTicks() + 1;
        data.setTurretEngageTicks(ticks);

        // Teto absoluto de tempo grudado numa torreta, valendo para todo mundo:
        // o proposito do invasor continua sendo o Nexus.
        if (ticks >= config.maxTurretEngageTicks) {
            mob.setTarget(null);
            data.giveUpOnTurret(config.turretIgnoreTicksAfterGiveUp);
            return;
        }

        // O de longe atira parado, entao "nao chegar mais perto" e o esperado —
        // so o de corpo a corpo desiste por falta de progresso.
        if (ranged || ticks < TURRET_STUCK_TIMEOUT) {
            return;
        }
        if (distance > data.getLastDistanceToTurret() - PROGRESS_EPSILON) {
            // Sem progresso real em direcao a torreta por um tempo bom: o
            // pathfinding nao esta dando conta (provavelmente ela esta fora
            // de alcance real, num pilar ou pontilhao). Desiste e volta o
            // foco para o Nexus.
            mob.setTarget(null);
            data.giveUpOnTurret(config.turretIgnoreTicksAfterGiveUp);
            return;
        }
        data.setTurretEngageTicks(0);
        data.setLastDistanceToTurret(distance);
    }
}
