package com.defendtheblock.invasion;

import com.defendtheblock.DefendTheBlock;
import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.config.DtbConfig;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Ponto unico de acesso ao estado do Nexus: ativacao, dano, queda e (se estiver
 * ligado na config) a exclusao do mundo.
 */
public final class NexusManager {

    /** Ticks entre a queda do Nexus e o desligamento do servidor. */
    private static final int SHUTDOWN_DELAY_TICKS = 140;

    private static int shutdownCountdown = -1;
    private static Path pendingDeletion;

    private NexusManager() {
    }

    /** O estado sempre vive no Overworld, mesmo que quem pergunte esteja em outra dimensao. */
    public static InvasionData getData(ServerWorld world) {
        return getData(world.getServer());
    }

    public static InvasionData getData(MinecraftServer server) {
        return DtbCompat.getInvasionData(server.getOverworld());
    }

    // ------------------------------------------------------------ ativacao

    public static void activate(ServerWorld world, BlockPos pos, PlayerEntity placer) {
        InvasionData data = getData(world);
        data.placeNexus(pos);
        data.setMultiplier(DtbConfig.get().mobMultiplier);
        NexusChunkLoader.apply(world.getServer().getOverworld(), data);

        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                60, 0.6D, 0.6D, 0.6D, 0.08D);

        Text message = Text.translatable("message.defendtheblock.nexus_placed").formatted(Formatting.AQUA);
        world.getServer().getPlayerManager().broadcast(message, false);
        DefendTheBlock.LOGGER.info("Nexus ativado em {}", pos.toShortString());
        if (placer != null) {
            placer.sendMessage(Text.translatable("tooltip.defendtheblock.nexus_block.3").formatted(Formatting.RED), false);
        }
    }

    // ---------------------------------------------------------------- dano

    /**
     * Aplica dano ao Nexus.
     *
     * @return true se o Nexus caiu com este golpe.
     */
    public static boolean damage(ServerWorld world, int amount, Entity source) {
        InvasionData data = getData(world);
        if (!data.hasNexus() || data.isGameOver() || amount <= 0) {
            return false;
        }

        BlockPos pos = data.getNexusPos();
        boolean destroyed = data.damageNexus(amount);

        ServerWorld nexusWorld = world.getServer().getOverworld();
        nexusWorld.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.BLOCKS, 1.0F,
                0.6F + nexusWorld.random.nextFloat() * 0.3F);
        nexusWorld.spawnParticles(ParticleTypes.CRIT, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                8, 0.5D, 0.5D, 0.5D, 0.1D);

        if (destroyed) {
            fall(nexusWorld, pos);
        }
        return destroyed;
    }

    private static void fall(ServerWorld world, BlockPos pos) {
        InvasionData data = getData(world);
        data.setGameOver(true);
        data.finishWave(false);
        // Sem Nexus nao ha mais motivo para segurar os chunks carregados.
        NexusChunkLoader.release(world, data);

        world.setBlockState(pos, Blocks.AIR.getDefaultState());
        world.playSound(null, pos, SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.BLOCKS, 1.0F, 0.6F);
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, 6, 1.5D, 1.5D, 1.5D, 0.0D);

        MinecraftServer server = world.getServer();
        Text title = Text.translatable("message.defendtheblock.nexus_destroyed").formatted(Formatting.DARK_RED);
        Text subtitle = DtbConfig.get().deleteWorldOnNexusDestroyed
                ? Text.translatable("message.defendtheblock.world_deleting").formatted(Formatting.RED)
                : Text.empty();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(new TitleS2CPacket(title));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
        }
        server.getPlayerManager().broadcast(title, false);
        DefendTheBlock.LOGGER.warn("O Nexus foi destruido em {}", pos.toShortString());

        InvasionManager.despawnAllInvaders(server);

        if (DtbConfig.get().deleteWorldOnNexusDestroyed) {
            pendingDeletion = server.getSavePath(WorldSavePath.ROOT).toAbsolutePath().normalize();
            shutdownCountdown = SHUTDOWN_DELAY_TICKS;
        }
    }

    /**
     * Remove o Nexus sem acionar a derrota: nao apaga o mundo, nao desliga o
     * servidor, so limpa o bloco e o estado da campanha. Pensado para testes
     * no criativo, via {@code /dtb removenexus}.
     *
     * @return true se havia um Nexus para remover.
     */
    public static boolean removeSafely(MinecraftServer server) {
        InvasionData data = getData(server);
        if (!data.hasNexus()) {
            return false;
        }

        BlockPos pos = data.getNexusPos();
        ServerWorld world = server.getOverworld();
        NexusChunkLoader.release(world, data);
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0F, 1.0F);

        InvasionManager.despawnAllInvaders(server);
        data.clearNexus();
        DefendTheBlock.LOGGER.info("Nexus removido manualmente (comando admin) em {}", pos.toShortString());
        return true;
    }

    // ------------------------------------------------ desligamento e delecao

    /** Chamado a cada tick do servidor por {@code DefendTheBlock}. */
    public static void tickShutdown(MinecraftServer server) {
        if (shutdownCountdown < 0) {
            return;
        }
        if (shutdownCountdown-- > 0) {
            return;
        }
        shutdownCountdown = -1;

        Text reason = Text.translatable("message.defendtheblock.nexus_destroyed")
                .append(" ")
                .append(Text.translatable("message.defendtheblock.world_deleting"));
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.disconnect(reason);
        }
        server.stop(false);
    }

    /** Registrado em {@code ServerLifecycleEvents.SERVER_STOPPED}. */
    public static void deletePendingWorld() {
        Path target = pendingDeletion;
        pendingDeletion = null;
        if (target == null) {
            return;
        }

        Thread worker = new Thread(() -> {
            // O session.lock ainda pode estar aberto por alguns instantes.
            for (int attempt = 0; attempt < 30; attempt++) {
                if (!Files.exists(target)) {
                    return;
                }
                if (deleteRecursively(target)) {
                    DefendTheBlock.LOGGER.warn("Mundo apagado: {}", target);
                    return;
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            DefendTheBlock.LOGGER.error("Nao foi possivel apagar o mundo em {}", target);
        }, "defendtheblock-world-deleter");
        worker.setDaemon(false);
        worker.start();
    }

    private static boolean deleteRecursively(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // tentamos de novo na proxima passada
                }
            });
        } catch (IOException e) {
            return false;
        }
        return !Files.exists(root);
    }
}
