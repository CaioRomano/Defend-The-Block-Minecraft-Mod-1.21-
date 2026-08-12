package com.defendtheblock.item;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.invasion.InvasionData;
import com.defendtheblock.invasion.NexusManager;
import com.defendtheblock.registry.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/**
 * Totem de Reuniao.
 *
 * <p>Ele reage ao mesmo gesto que colocaria um bloco no mundo (clique com o
 * botao direito, no ar ou num bloco), mas nao coloca nada: puxa para o Nexus
 * todos os jogadores que estiverem carregando um totem.
 */
public class GatheringTotemItem extends Item {

    public GatheringTotemItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack, true);
        }
        return gather(user, stack)
                ? TypedActionResult.success(stack, false)
                : TypedActionResult.fail(stack);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        // Mesmo gesto de "colocar bloco", mas sem colocar bloco nenhum.
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResult.PASS;
        }
        if (context.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }
        return gather(player, context.getStack()) ? ActionResult.CONSUME : ActionResult.FAIL;
    }

    private boolean gather(PlayerEntity user, ItemStack stack) {
        if (!(user instanceof ServerPlayerEntity serverUser)) {
            return false;
        }
        MinecraftServer server = serverUser.server;
        InvasionData data = NexusManager.getData(server);

        if (!data.hasNexus()) {
            user.sendMessage(Text.translatable("message.defendtheblock.totem_no_nexus")
                    .formatted(Formatting.RED), true);
            return false;
        }
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            user.sendMessage(Text.translatable("message.defendtheblock.totem_cooldown", "..."), true);
            return false;
        }

        ServerWorld overworld = server.getOverworld();
        BlockPos landing = findLanding(overworld, data.getNexusPos());
        int cooldown = DtbConfig.get().totemCooldown;
        ItemStack probe = new ItemStack(ModItems.GATHERING_TOTEM);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!player.getInventory().contains(probe)) {
                continue;
            }
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT,
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
            player.teleport(overworld, landing.getX() + 0.5D, landing.getY(), landing.getZ() + 0.5D,
                    player.getYaw(), player.getPitch());
            player.getItemCooldownManager().set(this, cooldown);
            player.sendMessage(Text.translatable("message.defendtheblock.totem_teleport")
                    .formatted(Formatting.AQUA), false);
        }

        overworld.spawnParticles(ParticleTypes.REVERSE_PORTAL, landing.getX() + 0.5D, landing.getY() + 0.5D,
                landing.getZ() + 0.5D, 60, 0.6D, 1.0D, 0.6D, 0.1D);
        overworld.playSound(null, landing, SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    /** Espaco livre logo acima do Nexus, ou a superficie ao lado dele. */
    private BlockPos findLanding(ServerWorld world, BlockPos nexus) {
        BlockPos above = nexus.up();
        if (world.getBlockState(above).isAir() && world.getBlockState(above.up()).isAir()) {
            return above;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = nexus.getX() + dx * 2;
                int z = nexus.getZ() + dz * 2;
                BlockPos candidate = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                if (world.getBlockState(candidate).isAir() && world.getBlockState(candidate.up()).isAir()) {
                    return candidate;
                }
            }
        }
        return above;
    }
}
