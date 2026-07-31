package com.defendtheblock.mixin;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import com.defendtheblock.invasion.InvasionData;
import com.defendtheblock.invasion.NexusManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Faz a flecha de um invasor (esqueleto, principalmente) causar dano ao Nexus
 * quando acerta o proprio bloco, em vez do dano vir so do golpe corpo a corpo
 * do {@code AttackNexusGoal}.
 *
 * <p>Mixina em {@code ProjectileEntity} (a classe base de todo projetil, o
 * mesmo lugar de onde {@code WebShotEntity} ja sobrescreve {@code onBlockHit}
 * com sucesso comprovado) em vez de {@code PersistentProjectileEntity}, porque
 * assim funciona independente de qual subclasse concreta (flecha normal,
 * espectral) realmente declara a colisao. O filtro
 * {@code instanceof PersistentProjectileEntity} logo na entrada restringe o
 * efeito so a flechas de verdade, deixando de fora a teia da aranha.
 */
@Mixin(ProjectileEntity.class)
public abstract class ArrowNexusDamageMixin {

    @Inject(method = "onBlockHit", at = @At("HEAD"))
    private void defendtheblock$damageNexus(BlockHitResult hitResult, CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity) (Object) this;
        if (!(self instanceof PersistentProjectileEntity) || !(self.getWorld() instanceof ServerWorld world)) {
            return;
        }

        Entity owner = self.getOwner();
        if (!(owner instanceof MobEntity mobOwner)) {
            return;
        }
        InvaderData invaderData = InvaderAccess.of(mobOwner);
        if (invaderData == null || !invaderData.isInvader()) {
            return;
        }

        InvasionData data = NexusManager.getData(world);
        if (!data.hasNexus() || data.isGameOver() || !hitResult.getBlockPos().equals(data.getNexusPos())) {
            return;
        }

        int damage = DtbConfig.get().nexusDamagePerHit + invaderData.getWave() / 4;
        NexusManager.damage(world, damage, mobOwner);
    }
}
