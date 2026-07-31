package com.defendtheblock.entity.ai;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.invader.InvaderAbility;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * IA de arrombamento: entra em acao quando {@link InvaderData#getObstacle()}
 * aponta para um bloco que esta impedindo o mob de chegar ao Nexus.
 *
 * <p>O que ele faz depende da habilidade sorteada:
 * <ul>
 *   <li>{@link InvaderAbility#DOOR_BREACHER} - todo invasor (exceto creeper)
 *       arromba porta fechada, sem precisar de nenhuma outra habilidade;</li>
 *   <li>{@link InvaderAbility#SUICIDE_BREACH} - creeper se explode no obstaculo;</li>
 *   <li>{@link InvaderAbility#TNT_SAPPER} - zumbi planta e acende uma TNT;</li>
 *   <li>{@link InvaderAbility#LADDER_BUILDER} - zumbi monta uma coluna de escadas
 *       (que o resto da horda tambem usa);</li>
 *   <li>{@link InvaderAbility#FIRE_STARTER} - zumbi ateia fogo em obstaculo de
 *       madeira em vez de quebra-lo;</li>
 *   <li>{@link InvaderAbility#PICKAXE_MINER} - zumbi minera o bloco.</li>
 * </ul>
 */
public class BreachObstacleGoal extends Goal {

    private static final double WORK_RANGE = 3.2D;

    private final MobEntity mob;
    private final InvaderData data;
    private final double speed;

    private BlockPos target;
    private int mineTicks;
    private int requiredMineTicks;
    private int approachTimer;

    public BreachObstacleGoal(MobEntity mob, double speed) {
        this.mob = mob;
        this.data = InvaderAccess.of(mob);
        this.speed = speed;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    private boolean hasBreachAbility() {
        return data.hasAbility(InvaderAbility.DOOR_BREACHER)
                || data.hasAbility(InvaderAbility.SUICIDE_BREACH)
                || data.hasAbility(InvaderAbility.TNT_SAPPER)
                || data.hasAbility(InvaderAbility.LADDER_BUILDER)
                || data.hasAbility(InvaderAbility.PICKAXE_MINER)
                || data.hasAbility(InvaderAbility.FIRE_STARTER);
    }

    @Override
    public boolean canStart() {
        if (data == null || !data.isInvader() || !hasBreachAbility()) {
            return false;
        }
        if (data.getBreachCooldown() > 0 || data.getObstacle() == null) {
            return false;
        }
        return mob.getWorld() instanceof ServerWorld;
    }

    @Override
    public boolean shouldContinue() {
        return target != null
                && data.getObstacle() != null
                && !mob.getWorld().getBlockState(target).isAir()
                && mob.squaredDistanceTo(Vec3d.ofCenter(target)) < 64.0D;
    }

    @Override
    public void start() {
        target = data.getObstacle();
        mineTicks = 0;
        approachTimer = 0;
        BlockState state = mob.getWorld().getBlockState(target);
        float hardness = Math.max(0.2F, state.getHardness(mob.getWorld(), target));
        // Porta usa seu proprio ritmo (mais rapido que minerar parede de verdade);
        // ferro (dureza 5) naturalmente demora mais que madeira (dureza 3).
        int ticksPerHardness = state.getBlock() instanceof DoorBlock
                ? DtbConfig.get().doorBreakTicksPerHardness
                : DtbConfig.get().mineTicksPerHardness;
        requiredMineTicks = Math.max(15, Math.min(400, (int) (hardness * ticksPerHardness)));
    }

    @Override
    public void stop() {
        if (mob.getWorld() instanceof ServerWorld world && target != null) {
            world.setBlockBreakingInfo(mob.getId(), target, -1);
        }
        target = null;
        mineTicks = 0;
        mob.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null || !(mob.getWorld() instanceof ServerWorld world)) {
            return;
        }
        Vec3d center = Vec3d.ofCenter(target);
        mob.getLookControl().lookAt(center.x, center.y, center.z);

        if (mob.squaredDistanceTo(center) > WORK_RANGE * WORK_RANGE) {
            if (--approachTimer <= 0) {
                approachTimer = 10;
                mob.getNavigation().startMovingTo(center.x, center.y, center.z, speed);
            }
            return;
        }
        mob.getNavigation().stop();

        if (data.hasAbility(InvaderAbility.DOOR_BREACHER) && world.getBlockState(target).getBlock() instanceof DoorBlock) {
            breakDoor(world);
            return;
        }

        if (data.hasAbility(InvaderAbility.SUICIDE_BREACH) && mob instanceof CreeperEntity creeper) {
            creeper.ignite();
            data.setBreachCooldown(200);
            return;
        }
        if (data.hasAbility(InvaderAbility.TNT_SAPPER) && placeTnt(world)) {
            return;
        }
        if (data.hasAbility(InvaderAbility.LADDER_BUILDER) && buildLadder(world)) {
            return;
        }
        if (data.hasAbility(InvaderAbility.FIRE_STARTER) && igniteWood(world)) {
            return;
        }
        if (data.hasAbility(InvaderAbility.PICKAXE_MINER)) {
            mine(world);
        }
    }

    // ------------------------------------------------------------------ tnt

    private boolean placeTnt(ServerWorld world) {
        ItemStack offHand = mob.getEquippedStack(EquipmentSlot.OFFHAND);
        if (!offHand.isOf(Items.TNT)) {
            return false;
        }
        Vec3d spot = tntSpot(world);
        if (spot == null) {
            return false;
        }

        TntEntity tnt = new TntEntity(world, spot.x, spot.y, spot.z, mob);
        tnt.setFuse(50);
        world.spawnEntity(tnt);
        world.playSound(null, BlockPos.ofFloored(spot), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.HOSTILE, 1.0F, 1.0F);

        mob.swingHand(Hand.OFF_HAND);
        offHand.decrement(1);
        mob.equipStack(EquipmentSlot.OFFHAND, offHand);
        data.setBreachCooldown(400);
        data.setObstacle(null);
        retreat();
        return true;
    }

    /** Um espaco livre encostado no obstaculo, do lado onde o mob esta. */
    private Vec3d tntSpot(World world) {
        Direction towardMob = Direction.getFacing(
                mob.getX() - (target.getX() + 0.5D), 0.0D, mob.getZ() - (target.getZ() + 0.5D));
        BlockPos spot = target.offset(towardMob);
        if (!world.getBlockState(spot).isReplaceable()) {
            spot = target.up();
            if (!world.getBlockState(spot).isReplaceable()) {
                return null;
            }
        }
        return Vec3d.ofBottomCenter(spot);
    }

    /** Depois de acender a TNT o zumbi corre para longe da explosao. */
    private void retreat() {
        Vec3d away = mob.getPos().subtract(Vec3d.ofCenter(target)).normalize().multiply(7.0D);
        mob.getNavigation().startMovingTo(mob.getX() + away.x, mob.getY(), mob.getZ() + away.z, speed * 1.4D);
    }

    // ------------------------------------------------------------- portas

    /**
     * Arromba a porta a base de golpes: todo invasor exceto o creeper faz isso,
     * sem precisar de item nenhum. Porta de ferro (dureza maior) demora mais
     * que porta de madeira, na proporcao real do jogo.
     */
    private void breakDoor(ServerWorld world) {
        BlockState state = world.getBlockState(target);
        if (!(state.getBlock() instanceof DoorBlock)) {
            // A porta ja sumiu (outro invasor arrombou primeiro, por exemplo).
            data.setObstacle(null);
            return;
        }

        mineTicks++;
        if (mineTicks % 5 == 0) {
            mob.swingHand(Hand.MAIN_HAND);
            world.playSound(null, target, state.getSoundGroup().getHitSound(), SoundCategory.HOSTILE, 0.6F, 0.8F);
        }
        int progress = (int) ((mineTicks / (float) requiredMineTicks) * 10.0F);
        world.setBlockBreakingInfo(mob.getId(), target, Math.min(9, progress));

        if (mineTicks >= requiredMineTicks) {
            world.setBlockBreakingInfo(mob.getId(), target, -1);
            // Porta ocupa duas metades (de cima e de baixo): quebra as duas juntas.
            BlockPos otherHalf = state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? target.up() : target.down();
            world.breakBlock(target, false, mob);
            if (world.getBlockState(otherHalf).getBlock() instanceof DoorBlock) {
                world.breakBlock(otherHalf, false, mob);
            }
            data.setObstacle(null);
            data.setBreachCooldown(20);
            target = null;
        }
    }

    // -------------------------------------------------------------- escadas

    private boolean buildLadder(ServerWorld world) {
        ItemStack offHand = mob.getEquippedStack(EquipmentSlot.OFFHAND);
        if (!offHand.isOf(Items.LADDER)) {
            return false;
        }

        Direction towardWall = Direction.getFacing(
                (target.getX() + 0.5D) - mob.getX(), 0.0D, (target.getZ() + 0.5D) - mob.getZ());
        BlockPos column = mob.getBlockPos();
        BlockPos wallBase = column.offset(towardWall);
        boolean hasWall = world.getBlockState(wallBase).isSolidBlock(world, wallBase);

        return hasWall
                ? attachToWall(world, column, towardWall, offHand)
                : buildFreestandingColumn(world, column, towardWall, offHand);
    }

    /** Ha parede de verdade encostada: a escada gruda nela, ate 5 blocos de altura. */
    private boolean attachToWall(ServerWorld world, BlockPos column, Direction towardWall, ItemStack offHand) {
        boolean placedAny = false;
        for (int dy = 0; dy < 5 && !offHand.isEmpty(); dy++) {
            BlockPos pos = column.up(dy);
            if (!world.getBlockState(pos).isReplaceable()) {
                continue;
            }
            BlockPos wall = pos.offset(towardWall);
            if (!world.getBlockState(wall).isSolidBlock(world, wall)) {
                continue;
            }
            placeLadder(world, pos, towardWall);
            offHand.decrement(1);
            placedAny = true;
        }
        return finishLadder(world, column, offHand, placedAny);
    }

    /**
     * Sem nenhuma parede para grudar: o zumbi ergue uma colunazinha de 2
     * blocos ao lado dele mesmo (a "parede" que a escada precisa) e prende as
     * escadas nela — o suficiente para a horda escalar ate um Nexus suspenso
     * mesmo sem nenhuma estrutura pronta por perto.
     */
    private boolean buildFreestandingColumn(ServerWorld world, BlockPos column, Direction towardWall,
                                            ItemStack offHand) {
        boolean placedAny = false;
        for (int dy = 0; dy < 2 && !offHand.isEmpty(); dy++) {
            BlockPos pos = column.up(dy);
            BlockPos wall = pos.offset(towardWall);
            if (!world.getBlockState(pos).isReplaceable() || !world.getBlockState(wall).isReplaceable()) {
                continue;
            }
            world.setBlockState(wall, Blocks.COBBLESTONE.getDefaultState());
            placeLadder(world, pos, towardWall);
            offHand.decrement(1);
            placedAny = true;
        }
        return finishLadder(world, column, offHand, placedAny);
    }

    private void placeLadder(ServerWorld world, BlockPos pos, Direction towardWall) {
        BlockState ladder = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, towardWall.getOpposite());
        world.setBlockState(pos, ladder);
    }

    private boolean finishLadder(ServerWorld world, BlockPos column, ItemStack offHand, boolean placedAny) {
        if (!placedAny) {
            return false;
        }
        mob.equipStack(EquipmentSlot.OFFHAND, offHand);
        mob.swingHand(Hand.OFF_HAND);
        world.playSound(null, column, SoundEvents.BLOCK_LADDER_PLACE, SoundCategory.HOSTILE, 1.0F, 1.0F);
        data.setBreachCooldown(120);
        data.setObstacle(null);
        return true;
    }

    // ------------------------------------------------------------ isqueiro

    /**
     * Zumbi com isqueiro: em vez de quebrar um obstaculo de madeira, ateia
     * fogo nele e deixa o proprio fogo do vanilla se espalhar e consumir o
     * bloco — tambem serve para atrapalhar/incendiar construcoes do jogador.
     */
    private boolean igniteWood(ServerWorld world) {
        BlockState state = world.getBlockState(target);
        boolean wood = state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.PLANKS)
                || state.isIn(BlockTags.WOODEN_DOORS) || state.isIn(BlockTags.WOODEN_FENCES)
                || state.isIn(BlockTags.WOODEN_TRAPDOORS) || state.isIn(BlockTags.WOODEN_STAIRS);
        if (!wood) {
            return false;
        }

        BlockPos above = target.up();
        if (!world.getBlockState(above).isAir()) {
            return false;
        }

        world.setBlockState(above, Blocks.FIRE.getDefaultState());
        mob.swingHand(Hand.MAIN_HAND);
        world.playSound(null, target, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.HOSTILE, 1.0F, 1.0F);
        data.setBreachCooldown(300);
        data.setObstacle(null);
        return true;
    }

    // ------------------------------------------------------------- picareta

    private void mine(ServerWorld world) {
        if (!NexusPathing.isBreakable(world, target)) {
            data.setObstacle(null);
            data.setBreachCooldown(60);
            return;
        }
        BlockState state = world.getBlockState(target);
        if (state.getHardness(world, target) > DtbConfig.get().maxMineHardness) {
            // Bloco duro demais para a picareta: deixa para a TNT ou o creeper.
            data.setObstacle(null);
            data.setBreachCooldown(100);
            return;
        }

        mineTicks++;
        if (mineTicks % 5 == 0) {
            mob.swingHand(Hand.MAIN_HAND);
            world.playSound(null, target, state.getSoundGroup().getHitSound(), SoundCategory.HOSTILE, 0.5F, 0.9F);
        }
        int progress = (int) ((mineTicks / (float) requiredMineTicks) * 10.0F);
        world.setBlockBreakingInfo(mob.getId(), target, Math.min(9, progress));

        if (mineTicks >= requiredMineTicks) {
            world.setBlockBreakingInfo(mob.getId(), target, -1);
            world.breakBlock(target, false, mob);
            data.setObstacle(null);
            data.setBreachCooldown(20);
            target = null;
        }
    }
}
