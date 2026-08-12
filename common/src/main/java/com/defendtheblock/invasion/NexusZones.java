package com.defendtheblock.invasion;

import com.defendtheblock.config.DtbConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * A geometria das areas em volta do Nexus, num lugar so.
 *
 * <p>Sao duas regioes concentricas, com <b>metricas diferentes de proposito</b>:
 *
 * <ul>
 *   <li><b>Zona livre de spawn</b> — um <i>quadrado</i> de chunks (distancia de
 *       Chebyshev), porque a ideia e literalmente "o chunk do Nexus mais os
 *       adjacentes": com o padrao {@code noSpawnChunkRadius = 1} isso e o 3x3
 *       de chunks em volta do bloco. Nada hostil nasce ali, nem a invasao nem o
 *       spawn natural do vanilla.</li>
 *   <li><b>Anel de invasao</b> — uma <i>coroa circular</i> (distancia
 *       euclidiana) que comeca onde o quadrado seguro termina e se estende por
 *       {@code spawnRingChunks} chunks. Com os padroes, os invasores nascem
 *       entre o chunk 2 e o chunk 4 a partir do Nexus.</li>
 * </ul>
 *
 * <p>Manter as duas regras aqui evita o risco classico de o spawn da invasao e
 * a checagem da zona segura discordarem por meio chunk e um mob nascer dentro
 * do quintal que deveria estar limpo.
 */
public final class NexusZones {

    private NexusZones() {
    }

    /** Raio (em chunks) do quadrado onde nada nasce. */
    private static int noSpawnRadius() {
        return Math.max(0, DtbConfig.get().noSpawnChunkRadius);
    }

    /** Raio (em chunks) da borda externa do anel de invasao. */
    public static int outerRadius() {
        return noSpawnRadius() + Math.max(1, DtbConfig.get().spawnRingChunks);
    }

    /** Distancia de Chebyshev, em chunks — a "vizinhanca quadrada" do jogo. */
    public static int chunkDistance(ChunkPos from, ChunkPos to) {
        return Math.max(Math.abs(from.x - to.x), Math.abs(from.z - to.z));
    }

    /**
     * O chunk esta dentro do quadrado livre de spawn?
     *
     * @param nexus posicao do Nexus
     * @param chunk chunk a testar
     */
    public static boolean isNoSpawnZone(BlockPos nexus, ChunkPos chunk) {
        return chunkDistance(new ChunkPos(nexus), chunk) <= noSpawnRadius();
    }

    /**
     * O chunk serve para a invasao spawnar? Precisa estar fora do quadrado
     * seguro e dentro do circulo externo.
     */
    public static boolean isInvasionSpawnZone(BlockPos nexus, ChunkPos chunk) {
        ChunkPos origin = new ChunkPos(nexus);
        if (chunkDistance(origin, chunk) <= noSpawnRadius()) {
            return false;
        }
        int dx = chunk.x - origin.x;
        int dz = chunk.z - origin.z;
        int outer = outerRadius();
        // Limite externo circular: e o "circulo maior" da area de invasao, em
        // vez de mais um quadrado concentrico.
        return dx * dx + dz * dz <= outer * outer;
    }
}
