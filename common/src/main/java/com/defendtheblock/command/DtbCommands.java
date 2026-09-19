package com.defendtheblock.command;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.invasion.InvasionData;
import com.defendtheblock.invasion.InvasionManager;
import com.defendtheblock.invasion.NexusManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
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
                        .executes(DtbCommands::stopWave))
                .then(CommandManager.literal("removenexus")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(DtbCommands::removeNexus))
                .then(CommandManager.literal("setwave")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("wave", IntegerArgumentType.integer(1, 1000))
                                .executes(DtbCommands::setWave)))
                .then(CommandManager.literal("spawnwave")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("mob", StringArgumentType.word())
                                .executes(DtbCommands::spawnWave))));
    }

    /**
     * {@code /dtb setwave <n>} — reposiciona a campanha na invasao informada.
     *
     * <p>Mexer so no contador basta: teto de vivos, ritmo de spawn, lote,
     * escalada de vida/dano, qualidade de equipamento e quais tipos de mob
     * estao liberados sao <b>todos</b> derivados do numero da onda. Se uma onda
     * estiver rodando, ela e reiniciada ja no numero novo, para o efeito ser
     * imediato em vez de valer so na proxima noite.
     */
    private static int setWave(CommandContext<ServerCommandSource> context) {
        int wave = IntegerArgumentType.getInteger(context, "wave");
        ServerCommandSource source = context.getSource();
        InvasionData data = NexusManager.getData(source.getServer());
        if (!data.hasNexus()) {
            source.sendFeedback(() -> Text.translatable("commands.defendtheblock.no_nexus")
                    .formatted(Formatting.RED), false);
            return 0;
        }

        boolean wasActive = data.isWaveActive();
        data.setWavesCompleted(wave - 1);
        if (wasActive) {
            InvasionManager.endWave(source.getServer(), data, false);
            InvasionManager.startWave(source.getServer(), data, wave);
        }
        source.sendFeedback(() -> Text.translatable("commands.defendtheblock.wave_set", wave)
                .formatted(Formatting.AQUA), true);
        return 1;
    }

    /**
     * {@code /dtb spawnwave <mob>} — onda de debug com um tipo so.
     *
     * <p>O tipo e resolvido pelo registro a partir do texto, e nao por um
     * argumento de entidade do Brigadier, porque essa familia de argumento
     * mudou entre 1.20.1 ({@code EntitySummonArgumentType}) e 1.21. Registro +
     * {@code Identifier.tryParse} e igual nas duas.
     *
     * <p>Aceita {@code zombie} ou {@code minecraft:zombie}; {@code reset}
     * devolve o sorteio normal.
     */
    private static int spawnWave(CommandContext<ServerCommandSource> context) {
        String raw = StringArgumentType.getString(context, "mob");
        ServerCommandSource source = context.getSource();

        if ("reset".equalsIgnoreCase(raw)) {
            InvasionManager.setForcedSpawnType(null);
            source.sendFeedback(() -> Text.translatable("commands.defendtheblock.spawnwave_reset")
                    .formatted(Formatting.AQUA), true);
            return 1;
        }

        Identifier id = Identifier.tryParse(raw.contains(":") ? raw : "minecraft:" + raw);
        EntityType<?> type = id == null ? null : Registries.ENTITY_TYPE.get(id);
        // O registro devolve PIG para id desconhecido em vez de null, entao a
        // checagem tem que ser pela presenca real da chave.
        if (id == null || !Registries.ENTITY_TYPE.getIds().contains(id)) {
            source.sendFeedback(() -> Text.translatable("commands.defendtheblock.spawnwave_unknown", raw)
                    .formatted(Formatting.RED), false);
            return 0;
        }
        Entity probe = type.create(source.getWorld());
        if (!(probe instanceof MobEntity)) {
            if (probe != null) {
                probe.discard();
            }
            source.sendFeedback(() -> Text.translatable("commands.defendtheblock.spawnwave_not_mob", raw)
                    .formatted(Formatting.RED), false);
            return 0;
        }
        probe.discard();

        @SuppressWarnings("unchecked")
        EntityType<? extends MobEntity> mobType = (EntityType<? extends MobEntity>) type;
        InvasionManager.setForcedSpawnType(mobType);

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
        source.sendFeedback(() -> Text.translatable("commands.defendtheblock.spawnwave_started", raw, wave)
                .formatted(Formatting.AQUA), true);
        return 1;
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

    /** Remove o Nexus sem apagar o mundo — pensado para testes no criativo. */
    private static int removeNexus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        boolean removed = NexusManager.removeSafely(source.getServer());
        if (!removed) {
            source.sendFeedback(() -> Text.translatable("commands.defendtheblock.no_nexus")
                    .formatted(Formatting.RED), false);
            return 0;
        }
        source.sendFeedback(() -> Text.translatable("commands.defendtheblock.nexus_removed")
                .formatted(Formatting.AQUA), true);
        return 1;
    }
}
