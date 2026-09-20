package com.defendtheblock.invasion;

import com.defendtheblock.DefendTheBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Todo bloco que um invasor coloca no mundo passa por aqui — e some no fim da
 * noite.
 *
 * <p>Sem isso, cada invasao deixava entulho permanente: pilares de cobblestone
 * dos mobs que ficaram presos, colunas de escada dos zumbis carpinteiros, teia
 * de aranha espalhada. Depois de algumas noites o terreno em volta do Nexus
 * vira um monumento as ondas anteriores.
 *
 * <p>A remocao <b>nao dropa item</b>: os blocos foram criados do nada pela
 * invasao, entao devolve-los ao jogador seria uma fonte infinita de recurso.
 */
public final class InvaderBlocks {

    private InvaderBlocks() {
    }

    /**
     * Coloca um bloco em nome de um invasor e registra a posicao para a
     * faxina do amanhecer.
     */
    public static void place(ServerWorld world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state);
        NexusManager.getData(world).addPlacedBlock(pos);
    }

    /**
     * Coloca um degrau do construtor: o bloco, mais <b>escada em todos os lados
     * livres</b>.
     *
     * <p>As escadas sao o ponto inteiro da mecanica, nao um enfeite. O zumbi
     * construtor e o <b>unico</b> invasor que coloca bloco; todo o resto da
     * horda so sabe subir o que ja existe ({@code ClimbLadderGoal}). Se a
     * coluna subisse sem escada, ele chegaria sozinho no topo e os outros
     * ficariam olhando de baixo. Forrar os quatro lados — em vez de escolher
     * um — garante que quem chegar por qualquer direcao ache degrau, sem o
     * construtor precisar adivinhar de onde a horda vem.
     *
     * @return true se o bloco de apoio foi colocado
     */
    public static boolean placeClimbStep(ServerWorld world, BlockPos pos) {
        if (!world.getBlockState(pos).isAir()) {
            return false;
        }
        place(world, pos, Blocks.COBBLESTONE.getDefaultState());

        for (Direction side : HORIZONTAL) {
            BlockPos ladderPos = pos.offset(side);
            if (!world.getBlockState(ladderPos).isAir()) {
                continue;
            }
            // A escada se apoia em pos.offset(facing.getOpposite()), entao para
            // ela grudar no bloco recem-colocado o facing e a propria direcao.
            place(world, ladderPos, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, side));
        }
        return true;
    }

    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};


    /**
     * Desfaz tudo o que os invasores construiram na invasao que acabou.
     *
     * <p>Antes de remover, confere que o bloco ainda e um dos que a invasao
     * sabe colocar. Isso protege o jogador: se ele minerou o pilar do mob e
     * construiu outra coisa naquela posicao, a faxina passa longe. O unico
     * caso que escapa e o jogador colocar exatamente o mesmo tipo de bloco na
     * exata posicao registrada — raro o bastante para nao valer guardar o
     * estado completo de cada bloco no save.
     */
    public static void clearAll(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        InvasionData data = NexusManager.getData(server);
        int removed = 0;

        for (long packed : data.getPlacedBlocks()) {
            BlockPos pos = BlockPos.fromLong(packed);
            if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
                continue;
            }
            if (!isInvaderBuilt(world.getBlockState(pos))) {
                continue;
            }
            // setBlockState para ar remove sem soltar drop, que e o que
            // queremos: esses blocos nasceram do nada.
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            removed++;
        }

        data.clearPlacedBlocks();
        if (removed > 0) {
            DefendTheBlock.LOGGER.info("Faxina pos-invasao: {} blocos de invasor removidos.", removed);
        }
    }

    /** Os unicos blocos que um invasor consegue colocar. */
    private static boolean isInvaderBuilt(BlockState state) {
        return state.isOf(Blocks.COBBLESTONE)
                || state.isOf(Blocks.LADDER)
                || state.isOf(Blocks.COBWEB)
                || state.isOf(Blocks.FIRE);
    }
}
