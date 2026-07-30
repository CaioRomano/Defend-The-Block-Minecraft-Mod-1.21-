package com.defendtheblock.command;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.invasion.InvasionData;
import com.defendtheblock.invasion.InvasionManager;
import com.defendtheblock.invasion.NexusManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/** Comando {@code /dtb} para inspecionar e ajustar a campanha de invasoes. */
public final class DtbCommands {

    private DtbCommands() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dtb")
                .then(CommandManager.literal("status").executes(DtbCommands::status))
                .then(CommandManager.literal("multiplier")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg(0.1D, 20.0D))
                                .executes(DtbCommands::setMultiplier)))
                .then(CommandManager.literal("forcewave")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(DtbCommands::forceWave))
                .then(CommandManager.literal("stopwave")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(DtbCommands::stopWave)));
    }

    private static int status(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        InvasionData data = NexusManager.getData(source.getServer());
        if (!data.hasNexus()) {
            source.sendFeedback(() -> Text.translatable("commands.defendtheblock.no_nexus")
                    .formatted(Formatting.RED), false);
            return 0;
        }
        BlockPos pos = data.getNexusPos();
        source.sendFeedback(() -> Text.translatable("commands.defendtheblock.status",
                pos.toShortString(),
                data.isWaveActive() ? data.getCurrentWave() : data.getWavesCompleted(),
                data.getMobsAlive(),
                data.getMobsSpawned(),
                String.format("%.2f", data.getMultiplier())), false);
        source.sendFeedback(() -> Text.translatable("hud.defendtheblock.nexus_health",
                data.getNexusHealth(), data.getNexusMaxHealth()), false);
        return 1;
    }

    private static int setMultiplier(CommandContext<ServerCommandSource> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        InvasionData data = NexusManager.getData(context.getSource().getServer());
        data.setMultiplier(value);

        // Persiste tambem na config, para valer nos proximos mundos.
        DtbConfig config = DtbConfig.get();
        config.mobMultiplier = value;
        config.save();

        context.getSource().sendFeedback(() -> Text.translatable("commands.defendtheblock.multiplier_set",
                String.format("%.2f", value)).formatted(Formatting.AQUA), true);
        return 1;
    }

    private static int forceWave(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        InvasionData data = NexusManager.getData(source.getServer());
        if (!data.hasNexus() || data.isGameOver()) {
            source.sendFeedback(() -> Text.translatable("commands.defendtheblock.no_nexus")
                    .formatted(Formatting.RED), false);
            return 0;
        }
        if (data.isWaveActive()) {
            InvasionManager.endWave(source.getServer(), data, false);
        }
        int wave = data.getWavesCompleted() + 1;
        InvasionManager.startWave(source.getServer(), data, wave);
        source.sendFeedback(() -> Text.translatable("commands.defendtheblock.wave_forced", wave), true);
        return 1;
    }

    private static int stopWave(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        InvasionData data = NexusManager.getData(source.getServer());
        if (!data.isWaveActive()) {
            return 0;
        }
        InvasionManager.endWave(source.getServer(), data, false);
        source.sendFeedback(() -> Text.translatable("commands.defendtheblock.wave_stopped"), true);
        return 1;
    }
}
