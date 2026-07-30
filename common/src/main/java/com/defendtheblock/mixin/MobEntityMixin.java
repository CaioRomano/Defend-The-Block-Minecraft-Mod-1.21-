package com.defendtheblock.mixin;

import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Anexa {@link InvaderData} a todo {@code MobEntity} e persiste no NBT, para
 * que um invasor continue invasor depois de descarregar/recarregar o chunk.
 */
@Mixin(MobEntity.class)
public abstract class MobEntityMixin implements InvaderAccess {

    @Unique
    private final InvaderData defendtheblock$invaderData = new InvaderData();

    @Override
    public InvaderData defendtheblock$getInvaderData() {
        return defendtheblock$invaderData;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void defendtheblock$writeInvader(NbtCompound nbt, CallbackInfo ci) {
        defendtheblock$invaderData.writeNbt(nbt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void defendtheblock$readInvader(NbtCompound nbt, CallbackInfo ci) {
        defendtheblock$invaderData.readNbt(nbt);
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void defendtheblock$tickInvader(CallbackInfo ci) {
        if (defendtheblock$invaderData.isInvader()) {
            defendtheblock$invaderData.tickCooldowns();
        }
    }
}
