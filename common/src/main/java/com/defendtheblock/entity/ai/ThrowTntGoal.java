package com.defendtheblock.entity.ai;

import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/**
 * O zumbi bombardeiro: em vez de plantar a TNT encostado no obstaculo, ele
 * <b>arremessa</b> a banana pelo ar, em arco, como um projetil.
 *
 * <p>Ele carrega <b>uma unica TNT</b>, entao isso acontece uma vez na vida do
 * mob — depois disso ele volta a ser um zumbi comum marchando para o Nexus.
 * O alvo, em ordem: o inimigo que ele estiver perseguindo (jogador ou torreta)
 * ou, se nao houver nenhum, o proprio Nexus.
 *
 * <p>A TNT e acesa no momento do arremesso (pavio ja correndo), entao ela
 * explode logo depois de aterrissar — nao da para simplesmente pegar e
 * devolver.
 */
public class ThrowTntGoal extends Goal {

    /** Alcance maximo do arremesso, em blocos. */
    private static final double THROW_RANGE = 16.0D;
    /** Nao arremessa em cima da propria cara. */
    private static final double MIN_RANGE = 3.0D;
    /** Pavio: tempo de voo mais uma folga curta depois de cair. */
    private static final int FUSE = 55;
    /** Tempo de "preparo" antes de soltar, para dar leitura visual ao jogador. */
    private static final int WINDUP = 25;

    private final MobEntity mob;
    private final InvaderData data;

    private Vec3d aimPoint;
    private int windup;

    public ThrowTntGoal(MobEntity mob) {
        this.mob = mob;
        this.data = InvaderAccess.of(mob);
        setControls(EnumSet.of(Control.LOOK));
    }

    private boolean hasTnt() {
        return mob.getEquippedStack(EquipmentSlot.OFFHAND).isOf(Items.TNT);
    }

    @Override
    public boolean canStart() {
        if (data == null || !data.isInvader() || !hasTnt()) {
            return false;
        }
        if (!(mob.getWorld() instanceof ServerWorld)) {
            return false;
        }
        aimPoint = findAimPoint();
        return aimPoint != null;
    }

    @Override
    public boolean shouldContinue() {
        return aimPoint != null && hasTnt();
    }

    @Override
    public void start() {
        windup = WINDUP;
    }

    @Override
    public void stop() {
        aimPoint = null;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (aimPoint == null || !(mob.getWorld() instanceof ServerWorld world)) {
            return;
        }
        mob.getNavigation().stop();
        mob.getLookControl().lookAt(aimPoint.x, aimPoint.y, aimPoint.z);

        if (--windup > 0) {
            return;
        }
        throwTnt(world);
        aimPoint = null;
    }

    /** O alvo perseguido, se houver; senao o proprio Nexus. Null se nada estiver no alcance. */
    private Vec3d findAimPoint() {
        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive() && inRange(target.getPos())) {
            return target.getPos().add(0.0D, 0.5D, 0.0D);
        }
        BlockPos nexus = data.getNexusPos();
        if (nexus != null) {
            Vec3d center = NexusPathing.center(nexus);
            if (inRange(center)) {
                return center;
            }
        }
        return null;
    }

    private boolean inRange(Vec3d point) {
        double distance = mob.getPos().squaredDistanceTo(point);
        return distance <= THROW_RANGE * THROW_RANGE && distance >= MIN_RANGE * MIN_RANGE;
    }

    private void throwTnt(ServerWorld world) {
        Vec3d from = new Vec3d(mob.getX(), mob.getEyeY() - 0.1D, mob.getZ());
        TntEntity tnt = new TntEntity(world, from.x, from.y, from.z, mob);
        tnt.setFuse(FUSE);

        // Arco balistico simples: aponta para o alvo e acrescenta altura
        // proporcional a distancia, do mesmo jeito que a flecha da torreta.
        double dx = aimPoint.x - from.x;
        double dy = aimPoint.y - from.y;
        double dz = aimPoint.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        Vec3d velocity = new Vec3d(dx, dy + horizontal * 0.28D, dz).normalize()
                .multiply(Math.min(1.05D, 0.32D + horizontal * 0.055D));
        tnt.setVelocity(velocity);
        tnt.velocityModified = true;

        world.spawnEntity(tnt);
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.HOSTILE, 1.0F, 1.0F);
        mob.swingHand(Hand.OFF_HAND);

        // Uma TNT por zumbi: o slot fica vazio e a goal nunca mais roda.
        ItemStack offHand = mob.getEquippedStack(EquipmentSlot.OFFHAND);
        offHand.decrement(1);
        mob.equipStack(EquipmentSlot.OFFHAND, offHand);
    }
}
