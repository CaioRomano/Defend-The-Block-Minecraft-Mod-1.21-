package com.defendtheblock.invasion;

import com.defendtheblock.config.DtbConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.Arrays;

/**
 * A geometria das areas em volta do Nexus, num lugar so.
 *
 * <p>Tudo parte de uma unica medida: a <b>distancia minima de spawn</b>
 * ({@code minSpawnChunkDistance}, 4 chunks por padrao). Dela saem duas coisas,
 * com papeis bem diferentes:
 *
 * <ul>
 *   <li><b>A regra</b> — {@link #isTooCloseToSpawn}, medida em <b>blocos e em
 *       3D</b>: uma esfera de {@code minSpawnChunkDistance * 16} blocos de raio
 *       em volta do bloco. Nenhum mob agressivo nasce dentro dela, nem a
 *       invasao nem o spawn natural da noite. E a palavra final; vale para
 *       qualquer posicao do mundo.</li>
 *   <li><b>O amostrador</b> — {@link #ringOffsets}, a lista de chunks onde a
 *       invasao procura lugar: do chunk {@code minSpawnChunkDistance} ate
 *       {@code outerRadius()}, com borda externa circular. Serve so para
 *       <i>onde olhar</i> e e pre-calculado, porque roda no caminho quente do
 *       spawn.</li>
 * </ul>
 *
 * <p><b>Por que a regra e 3D.</b> Antes ela era a vizinhanca quadrada de chunks
 * — uma coluna de altura infinita. Isso dizia que uma caverna 80 blocos abaixo
 * da base estava "a 0 chunk de distancia" e esterilizava o mundo inteiro em
 * baixo do Nexus, enquanto na pratica aquilo e mais longe do bloco do que o
 * campo aberto a 4 chunks. Medindo em 3D a conta fica honesta nos dois
 * sentidos: horizontalmente e praticamente a mesma area de antes (circulo de
 * 128 blocos de diametro, em vez de um quadrado de 112), e verticalmente a
 * protecao passa a ter fim — ceu alto e caverna funda voltam a ser lugar de
 * mob, que e o que torna uma base enterrada atacavel.
 *
 * <p>O amostrador <b>nao</b> substitui a regra: um chunk na borda interna do
 * anel ainda contem cantos a menos de 64 blocos do bloco, entao
 * {@link InvasionManager} aplica a regra exata na posicao final.
 */
public final class NexusZones {

    private NexusZones() {
    }

    /**
     * Distancia minima (em chunks) para qualquer mob agressivo nascer. Minimo 1:
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
     * <b>A regra.</b> Esta posicao esta perto demais do Nexus para alguem
     * nascer? Distancia euclidiana em 3D, em blocos.
     *
     * <p>Em inteiros de proposito: roda uma vez por mob que entra no mundo e
     * varias vezes por spawn de invasor, e nao precisa de raiz quadrada nem de
     * ponto flutuante para comparar com um limite fixo.
     */
    public static boolean isTooCloseToSpawn(BlockPos nexus, BlockPos pos) {
        long dx = pos.getX() - nexus.getX();
        long dy = pos.getY() - nexus.getY();
        long dz = pos.getZ() - nexus.getZ();
        long limit = (long) minSpawnDistance() * 16L;
        return dx * dx + dy * dy + dz * dz < limit * limit;
    }

    // ------------------------------------------------------- o amostrador

    /** Pares {@code dx, dz} achatados do anel atual. Ver {@link #ringOffsets}. */
    private static int[] ringOffsets;
    /** Config que gerou {@link #ringOffsets}, para refazer quando ela mudar. */
    private static int ringOffsetsKey = -1;

    /**
     * Os offsets de chunk do anel de invasao, como pares {@code dx, dz}
     * achatados num unico array ({@code dx} em {@code 2i}, {@code dz} em
     * {@code 2i + 1}).
     *
     * <p><b>Por que pre-calculado:</b> o spawn sorteia um chunk do anel a cada
     * tentativa. Sorteando dentro do quadrado que contem o anel, com os padroes
     * so <b>38%</b> dos sorteios caiam no anel (64 chunks validos de 169) — quase
     * dois tercos das tentativas eram descartadas antes de qualquer checagem de
     * terreno, e isso num caminho que roda dezenas de vezes por segundo nas
     * ondas altas. Sorteando um indice desta lista, toda tentativa cai no anel.
     *
     * <p>O array e devolvido sem copia, por ser caminho quente: quem chamar
     * <b>nao deve escrever nele</b>. Vive na thread do servidor, unica que
     * spawna invasor e recarrega config.
     */
    public static int[] ringOffsets() {
        int min = minSpawnDistance();
        int ring = Math.max(1, DtbConfig.get().spawnRingChunks);
        int key = min * 1024 + ring;

        int[] cached = ringOffsets;
        if (cached != null && ringOffsetsKey == key) {
            return cached;
        }

        int outer = min + ring - 1;
        int[] built = new int[(2 * outer + 1) * (2 * outer + 1) * 2];
        int size = 0;
        for (int dx = -outer; dx <= outer; dx++) {
            for (int dz = -outer; dz <= outer; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) < min) {
                    continue;
                }
                // Borda externa circular: uma coroa em volta do quadrado
                // seguro, em vez de mais um quadrado concentrico.
                if (dx * dx + dz * dz > outer * outer) {
                    continue;
                }
                built[size++] = dx;
                built[size++] = dz;
            }
        }

        int[] trimmed = Arrays.copyOf(built, size);
        ringOffsets = trimmed;
        ringOffsetsKey = key;
        return trimmed;
    }
}
