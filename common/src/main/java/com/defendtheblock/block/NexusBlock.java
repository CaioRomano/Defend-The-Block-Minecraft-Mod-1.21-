package com.defendtheblock.block;

import com.defendtheblock.invasion.InvasionData;
import com.defendtheblock.invasion.NexusManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * O bloco que o jogador precisa defender.
 *
 * <p>Regras:
 * <ul>
 *   <li>So funciona no Overworld;</li>
 *   <li>apenas o <b>primeiro</b> Nexus colocado no mundo vale: qualquer outro e
 *       devolvido para o inventario do jogador;</li>
 *   <li>depois de colocado nao pode mais ser retirado (dureza -1 + cancelamento
 *       do evento de quebra, ver {@code DefendTheBlock});</li>
 *   <li>a vida dele mora em {@link InvasionData}, nao no {@code BlockState}.</li>
 * </ul>
 */
public class NexusBlock extends Block {

    public NexusBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (!world.getRegistryKey().equals(World.OVERWORLD)) {
            reject(serverWorld, pos, placer, Text.translatable("message.defendtheblock.wrong_dimension"));
            return;
        }

        InvasionData data = NexusManager.getData(serverWorld);
        if (data.hasNexus() && !data.getNexusPos().equals(pos)) {
            BlockPos existing = data.getNexusPos();
            reject(serverWorld, pos, placer, Text.translatable("message.defendtheblock.nexus_exists",
                    existing.getX(), existing.getY(), existing.getZ()));
            return;
        }

        if (!data.hasNexus()) {
            NexusManager.activate(serverWorld, pos, placer instanceof PlayerEntity player ? player : null);
        }
    }

    /** Remove o bloco recem colocado e devolve o item para quem colocou. */
    private void reject(ServerWorld world, BlockPos pos, LivingEntity placer, Text reason) {
        world.removeBlock(pos, false);
        if (placer instanceof PlayerEntity player) {
            if (!player.getAbilities().creativeMode) {
                player.getInventory().offerOrDrop(new ItemStack(this));
            }
            player.sendMessage(reason, false);
        }
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 0.6F, 1.4F);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        for (int i = 0; i < 2; i++) {
            world.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 1.4D,
                    pos.getY() + 1.0D + random.nextDouble() * 0.6D,
                    pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 1.4D,
                    0.0D, 0.02D, 0.0D);
        }
        if (random.nextInt(6) == 0) {
            world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                    0.0D, 0.01D, 0.0D);
        }
    }
}
