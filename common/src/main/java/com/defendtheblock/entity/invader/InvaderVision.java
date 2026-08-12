package com.defendtheblock.entity.invader;

import com.defendtheblock.config.DtbConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Campo de visao do invasor.
 *
 * <p>O vanilla so oferece {@code getVisibilityCache().canSee(...)}, que e
 * <b>linha de visada</b>: responde "existe caminho livre entre os dois?", e
 * nao "o mob esta olhando para la?". Sozinho, ele deixa o mob mirar em algo
 * exatamente atras da propria nuca.
 *
 * <p>Aqui entra o cone: o alvo so conta se estiver dentro de
 * {@code invaderFieldOfViewDegrees} em volta do rumo em que a cabeca do mob
 * esta virada. Combinar os dois — cone <i>e</i> linha de visada — e o que faz
 * "so ataca o que ele enxerga" significar de verdade o que a frase diz.
 */
public final class InvaderVision {

    private InvaderVision() {
    }

    /** O alvo esta dentro do cone de visao <b>e</b> com linha de visada livre? */
    public static boolean sees(MobEntity mob, Entity target) {
        return isInFieldOfView(mob, target) && mob.getVisibilityCache().canSee(target);
    }

    /**
     * So o cone, sem a linha de visada.
     *
     * <p>O calculo e feito no plano horizontal: um mob no chao olhando para
     * frente enxerga uma torreta no alto de uma torre a sua frente, o que e o
     * comportamento esperado. Restringir tambem na vertical faria a torre
     * virar um ponto cego gratuito.
     */
    public static boolean isInFieldOfView(MobEntity mob, Entity target) {
        Vec3d toTarget = target.getPos().subtract(mob.getPos());
        Vec3d flat = new Vec3d(toTarget.x, 0.0D, toTarget.z);
        if (flat.lengthSquared() < 1.0E-4D) {
            // Praticamente em cima do mob: considera visto, senao um alvo
            // colado nele viraria ponto cego.
            return true;
        }

        Vec3d facing = Vec3d.fromPolar(0.0F, mob.getHeadYaw());
        double cosAngle = facing.normalize().dotProduct(flat.normalize());
        double halfAngle = Math.toRadians(DtbConfig.get().invaderFieldOfViewDegrees / 2.0D);
        return cosAngle >= Math.cos(halfAngle);
    }
}
