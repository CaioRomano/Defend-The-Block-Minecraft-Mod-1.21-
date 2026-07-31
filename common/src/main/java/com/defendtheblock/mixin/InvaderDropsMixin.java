package com.defendtheblock.mixin;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.invader.InvaderAccess;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Impede que os mobs recrutados pela invasao dropem itens ao morrer.
 *
 * <p>A invasao spawna centenas de mobs por noite. Sem isso, o chao em volta do
 * Nexus vira uma montanha de carne podre, ossos e equipamento — mais lag do
 * que recompensa, e nada disso tem a ver com o desafio. A recompensa da noite
 * vem do saque que o Nexus solta ao amanhecer.
 *
 * <p>Desligavel em {@code invadersDropLoot}. O XP continua caindo normalmente:
 * so os itens sao cortados.
 *
 * <p>Nota de versao: {@code dropLoot(DamageSource, boolean)} tem a mesma
 * assinatura no 1.20.1 e no 1.21.1 (o parametro {@code ServerWorld} so entrou
 * no 1.21.2), entao este mixin pode viver no codigo compartilhado.
 */
@Mixin(LivingEntity.class)
public abstract class InvaderDropsMixin {

    @Inject(method = "dropLoot", at = @At("HEAD"), cancellable = true)
    private void defendtheblock$skipInvaderLoot(DamageSource damageSource, boolean causedByPlayer, CallbackInfo ci) {
        if (!DtbConfig.get().invadersDropLoot && InvaderAccess.isInvader((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
