package com.defendtheblock.mixin;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.invader.InvaderAccess;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Corta a loot table dos invasores — versao <b>1.21.1</b>.
 *
 * <p>Por que este arquivo vive no diretorio da versao, e nao em {@code common/}:
 * a familia de metodos de drop ganhou um {@code ServerWorld} como primeiro
 * parametro no 1.21. Isso foi confirmado na pratica — uma versao anterior
 * assumiu a assinatura do 1.20.1 para {@code dropEquipment} e o Mixin
 * derrubou o jogo no boot dizendo exatamente qual descritor ele esperava:
 * {@code (ServerWorld, DamageSource, boolean)}. {@code dropLoot} e chamado
 * lado a lado com ele em {@code LivingEntity#drop} e foi refatorado junto.
 *
 * <p>Ele tambem mora num config proprio marcado como {@code "required": false},
 * de modo que um erro de assinatura aqui vira um aviso no log em vez de um
 * crash: o mod continua funcionando, so os itens da loot table e que voltariam
 * a cair.
 *
 * <p>O equipamento (armadura/arma) e tratado sem mixin nenhum, por
 * {@code InvaderEquipment#dropChance}.
 */
@Mixin(LivingEntity.class)
public abstract class InvaderDropsMixin {

    @Inject(method = "dropLoot", at = @At("HEAD"), cancellable = true)
    private void defendtheblock$skipInvaderLoot(ServerWorld world, DamageSource damageSource,
                                                boolean causedByPlayer, CallbackInfo ci) {
        if (!DtbConfig.get().invadersDropLoot && InvaderAccess.isInvader((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
