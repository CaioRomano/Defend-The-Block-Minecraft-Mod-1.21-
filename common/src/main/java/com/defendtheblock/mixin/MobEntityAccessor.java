package com.defendtheblock.mixin;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Da acesso aos seletores de goal para podermos injetar as IAs de invasao em
 * mobs vanilla (zumbi, esqueleto, creeper, aranha, blaze...).
 */
@Mixin(MobEntity.class)
public interface MobEntityAccessor {

    @Accessor("goalSelector")
    GoalSelector defendtheblock$getGoalSelector();

    @Accessor("targetSelector")
    GoalSelector defendtheblock$getTargetSelector();
}
