package com.defendtheblock;

import com.defendtheblock.command.DtbCommands;
import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.invasion.InvasionData;
import com.defendtheblock.invasion.InvasionManager;
import com.defendtheblock.invasion.NexusChunkLoader;
import com.defendtheblock.invasion.NexusManager;
import com.defendtheblock.registry.ModBlocks;
import com.defendtheblock.registry.ModEntities;
import com.defendtheblock.registry.ModItemGroups;
import com.defendtheblock.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefendTheBlock implements ModInitializer {

    public static final String MOD_ID = "defendtheblock";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        DtbConfig.get();

        ModBlocks.register();
        ModItems.register();
        ModItemGroups.register();
        ModEntities.register();
        DtbCompat.registerServerNetworking();

        ServerTickEvents.END_SERVER_TICK.register(InvasionManager::tick);
        ServerEntityEvents.ENTITY_LOAD.register(InvasionManager::onEntityLoad);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> NexusManager.deletePendingWorld());

        // Reaplica o forceload no boot: pega mudancas de config e conserta o
        // estado caso alguem tenha mexido com /forceload na mao.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            InvasionData data = NexusManager.getData(server);
            if (data.hasNexus() && !data.isGameOver()) {
                NexusChunkLoader.apply(server.getOverworld(), data);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                InvasionManager.syncTo(handler.getPlayer()));

        // O Nexus nao pode ser retirado depois de colocado, nem no criativo.
        PlayerBlockBreakEvents.BEFORE.register(DefendTheBlock::onBeforeBlockBreak);

        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
                DtbCommands.register(dispatcher));

        LOGGER.info("Defend The Block carregado.");
    }

    private static boolean onBeforeBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state,
                                              BlockEntity blockEntity) {
        if (!state.isOf(ModBlocks.NEXUS_BLOCK)) {
            return true;
        }
        if (!world.isClient) {
            player.sendMessage(Text.translatable("message.defendtheblock.nexus_unbreakable")
                    .formatted(Formatting.RED), true);
        }
        return false;
    }
}
