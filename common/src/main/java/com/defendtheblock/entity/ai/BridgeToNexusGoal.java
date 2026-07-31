package com.defendtheblock.entity.ai;

import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import com.defendtheblock.invasion.NexusManager;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

/**
 * Deixa o invasor construir caminho proprio quando o Nexus esta num lugar mais
 * alto ou suspenso no ar e ele fica preso sem conseguir se aproximar.
 *
 * <p>E uma heuristica simples e limitada de proposito, nao um pathfinder de
 * verdade: o mob pula continuamente e, sempre que fica no ar sobre um vazio,
 * um bloco aparece embaixo dos seus pes. Isso o faz "pilar" para cima com o
 * tempo (util quando o Nexus esta acima) e tambem preenche buracos no chao a
 * frente (util quando ha um vao horizontal no caminho). So entra em acao
 * depois de {@code STUCK_THRESHOLD} ciclos sem progresso (ver
 * {@link AttackNexusGoal}) e para depois de um teto de blocos, para nao virar
 * uma torre infinita.
 *
 * <p>So reivindica {@link Control#JUMP}: o {@link AttackNexusGoal} continua
 * dono do controle de movimento e do olhar ao mesmo tempo, entao o mob segue
 * tentando andar em direcao ao Nexus enquanto este goal cuida so do pulo e da
 * construcao.
 */
public class BridgeToNexusGoal extends Goal {

    private static final int STUCK_THRESHOLD = 3;
    private static final int MAX_BLOCKS_PLACED = 48;
    private static final int PLACE_INTERVAL = 6;

    private final MobEntity mob;
    private final InvaderData data;

    private int blocksPlaced;
    private int placeCooldown;

    public BridgeToNexusGoal(MobEntity mob) {
        this.mob = mob;
        this.data = InvaderAccess.of(mob);
        setControls(EnumSet.of(Control.JUMP));
    }

    @Override
    public boolean canStart() {
        if (data == null || !data.isInvader() || data.getNexusPos() == null) {
            return false;
        }
        if (!(mob.getWorld() instanceof ServerWorld world) || NexusManager.getData(world).isGameOver()) {
            return false;
        }
        if (data.getStuckTicks() < STUCK_THRESHOLD) {
            return false;
        }
        // So faz sentido quando o Nexus esta visivelmente acima do mob: e o
        // caso descrito como "lugar mais alto ou suspenso no ar".
        return data.getNexusPos().getY() - mob.getBlockY() >= 2;
    }

    @Override
    public boolean shouldContinue() {
        return canStart() && blocksPlaced < MAX_BLOCKS_PLACED;
    }

    @Override
    public void start() {
        blocksPlaced = 0;
        placeCooldown = 0;
    }

    @Override
    public void stop() {
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
        // Pula sem parar: e o pulo que da o impulso para o bloco colocado
        // embaixo virar um degrau.
        mob.getJumpControl().setActive();

        if (placeCooldown-- > 0) {
            return;
        }
        BlockPos below = mob.getBlockPos().down();
        if (!world.getBlockState(below).isAir() || mob.isOnGround()) {
            return;
        }
        world.setBlockState(below, Blocks.COBBLESTONE.getDefaultState());
        blocksPlaced++;
        placeCooldown = PLACE_INTERVAL;
        world.playSound(null, below, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.HOSTILE, 1.0F, 1.0F);
    }
}
