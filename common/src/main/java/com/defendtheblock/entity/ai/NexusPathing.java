package com.defendtheblock.entity.ai;

import com.defendtheblock.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Utilitarios de navegacao usados pelas IAs de invasao. */
public final class NexusPathing {

    private NexusPathing() {
    }

    /**
     * Centro do bloco, o ponto que os invasores perseguem. E exatamente
     * {@link Vec3d#ofCenter(net.minecraft.util.math.Vec3i)}; o nome proprio
     * fica so porque "o centro do Nexus" aparece em meia duzia de goals e le
     * melhor assim.
     */
    public static Vec3d center(BlockPos pos) {
        return Vec3d.ofCenter(pos);
    }

    /**
     * Procura o primeiro bloco solido entre o mob e o Nexus, ate 3 blocos a
     * frente, na altura dos pes e da cabeca.
     *
     * @return posicao do obstaculo, ou null se o caminho esta livre
     */
    public static BlockPos findObstacle(MobEntity mob, BlockPos nexus) {
        World world = mob.getWorld();
        Vec3d from = mob.getPos();
        Vec3d delta = center(nexus).subtract(from);
        Vec3d dir = new Vec3d(delta.x, 0.0D, delta.z);
        if (dir.lengthSquared() < 1.0E-4D) {
            return null;
        }
        dir = dir.normalize();

        for (double d = 0.6D; d <= 3.0D; d += 0.5D) {
            BlockPos base = BlockPos.ofFloored(from.x + dir.x * d, from.y, from.z + dir.z * d);
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos pos = base.up(dy);
                if (isObstacle(world, pos)) {
                    return pos;
                }
            }
        }
        return null;
    }

    /**
     * Um bloco conta como obstaculo se e solido e nao esta na lista de
     * intocaveis. Porta fechada e sempre obstaculo, independente do que
     * {@code isSolidBlock} disser sobre ela: o invasor precisa arrombar, nunca
     * so passar por cima da navegacao vanilla abrindo-a sozinha.
     */
    public static boolean isObstacle(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (isClosedDoor(state)) {
            return isBreakable(world, pos);
        }
        if (state.isAir() || !state.isSolidBlock(world, pos)) {
            return false;
        }
        return isBreakable(world, pos);
    }

    /** true se o bloco e uma porta fechada. */
    public static boolean isClosedDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock && !state.get(DoorBlock.OPEN);
    }

    /**
     * Porta fechada bem na frente do mob, no rumo direto do Nexus. Ao contrario
     * de {@link #findObstacle}, olha so 1 a 2 blocos a frente e roda todo tick,
     * sem esperar o mob ficar "travado" — e o que impede a navegacao vanilla de
     * simplesmente abrir a porta e passar por ela antes do
     * {@code BreachObstacleGoal} ter a chance de arromba-la.
     */
    public static BlockPos findClosedDoorAhead(MobEntity mob, BlockPos nexus) {
        World world = mob.getWorld();
        Vec3d from = mob.getPos();
        Vec3d delta = center(nexus).subtract(from);
        Vec3d dir = new Vec3d(delta.x, 0.0D, delta.z);
        if (dir.lengthSquared() < 1.0E-4D) {
            return null;
        }
        dir = dir.normalize();

        for (double d = 0.6D; d <= 2.0D; d += 0.5D) {
            BlockPos pos = BlockPos.ofFloored(from.x + dir.x * d, from.y, from.z + dir.z * d);
            if (isClosedDoor(world.getBlockState(pos))) {
                return pos;
            }
        }
        return null;
    }

    /** O Nexus e os blocos indestrutiveis nunca podem ser removidos por um invasor. */
    public static boolean isBreakable(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isOf(ModBlocks.NEXUS_BLOCK) || state.isOf(Blocks.BEDROCK) || state.isOf(Blocks.BARRIER)) {
            return false;
        }
        if (state.isIn(BlockTags.PORTALS) || state.isIn(BlockTags.FEATURES_CANNOT_REPLACE)) {
            return false;
        }
        // Dureza negativa = indestrutivel (bedrock, barreira, blocos de comando...).
        return state.getHardness(world, pos) >= 0.0F;
    }

    public static boolean isClimbable(World world, BlockPos pos) {
        return world.getBlockState(pos).isIn(BlockTags.CLIMBABLE);
    }

    /** Passo da amostragem de {@link #findCover}, em blocos. */
    private static final double COVER_STEP = 0.25D;

    /**
     * Existe bloco solido entre o invasor e o Nexus?
     *
     * <p>Sem esta checagem, bastava <b>estar perto</b> do Nexus para bate-lo: um
     * mob do lado de fora de uma parede, a menos de 3 blocos do bloco, tirava
     * vida atravessando a parede como se ela nao existisse. Cobrir o Nexus de
     * blocos, que deveria ser a defesa mais obvia do jogo, nao servia para nada.
     *
     * <p>Amostrar a reta a cada 0.25 bloco em vez de usar
     * {@code World#raycast} e proposital: so usa
     * {@code BlockState#isSolidBlock}, que este projeto ja usa em varios
     * lugares, e evita depender da assinatura de {@code RaycastContext} — que e
     * exatamente o tipo de API cuja diferenca entre versoes ja derrubou este
     * mod antes.
     *
     * @return o primeiro bloco solido no caminho, ou null se a linha chega
     *         limpa ate o Nexus
     */
    public static BlockPos findCover(World world, Vec3d from, BlockPos nexus) {
        // Escalares e um BlockPos.Mutable em vez de Vec3d/BlockPos novos por
        // passo: com passo de 0.25 bloco uma linha de 3 blocos ja eram 12 Vec3d
        // mais 12 BlockPos, e isto roda todo tick para cada invasor em alcance
        // do Nexus — e o caminho quente mais alocador do mod.
        double dx = nexus.getX() + 0.5D - from.x;
        double dy = nexus.getY() + 0.5D - from.y;
        double dz = nexus.getZ() + 0.5D - from.z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-4D) {
            return null;
        }

        double scale = COVER_STEP / length;
        double stepX = dx * scale;
        double stepY = dy * scale;
        double stepZ = dz * scale;
        int steps = (int) Math.ceil(length / COVER_STEP);

        double x = from.x;
        double y = from.y;
        double z = from.z;
        BlockPos.Mutable cursor = new BlockPos.Mutable();

        for (int i = 0; i < steps; i++) {
            x += stepX;
            y += stepY;
            z += stepZ;
            cursor.set(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
            if (cursor.equals(nexus)) {
                // Chegou no proprio bloco sem esbarrar em nada: linha limpa.
                return null;
            }
            if (world.getBlockState(cursor).isSolidBlock(world, cursor)) {
                // Imutavel na saida: quem chama guarda esta posicao.
                return cursor.toImmutable();
            }
        }
        return null;
    }
}
