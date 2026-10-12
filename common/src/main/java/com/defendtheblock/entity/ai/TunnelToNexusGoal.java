package com.defendtheblock.entity.ai;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.invader.InvaderAbility;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import com.defendtheblock.invasion.NexusManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.EnumSet;

/**
 * O tunel do zumbi sapador: o espelho exato da rampa do construtor, para o
 * Nexus que esta <b>abaixo</b> em vez de acima.
 *
 * <p><b>O problema que resolve.</b> O spawn da invasao sempre nasce na
 * superficie. Com o Nexus enterrado, a horda chegava em cima da base e ficava
 * girando: a {@link BridgeToNexusGoal} so age quando o bloco esta acima do mob,
 * e a {@link BreachObstacleGoal} so trata um obstaculo <i>identificado</i> no
 * caminho — nenhuma das duas sabe que o caminho e para baixo. Uma base
 * subterranea bem fechada era praticamente imune.
 *
 * <p><b>Quem cava:</b> so o {@link InvaderAbility#PICKAXE_MINER}, o mesmo que
 * abre parede. Colocar e tirar bloco continua sendo privilegio de um mob por
 * especialidade, nao da horda inteira — e matar esse mob continua sendo uma
 * jogada com consequencia visivel.
 *
 * <p><b>Como cava:</b> uma escada descendente de 1 de largura e 2 de altura, um
 * degrau por vez no rumo do Nexus. Nao e um poco: poco de 1x1 a horda nao desce
 * (queda alta e penalidade de pathfinding) e nao sobe nunca mais. Degrau de um
 * bloco por passo, sim — todo mob anda nele sem precisar saber de nada, que e o
 * mesmo motivo de o construtor forrar a rampa de escadas. O tunel e a passagem
 * da horda, nao um atalho pessoal.
 *
 * <p>Fica em {@link Control#MOVE} porque precisa empurrar o mob para dentro do
 * degrau que acabou de abrir. A {@link AttackNexusGoal} sozinha nao faria isso:
 * o pathfinding dela ainda nao ve caminho ate o Nexus (e justamente por isso
 * que este goal acordou), entao o mob abriria o primeiro degrau e ficaria
 * parado olhando para ele.
 */
public class TunnelToNexusGoal extends Goal {

    /** Ciclos de travamento antes de comecar a cavar. */
    private static final int STUCK_THRESHOLD = 1;
    /** Teto de blocos removidos por tunel, para nao virar uma mina infinita. */
    private static final int MAX_BLOCKS_MINED = 128;
    /** Desnivel minimo para o tunel fazer sentido. */
    private static final int MIN_DROP = 2;

    private final MobEntity mob;
    private final InvaderData data;
    private final double speed;

    private int blocksMined;
    private int digCooldown;

    public TunnelToNexusGoal(MobEntity mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.data = InvaderAccess.of(mob);
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (data == null || !data.isInvader() || data.getNexusPos() == null) {
            return false;
        }
        if (!data.hasAbility(InvaderAbility.PICKAXE_MINER)) {
            return false;
        }
        if (!(mob.getWorld() instanceof ServerWorld world) || NexusManager.getData(world).isGameOver()) {
            return false;
        }
        if (data.getStuckTicks() < STUCK_THRESHOLD) {
            return false;
        }
        // So faz sentido quando o Nexus esta visivelmente abaixo: e o caso da
        // base enterrada, do bunker, do porao sem entrada.
        return mob.getBlockY() - data.getNexusPos().getY() >= MIN_DROP;
    }

    @Override
    public boolean shouldContinue() {
        return canStart() && blocksMined < MAX_BLOCKS_MINED;
    }

    @Override
    public void start() {
        blocksMined = 0;
        digCooldown = 0;
        // Pede espaco: cavar exige o mob parado num ponto exato, e a horda
        // empurrando o tira de la (ver YieldToWorkerGoal).
        data.setWorking(true);
    }

    @Override
    public void stop() {
        data.setWorking(false);
        data.setStuckTicks(0);
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(mob.getWorld() instanceof ServerWorld world)) {
            return;
        }
        BlockPos ahead = mob.getBlockPos().offset(towardNexus(data.getNexusPos()));
        BlockPos below = ahead.down();

        // Onde o mob vai pisar no proximo passo. Descer um degrau e o caso
        // normal; seguir reto acontece quando o degrau nao tem piso. Nos dois
        // casos sobra chao solido sob os pes: o tunel nunca abre queda, nem
        // para a horda nem para o proprio sapador.
        BlockPos target;
        if (isFloor(world, below.down())) {
            target = below;
        } else if (isFloor(world, below)) {
            target = ahead;
        } else {
            // Vazio a frente e abaixo: ja existe passagem aberta (caverna,
            // ravina). Nao e caso de cavar — devolve o mob para o pathfinding.
            abort();
            return;
        }

        mob.getLookControl().lookAt(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);

        // Dois blocos por passo, de cima para baixo: o espaco da cabeca e o
        // dos pes. Um bloco por vez, no ritmo da picareta.
        if (clear(world, target.up()) || clear(world, target)) {
            return;
        }

        // Passagem aberta: entra nela. O proximo tick recalcula o rumo a
        // partir da posicao nova.
        mob.getNavigation().startMovingTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
    }

    /**
     * Remove o bloco se houver bloco para remover, respeitando o ritmo de
     * escavacao.
     *
     * @return true se ainda ha trabalho nesta posicao (ou acabou de ter), ou
     *         seja: o chamador nao deve seguir para o passo seguinte
     */
    private boolean clear(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        // Liquido nao se cava: abrir um oceano ou lava dentro do tunel
        // afogaria a propria horda e inundaria a base por um caminho que o
        // jogador nunca teve chance de defender.
        if (!state.getFluidState().isEmpty()) {
            abort();
            return true;
        }
        if (!NexusPathing.isBreakable(world, pos)) {
            abort();
            return true;
        }
        if (state.getHardness(world, pos) > DtbConfig.get().maxMineHardness) {
            // Duro demais para a picareta (obsidiana e companhia): o tunel
            // morre aqui em vez de empacar o mob para sempre.
            abort();
            return true;
        }

        if (digCooldown-- > 0) {
            return true;
        }
        digCooldown = Math.max(1, DtbConfig.get().tunnelTicksPerBlock);
        mob.swingHand(Hand.MAIN_HAND);
        world.playSound(null, pos, state.getSoundGroup().getBreakSound(),
                SoundCategory.HOSTILE, 0.6F, 0.9F);
        world.breakBlock(pos, false, mob);
        blocksMined++;
        return true;
    }

    /** Ha piso solido aqui, do tipo em que um mob fica de pe? */
    private boolean isFloor(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).isSolidBlock(world, pos);
    }

    /** Desiste do tunel sem deixar o mob travado neste goal. */
    private void abort() {
        blocksMined = MAX_BLOCKS_MINED;
        data.setStuckTicks(0);
    }

    /**
     * O rumo horizontal do Nexus, arredondado para um dos quatro lados. O eixo
     * dominante e o que mais aproxima: cavar na diagonal exigiria abrir duas
     * colunas por passo para a passagem continuar andavel.
     */
    private Direction towardNexus(BlockPos nexus) {
        int dx = nexus.getX() - mob.getBlockX();
        int dz = nexus.getZ() - mob.getBlockZ();
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
