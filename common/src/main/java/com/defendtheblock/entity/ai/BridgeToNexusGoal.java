package com.defendtheblock.entity.ai;

import com.defendtheblock.entity.invader.InvaderAbility;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import com.defendtheblock.invasion.InvaderBlocks;
import com.defendtheblock.invasion.NexusManager;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

/**
 * A rampa do zumbi construtor: o unico jeito de a horda chegar num Nexus que o
 * pathfinding nao alcanca.
 *
 * <p><b>Quem constroi:</b> so o {@link InvaderAbility#LADDER_BUILDER}. Antes
 * qualquer invasor preso acabava empilhando bloco, e havia ainda um segundo
 * tipo de construtor dedicado. Os dois sumiram: com a horda inteira
 * improvisando rampa, nenhum muro e nenhuma altura significava nada, e a
 * defesa virava decorativa. Agora colocar bloco e privilegio de <b>um</b> mob,
 * e matar esse mob e uma jogada com consequencia visivel.
 *
 * <p><b>Quando constroi:</b> quando o Nexus esta acima e o mob esta travado —
 * o caso do Nexus suspenso no ar, numa torre, ou em qualquer lugar sem caminho
 * de chao. O caso do obstaculo a frente (passar por cima de um muro) e tratado
 * pela {@link BreachObstacleGoal}, que constroi rente ao bloco que atrapalha.
 *
 * <p><b>Como constroi:</b> pula e poe um degrau embaixo dos proprios pes, e
 * cada degrau sai com escada em todos os lados livres (ver
 * {@code InvaderBlocks#placeClimbStep}). E isso que transforma a rampa de um
 * truque pessoal dele numa passagem que a horda inteira usa — ninguem mais
 * sabe construir, mas todo invasor sabe subir escada.
 *
 * <p>So reivindica {@link Control#JUMP}: a {@link AttackNexusGoal} continua
 * dona do movimento e do olhar, entao o mob segue tentando andar para o Nexus
 * enquanto este goal cuida so do pulo e do degrau.
 */
public class BridgeToNexusGoal extends Goal {

    /** Ciclos de travamento antes de comecar a construir. */
    private static final int STUCK_THRESHOLD = 1;
    /** Teto de degraus, para a rampa nao virar uma torre infinita. */
    private static final int MAX_BLOCKS_PLACED = 128;
    /** Ticks entre dois degraus. */
    private static final int PLACE_INTERVAL = 4;

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
        if (!data.hasAbility(InvaderAbility.LADDER_BUILDER)) {
            return false;
        }
        if (!(mob.getWorld() instanceof ServerWorld world) || NexusManager.getData(world).isGameOver()) {
            return false;
        }
        if (data.getStuckTicks() < STUCK_THRESHOLD) {
            return false;
        }
        // So faz sentido quando o Nexus esta visivelmente acima: e o caso do
        // "caminho impossivel de alcancar" (suspenso, torre, plataforma).
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
        // Pede espaco: a rampa exige o mob parado num ponto exato, e a horda
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
        // Pula sem parar: e o pulo que abre o vao embaixo dos pes para o degrau.
        mob.getJumpControl().setActive();

        if (placeCooldown-- > 0) {
            return;
        }
        BlockPos below = mob.getBlockPos().down();
        if (!world.getBlockState(below).isAir() || mob.isOnGround()) {
            return;
        }
        if (InvaderBlocks.placeClimbStep(world, below)) {
            blocksPlaced++;
            placeCooldown = PLACE_INTERVAL;
            world.playSound(null, below, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.HOSTILE, 1.0F, 1.0F);
        }
    }
}
