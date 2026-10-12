package com.defendtheblock.invasion;

import com.defendtheblock.config.DtbConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * A geometria das areas em volta do Nexus, num lugar so.
 *
 * <p>Tudo parte de uma unica medida: a <b>distancia minima de spawn</b>
 * ({@code minSpawnChunkDistance}, 4 chunks por padrao). Dela saem duas regioes,
 * com <b>metricas diferentes de proposito</b>:
 *
 * <ul>
 *   <li><b>Zona livre de spawn</b> — um <i>quadrado</i> de chunks (distancia de
 *       Chebyshev), porque e assim que o jogo enxerga vizinhanca de chunk: todo
 *       chunk a menos de {@code minSpawnChunkDistance} do Nexus. Com o padrao,
 *       o quadrado de 7x7 chunks em volta do bloco. Nada hostil nasce ali, nem
 *       a invasao nem o spawn natural do vanilla.</li>
 *   <li><b>Anel de invasao</b> — uma <i>coroa circular</i> (distancia
 *       euclidiana) que comeca exatamente onde o quadrado seguro termina e se
 *       estende por {@code spawnRingChunks} chunks. Com os padroes, os
 *       invasores nascem entre o chunk 4 e o chunk 6 a partir do Nexus.</li>
 * </ul>
 *
 * <p>Manter as duas regras aqui evita o risco classico de o spawn da invasao e
 * a checagem da zona segura discordarem por meio chunk e um mob nascer dentro
 * do quintal que deveria estar limpo. {@link NexusChunkLoader} tambem le
 * {@link #outerRadius()} para garantir que o anel inteiro fique carregado.
 */
public final class NexusZones {

    private NexusZones() {
    }

    /**
     * Distancia minima (em chunks) para qualquer mob hostil nascer. Minimo 1:
     * o chunk do Nexus nunca e area de spawn.
     */
    public static int minSpawnDistance() {
        return Math.max(1, DtbConfig.get().minSpawnChunkDistance);
    }

    /** Raio (em chunks) da borda externa do anel de invasao. */
    public static int outerRadius() {
        return minSpawnDistance() + Math.max(1, DtbConfig.get().spawnRingChunks) - 1;
    }

    /** Distancia de Chebyshev, em chunks — a "vizinhanca quadrada" do jogo. */
    public static int chunkDistance(ChunkPos from, ChunkPos to) {
        return Math.max(Math.abs(from.x - to.x), Math.abs(from.z - to.z));
    }

    /**
     * O chunk esta perto demais do Nexus para alguem nascer? Vale tanto para a
     * invasao quanto para o spawn natural da noite.
     *
     * @param nexus posicao do Nexus
     * @param chunk chunk a testar
     */
    public static boolean isNoSpawnZone(BlockPos nexus, ChunkPos chunk) {
        return chunkDistance(new ChunkPos(nexus), chunk) < minSpawnDistance();
    }

    /**
     * O chunk serve para a invasao spawnar? Precisa respeitar a distancia
     * minima e estar dentro do circulo externo.
     */
    public static boolean isInvasionSpawnZone(BlockPos nexus, ChunkPos chunk) {
        ChunkPos origin = new ChunkPos(nexus);
        if (chunkDistance(origin, chunk) < minSpawnDistance()) {
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
