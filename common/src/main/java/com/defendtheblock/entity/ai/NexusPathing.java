package com.defendtheblock.entity.ai;

import com.defendtheblock.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Utilitarios de navegacao usados pelas IAs de invasao. */
public final class NexusPathing {

    private NexusPathing() {
    }

    /** Centro do bloco, o ponto que os invasores perseguem. */
    public static Vec3d center(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    /**
     * Procura o primeiro bloco solido entre o mob e o Nexus, ate 3 blocos a
     * frente, na altura dos pes e da cabeca.
     *
     * @return posicao do obstaculo, ou null se o caminho esta livre
     */
    public static BlockPos findObstacle(MobEntity mob, BlockPos nexus) {
        World world = mob.getWorld();
        Vec3d from = mob.getPos();
        Vec3d delta = center(nexus).subtract(from);
        Vec3d dir = new Vec3d(delta.x, 0.0D, delta.z);
        if (dir.lengthSquared() < 1.0E-4D) {
            return null;
        }
        dir = dir.normalize();

        for (double d = 0.6D; d <= 3.0D; d += 0.5D) {
            BlockPos base = BlockPos.ofFloored(from.x + dir.x * d, from.y, from.z + dir.z * d);
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos pos = base.up(dy);
                if (isObstacle(world, pos)) {
                    return pos;
                }
            }
        }
        return null;
    }

    /** Um bloco conta como obstaculo se e solido e nao esta na lista de intocaveis. */
    public static boolean isObstacle(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || !state.isSolidBlock(world, pos)) {
            return false;
        }
        return isBreakable(world, pos);
    }

    /** O Nexus e os blocos indestrutiveis nunca podem ser removidos por um invasor. */
    public static boolean isBreakable(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isOf(ModBlocks.NEXUS_BLOCK) || state.isOf(Blocks.BEDROCK) || state.isOf(Blocks.BARRIER)) {
            return false;
        }
        if (state.isIn(BlockTags.PORTALS) || state.isIn(BlockTags.FEATURES_CANNOT_REPLACE)) {
            return false;
        }
        // Dureza negativa = indestrutivel (bedrock, barreira, blocos de comando...).
        return state.getHardness(world, pos) >= 0.0F;
    }

    public static boolean isClimbable(World world, BlockPos pos) {
        return world.getBlockState(pos).isIn(BlockTags.CLIMBABLE);
    }
}
