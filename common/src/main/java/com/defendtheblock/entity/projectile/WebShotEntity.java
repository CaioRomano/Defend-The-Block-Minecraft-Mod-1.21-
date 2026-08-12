package com.defendtheblock.entity.projectile;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.invasion.InvaderBlocks;
import com.defendtheblock.registry.ModEntities;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * A teia cuspida pelas aranhas invasoras: prende quem for atingido e deixa uma
 * teia no chao onde bate.
 */
public class WebShotEntity extends ThrownItemEntity {

    public WebShotEntity(EntityType<? extends WebShotEntity> type, World world) {
        super(type, world);
    }

    public WebShotEntity(World world, LivingEntity owner) {
        super(ModEntities.WEB_SHOT, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBWEB;
    }

    @Override
    protected void onEntityHit(EntityHitResult hitResult) {
        super.onEntityHit(hitResult);
        Entity hit = hitResult.getEntity();
        if (hit instanceof LivingEntity living && living != getOwner()) {
            DtbCompat.applySlowness(living, 120, 2);
            living.damage(getDamageSources().thrown(this, getOwner()), 1.0F);
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult hitResult) {
        super.onBlockHit(hitResult);
        placeWeb(hitResult.getBlockPos().offset(hitResult.getSide()));
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (getWorld() instanceof ServerWorld world) {
            world.playSound(null, getBlockPos(), SoundEvents.ENTITY_SPIDER_HURT, SoundCategory.HOSTILE, 0.7F, 1.6F);
            discard();
        }
    }

    private void placeWeb(BlockPos pos) {
        if (!(getWorld() instanceof ServerWorld world)) {
            return;
        }
        // Via InvaderBlocks: a teia entra na lista de faxina do amanhecer,
        // senao cada noite deixaria o terreno coberto de teia para sempre.
        if (world.getBlockState(pos).isReplaceable()) {
            InvaderBlocks.place(world, pos, Blocks.COBWEB.getDefaultState());
            return;
        }
        // Se o ponto exato esta ocupado, tenta um vizinho livre.
        for (Direction direction : Direction.values()) {
            BlockPos side = pos.offset(direction);
            if (world.getBlockState(side).isReplaceable()) {
                InvaderBlocks.place(world, side, Blocks.COBWEB.getDefaultState());
                return;
            }
        }
    }
}
