package com.defendtheblock.entity.invader;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;

/**
 * Implementado em {@code MobEntity} pelo mixin, da acesso aos dados de invasor.
 */
public interface InvaderAccess {

    InvaderData defendtheblock$getInvaderData();

    /** Atalho seguro: devolve null se a entidade nao for um {@link MobEntity}. */
    static InvaderData of(Entity entity) {
        if (entity instanceof InvaderAccess access) {
            return access.defendtheblock$getInvaderData();
        }
        return null;
    }

    /** @return true se a entidade e um mob marcado como invasor. */
    static boolean isInvader(Entity entity) {
        InvaderData data = of(entity);
        return data != null && data.isInvader();
    }
}
