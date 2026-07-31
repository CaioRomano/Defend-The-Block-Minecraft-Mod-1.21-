package com.defendtheblock.entity.invader;

import com.defendtheblock.config.DtbConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;

/**
 * Mantem o Nexus como prioridade de movimento do invasor.
 *
 * <p>Sem isso, o alvo de combate do mob (jogador ou torreta) tomaria conta do
 * controle de movimento por completo assim que fosse definido — pelo alcance
 * de perseguicao proprio do mob no caso do jogador (vanilla), ou pelo
 * {@link com.defendtheblock.entity.ai.TargetTurretGoal} no caso da torreta — e
 * so devolveria o controle quando o alvo morresse. Com varias torretas
 * espalhadas no caminho, isso vira uma corrente de perseguicoes: o invasor mata
 * uma, encontra a proxima, mata essa, e so no final tenta chegar ao bloco.
 *
 * <p>A regra e simples: se o alvo atual esta mais longe que
 * {@code nexusPriorityEngageRange}, ele e esquecido a cada tick. Isso nao
 * impede o combate — se o jogador ou a torreta estiver genuinamente no caminho
 * (perto o bastante durante a caminhada ate o Nexus), o alvo permanece e as
 * IAs de combate (vanilla ou nossas) agem normalmente. So evita que o mob saia
 * do caminho para cacar algo distante.
 */
public final class InvaderCombatPriority {

    private InvaderCombatPriority() {
    }

    public static void tick(MobEntity mob, InvaderData data) {
        if (!data.isInvader() || data.getNexusPos() == null) {
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        double range = DtbConfig.get().nexusPriorityEngageRange;
        if (mob.squaredDistanceTo(target) > range * range) {
            mob.setTarget(null);
        }
    }
}
