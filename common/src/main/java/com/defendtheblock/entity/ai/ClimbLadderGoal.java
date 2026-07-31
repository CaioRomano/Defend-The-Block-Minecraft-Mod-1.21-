package com.defendtheblock.entity.ai;

import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * Faz <b>qualquer</b> invasor usar escadas.
 *
 * <p>A navegacao vanilla praticamente nunca escolhe subir uma escada, entao aqui
 * o mob procura um bloco escalavel proximo, anda ate ele e ganha empurrao
 * vertical enquanto estiver agarrado. E o que permite a horda inteira usar as
 * escadas montadas pelos zumbis carpinteiros.
 */
public class ClimbLadderGoal extends Goal {

    private static final int SEARCH_RADIUS = 3;
    private static final int SEARCH_COOLDOWN = 20;
    private static final double CLIMB_SPEED = 0.19D;

    private final MobEntity mob;
    private final InvaderData data;
    private final double speed;

    private BlockPos ladder;
    private int repathTimer;
    private int ticksClimbing;
    private int searchCooldown;

    public ClimbLadderGoal(MobEntity mob, double speed) {
        this.mob = mob;
        this.data = InvaderAccess.of(mob);
        this.speed = speed;
        setControls(EnumSet.of(Control.MOVE, Control.JUMP));
    }

    @Override
    public boolean canStart() {
        if (data == null || !data.isInvader() || data.getNexusPos() == null) {
            return false;
        }
        // So vale a pena subir se o mob esta travado ou se o alvo esta acima dele.
        boolean blocked = data.getObstacle() != null;
        boolean nexusAbove = data.getNexusPos().getY() > mob.getBlockY() + 1;
        if (!blocked && !nexusAbove) {
            return false;
        }
        // A varredura de blocos e cara, entao so acontece de tempos em tempos.
        if (searchCooldown-- > 0) {
            return false;
        }
        searchCooldown = SEARCH_COOLDOWN;
        ladder = findLadder();
        return ladder != null;
    }

    @Override
    public boolean shouldContinue() {
        if (ladder == null || !NexusPathing.isClimbable(mob.getWorld(), ladder)) {
            return false;
        }
        // Desiste se ficou muito tempo pendurado sem sair do lugar.
        return ticksClimbing < 200 && mob.squaredDistanceTo(Vec3d.ofCenter(ladder)) < 64.0D;
    }

    @Override
    public void start() {
        repathTimer = 0;
        ticksClimbing = 0;
    }

    @Override
    public void stop() {
        ladder = null;
        mob.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticksClimbing++;
        Vec3d center = Vec3d.ofCenter(ladder);
        double dx = mob.getX() - center.x;
        double dz = mob.getZ() - center.z;
        boolean closeEnoughToGrab = dx * dx + dz * dz < 0.36D;

        // Nao espera passivamente a navegacao vanilla decidir entrar na
        // escada (ela quase nunca escolhe isso sozinha): assim que o mob esta
        // perto o bastante da coluna, forca a escalada mesmo que
        // mob.isClimbing() ainda nao tenha sido setado pela colisao.
        if (mob.isClimbing() || closeEnoughToGrab) {
            climb(center);
            return;
        }

        if (--repathTimer <= 0) {
            repathTimer = 10;
            mob.getNavigation().startMovingTo(center.x, center.y, center.z, speed);
            mob.getMoveControl().moveTo(center.x, center.y, center.z, speed);
        }
    }

    private void climb(Vec3d center) {
        World world = mob.getWorld();
        // Empurra o mob de volta para o centro da coluna enquanto sobe, senao
        // ele desgruda da escada e a escalada para no meio do caminho.
        Vec3d pull = new Vec3d(center.x - mob.getX(), 0.0D, center.z - mob.getZ());
        Vec3d nudge = pull.lengthSquared() > 1.0E-4D ? pull.normalize().multiply(0.1D) : Vec3d.ZERO;

        mob.setVelocity(nudge.x, CLIMB_SPEED, nudge.z);
        mob.velocityModified = true;
        mob.fallDistance = 0.0F;

        // Chegou no topo da escada: joga o mob para dentro, em direcao ao Nexus.
        BlockPos above = mob.getBlockPos().up(2);
        if (!NexusPathing.isClimbable(world, above) && world.getBlockState(above).isAir()) {
            Vec3d toward = NexusPathing.center(data.getNexusPos()).subtract(mob.getPos());
            Vec3d flat = new Vec3d(toward.x, 0.0D, toward.z);
            if (flat.lengthSquared() > 1.0E-4D) {
                flat = flat.normalize().multiply(0.22D);
                mob.setVelocity(flat.x, 0.32D, flat.z);
                mob.velocityModified = true;
            }
        }
    }

    /** Procura o bloco escalavel mais proximo dentro de {@link #SEARCH_RADIUS}. */
    private BlockPos findLadder() {
        World world = mob.getWorld();
        BlockPos origin = mob.getBlockPos();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (!NexusPathing.isClimbable(world, pos)) {
                        continue;
                    }
                    // Escada de um bloco so nao ajuda ninguem.
                    if (!NexusPathing.isClimbable(world, pos.up())) {
                        continue;
                    }
                    double distance = pos.getSquaredDistance(mob.getPos());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos.toImmutable();
                    }
                }
            }
        }
        return best;
    }
}
