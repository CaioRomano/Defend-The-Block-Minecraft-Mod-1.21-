package com.defendtheblock.invasion;

import com.defendtheblock.DefendTheBlock;
import com.defendtheblock.config.DtbConfig;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * Mantem o chunk do Nexus e os vizinhos sempre carregados.
 *
 * <p>Usa o forceload do proprio Minecraft ({@link ServerWorld#setChunkForced}),
 * o mesmo do comando {@code /forceload}. Duas consequencias importantes disso:
 * o estado e persistido pelo vanilla em {@code forcedchunks.dat} (sobrevive a
 * reinicios sozinho) e os chunks ficam em nivel de <i>entity ticking</i>, ou
 * seja, mobs, spawns e block entities continuam rodando ali mesmo sem nenhum
 * jogador por perto.
 *
 * <p>O raio efetivamente aplicado fica salvo em {@link InvasionData} para que
 * uma mudanca de config libere exatamente a area antiga antes de forcar a nova.
 */
public final class NexusChunkLoader {

    private NexusChunkLoader() {
    }

    /**
     * Sincroniza a area carregada com a config atual. Idempotente: pode ser
     * chamado no boot do servidor e sempre que o Nexus for (re)ativado.
     */
    public static void apply(ServerWorld world, InvasionData data) {
        if (!data.hasNexus()) {
            return;
        }
        DtbConfig config = DtbConfig.get();
        int desired = config.keepNexusChunksLoaded ? Math.max(0, config.forcedChunkRadius) : -1;
        int previous = data.getForcedRadius();

        // Se o raio mudou, solta a area antiga antes de marcar a nova.
        if (previous >= 0 && previous != desired) {
            setForced(world, data.getNexusPos(), previous, false);
        }
        if (desired >= 0) {
            int count = setForced(world, data.getNexusPos(), desired, true);
            if (previous != desired) {
                DefendTheBlock.LOGGER.info("Mantendo {} chunk(s) carregados em volta do Nexus em {}",
                        count, data.getNexusPos().toShortString());
            }
        }
        data.setForcedRadius(desired);
    }

    /** Libera a area carregada (Nexus destruido ou removido). */
    public static void release(ServerWorld world, InvasionData data) {
        int previous = data.getForcedRadius();
        if (previous < 0 || !data.hasNexus()) {
            return;
        }
        setForced(world, data.getNexusPos(), previous, false);
        data.setForcedRadius(-1);
        DefendTheBlock.LOGGER.info("Chunks do Nexus liberados.");
    }

    private static int setForced(ServerWorld world, BlockPos center, int radius, boolean forced) {
        ChunkPos origin = new ChunkPos(center);
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                world.setChunkForced(origin.x + dx, origin.z + dz, forced);
                count++;
            }
        }
        return count;
    }
}
