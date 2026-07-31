package com.defendtheblock.item;

import com.defendtheblock.entity.turret.TurretEntity;
import com.defendtheblock.registry.ModEntities;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Item que posiciona a torreta de flechas no mundo. */
public class ArrowTurretItem extends Item {

    public ArrowTurretItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        BlockPos pos = context.getBlockPos().offset(context.getSide());
        if (!world.getBlockState(pos).isReplaceable()) {
            return ActionResult.FAIL;
        }

        TurretEntity turret = ModEntities.ARROW_TURRET.create(world);
        if (turret == null) {
            return ActionResult.FAIL;
        }
        turret.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                context.getPlayerYaw(), 0.0F);
        if (!world.spawnEntity(turret)) {
            return ActionResult.FAIL;
        }

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().creativeMode) {
            context.getStack().decrement(1);
        }
        return ActionResult.CONSUME;
    }
}
