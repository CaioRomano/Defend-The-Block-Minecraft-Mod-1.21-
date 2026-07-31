package com.defendtheblock.entity.turret;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.ai.TurretShootGoal;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.registry.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A torreta de flechas.
 *
 * <p>E uma entidade (nao um bloco) com aparencia de besta montada num trepe: ela
 * gira para o mob mais proximo, consome flechas de verdade e aceita melhorias de
 * material e livros encantados.
 *
 * <p>Nota de implementacao: nada aqui usa {@code DataTracker} de proposito,
 * porque {@code initDataTracker} mudou de assinatura entre 1.20.1 e 1.21. O
 * estado visivel para o cliente (nivel e municao) viaja no nome customizado, que
 * o vanilla ja sincroniza sozinho.
 */
public class TurretEntity extends MobEntity {

    /** Encantamentos aceitos e o nivel maximo de cada um. */
    public static final Map<String, Integer> SUPPORTED_ENCHANTMENTS = new LinkedHashMap<>();

    static {
        SUPPORTED_ENCHANTMENTS.put("minecraft:power", 5);
        SUPPORTED_ENCHANTMENTS.put("minecraft:punch", 2);
        SUPPORTED_ENCHANTMENTS.put("minecraft:flame", 1);
        SUPPORTED_ENCHANTMENTS.put("minecraft:piercing", 4);
        SUPPORTED_ENCHANTMENTS.put("minecraft:multishot", 1);
        SUPPORTED_ENCHANTMENTS.put("minecraft:quick_charge", 3);
    }

    private int tier;
    private int ammo;
    private ItemStack ammoStack = ItemStack.EMPTY;

    private int power;
    private int punch;
    private int flame;
    private int piercing;
    private int multishot;
    private int quickCharge;

    private int cooldown;

    public TurretEntity(EntityType<? extends TurretEntity> type, World world) {
        super(type, world);
        setPersistent();
        setCustomNameVisible(true);
    }

    public static DefaultAttributeContainer.Builder createTurretAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new TurretShootGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this, MobEntity.class, 10, true, false,
                entity -> entity instanceof Monster || InvaderAccess.isInvader(entity)));
    }

    // ------------------------------------------------------------ atributos

    public TurretTier tier() {
        return TurretTier.of(tier);
    }

    public int getTierIndex() {
        return tier;
    }

    public double getRange() {
        return tier().range();
    }

    public double getArrowDamage() {
        double base = tier().damage() * (1.0D + power * 0.25D);
        return base * DtbConfig.get().turretDamageMultiplier;
    }

    public int getReloadTicks() {
        int reload = tier().reload() - quickCharge * 3;
        return Math.max(4, reload);
    }

    public int getAmmo() {
        return ammo;
    }

    public int getMaxAmmo() {
        return tier().maxAmmo();
    }

    public boolean hasAmmo() {
        return !DtbConfig.get().turretConsumesAmmo || ammo > 0;
    }

    public ItemStack getAmmoStack() {
        return ammoStack.isEmpty() ? new ItemStack(Items.ARROW) : ammoStack;
    }

    public int getPunch() {
        return punch;
    }

    public int getFlame() {
        return flame;
    }

    public int getPiercing() {
        return piercing;
    }

    public int getMultishot() {
        return multishot;
    }

    public void consumeAmmo() {
        if (DtbConfig.get().turretConsumesAmmo && ammo > 0) {
            ammo--;
            if (ammo == 0) {
                ammoStack = ItemStack.EMPTY;
            }
            updateDisplayName();
        }
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    // ------------------------------------------------------------ tick / ia

    @Override
    public void tick() {
        super.tick();
        if (cooldown > 0) {
            cooldown--;
        }
        if (!getWorld().isClient && age % 40 == 0 && getCustomName() == null) {
            updateDisplayName();
        }
    }

    @Override
    public void tickMovement() {
        // A torreta e fixa: zeramos qualquer movimento residual mas deixamos a
        // gravidade agir para ela assentar no chao.
        setVelocity(0.0D, getVelocity().y, 0.0D);
        super.tickMovement();
    }

    /** Graus de tolerancia para considerar a besta "apontada" para o alvo. */
    private static final float AIM_TOLERANCE_DEGREES = 3.0F;

    /**
     * Gira suavemente a besta em direcao ao alvo.
     *
     * @return true quando a mira ja esta dentro da tolerancia (pode atirar).
     */
    public boolean aimAt(LivingEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double dy = target.getBodyY(0.5D) - getEyeY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (MathHelper.atan2(dz, dx) * 57.2957763671875D) - 90.0F;
        float targetPitch = (float) (-(MathHelper.atan2(dy, horizontal) * 57.2957763671875D));

        setYaw(approachAngle(getYaw(), targetYaw, 20.0F));
        bodyYaw = getYaw();
        headYaw = getYaw();
        setPitch(approachAngle(getPitch(), targetPitch, 20.0F));

        float yawError = Math.abs(MathHelper.wrapDegrees(targetYaw - getYaw()));
        float pitchError = Math.abs(MathHelper.wrapDegrees(targetPitch - getPitch()));
        return yawError <= AIM_TOLERANCE_DEGREES && pitchError <= AIM_TOLERANCE_DEGREES;
    }

    private static float approachAngle(float current, float target, float maxStep) {
        return current + MathHelper.clamp(MathHelper.wrapDegrees(target - current), -maxStep, maxStep);
    }

    // ------------------------------------------------------------ interacao

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack held = player.getStackInHand(hand);
        if (getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        if (player.isSneaking() && held.isEmpty()) {
            pickUp(player);
            return ActionResult.SUCCESS;
        }
        if (held.getItem() instanceof ArrowItem) {
            return loadAmmo(player, held);
        }
        if (held.isOf(Items.ENCHANTED_BOOK)) {
            return applyBook(player, held);
        }
        if (!held.isEmpty()) {
            return upgrade(player, held);
        }
        reportStatus(player);
        return ActionResult.SUCCESS;
    }

    private ActionResult loadAmmo(PlayerEntity player, ItemStack held) {
        if (ammo >= getMaxAmmo()) {
            player.sendMessage(Text.translatable("turret.defendtheblock.full_ammo"), true);
            return ActionResult.CONSUME;
        }
        // Trocar de tipo de flecha so e permitido com o carregador vazio.
        if (ammo > 0 && getAmmoStack().getItem() != held.getItem()) {
            player.sendMessage(Text.translatable("turret.defendtheblock.full_ammo"), true);
            return ActionResult.CONSUME;
        }

        int space = getMaxAmmo() - ammo;
        int moved = Math.min(space, held.getCount());
        ammo += moved;
        ammoStack = held.copy();
        ammoStack.setCount(1);
        if (!player.getAbilities().creativeMode) {
            held.decrement(moved);
        }

        playSound(DtbCompat.CROSSBOW_LOADED, 1.0F, 1.0F);
        updateDisplayName();
        reportStatus(player);
        return ActionResult.SUCCESS;
    }

    private ActionResult applyBook(PlayerEntity player, ItemStack book) {
        Map<String, Integer> stored = DtbCompat.readEnchantments(book);
        boolean applied = false;

        for (Map.Entry<String, Integer> entry : stored.entrySet()) {
            Integer max = SUPPORTED_ENCHANTMENTS.get(entry.getKey());
            if (max == null) {
                continue;
            }
            int level = Math.min(max, entry.getValue());
            switch (entry.getKey()) {
                case "minecraft:power" -> applied |= raise(level, power, value -> power = value);
                case "minecraft:punch" -> applied |= raise(level, punch, value -> punch = value);
                case "minecraft:flame" -> applied |= raise(level, flame, value -> flame = value);
                case "minecraft:piercing" -> applied |= raise(level, piercing, value -> piercing = value);
                case "minecraft:multishot" -> applied |= raise(level, multishot, value -> multishot = value);
                case "minecraft:quick_charge" -> applied |= raise(level, quickCharge, value -> quickCharge = value);
                default -> {
                }
            }
        }

        if (!applied) {
            player.sendMessage(Text.translatable("turret.defendtheblock.bad_enchantment"), true);
            return ActionResult.CONSUME;
        }
        if (!player.getAbilities().creativeMode) {
            book.decrement(1);
        }
        playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 1.2F);
        player.sendMessage(Text.translatable("turret.defendtheblock.enchanted",
                Text.literal(String.join(", ", stored.keySet()))), false);
        return ActionResult.SUCCESS;
    }

    private interface LevelSetter {
        void set(int value);
    }

    private boolean raise(int level, int current, LevelSetter setter) {
        if (level <= current) {
            return false;
        }
        setter.set(level);
        return true;
    }

    private ActionResult upgrade(PlayerEntity player, ItemStack held) {
        Item needed = TurretTier.nextUpgradeItem(tier);
        if (needed == null) {
            player.sendMessage(Text.translatable("turret.defendtheblock.max_tier"), true);
            return ActionResult.CONSUME;
        }
        int needCount = TurretTier.nextUpgradeCount(tier);
        if (!held.isOf(needed) || held.getCount() < needCount) {
            player.sendMessage(Text.translatable("turret.defendtheblock.wrong_material",
                    needCount, Text.translatable(needed.getTranslationKey())), true);
            return ActionResult.CONSUME;
        }

        tier++;
        applyTierAttributes(true);
        if (!player.getAbilities().creativeMode) {
            held.decrement(needCount);
        }
        playSound(SoundEvents.BLOCK_ANVIL_USE, 1.0F, 1.4F);
        player.sendMessage(Text.translatable("turret.defendtheblock.upgraded", tierName()), false);
        updateDisplayName();
        reportStatus(player);
        return ActionResult.SUCCESS;
    }

    private void pickUp(PlayerEntity player) {
        ItemStack turret = new ItemStack(ModItems.ARROW_TURRET);
        if (!player.getInventory().insertStack(turret)) {
            dropStack(turret);
        }
        if (ammo > 0) {
            ItemStack arrows = getAmmoStack().copy();
            arrows.setCount(ammo);
            if (!player.getInventory().insertStack(arrows)) {
                dropStack(arrows);
            }
        }
        playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
        discard();
    }

    private void reportStatus(PlayerEntity player) {
        player.sendMessage(Text.translatable("turret.defendtheblock.ammo", ammo, getMaxAmmo(),
                getAmmoStack().getName()), false);
        player.sendMessage(Text.translatable("turret.defendtheblock.stats", tierName(),
                String.format("%.1f", getArrowDamage()), (int) getRange(),
                String.format("%.1f", getReloadTicks() / 20.0F)), false);
    }

    public Text tierName() {
        return Text.translatable("turret.defendtheblock.tier." + tier);
    }

    private void applyTierAttributes(boolean heal) {
        EntityAttributeInstance maxHealth = getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(tier().maxHealth());
        }
        if (heal) {
            setHealth(tier().maxHealth());
        }
        ammo = Math.min(ammo, getMaxAmmo());
    }

    private void updateDisplayName() {
        setCustomName(Text.translatable("entity.defendtheblock.arrow_turret")
                .append(" ")
                .append(tierName())
                .append(Text.literal(" [" + ammo + "/" + getMaxAmmo() + "]")
                        .formatted(ammo > 0 ? Formatting.AQUA : Formatting.RED)));
    }

    // -------------------------------------------------------- comportamento

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushAway(net.minecraft.entity.Entity entity) {
        // torreta fixa: nao empurra e nao e empurrada
    }

    @Override
    public void takeKnockback(double strength, double x, double z) {
        // imune a repulsao
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (getWorld() instanceof ServerWorld) {
            dropStack(new ItemStack(ModItems.ARROW_TURRET));
            if (ammo > 0) {
                ItemStack arrows = getAmmoStack().copy();
                arrows.setCount(ammo);
                dropStack(arrows);
            }
        }
    }

    // ----------------------------------------------------------------- nbt

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Tier", tier);
        nbt.putInt("Ammo", ammo);
        nbt.putInt("Power", power);
        nbt.putInt("Punch", punch);
        nbt.putInt("Flame", flame);
        nbt.putInt("Piercing", piercing);
        nbt.putInt("Multishot", multishot);
        nbt.putInt("QuickCharge", quickCharge);
        if (!ammoStack.isEmpty()) {
            nbt.put("AmmoStack", DtbCompat.writeStack(this, ammoStack));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        tier = MathHelper.clamp(nbt.getInt("Tier"), 0, TurretTier.MAX_TIER);
        ammo = nbt.getInt("Ammo");
        power = nbt.getInt("Power");
        punch = nbt.getInt("Punch");
        flame = nbt.getInt("Flame");
        piercing = nbt.getInt("Piercing");
        multishot = nbt.getInt("Multishot");
        quickCharge = nbt.getInt("QuickCharge");
        if (nbt.contains("AmmoStack")) {
            ammoStack = DtbCompat.readStack(this, nbt.getCompound("AmmoStack"));
        }
        applyTierAttributes(false);
        updateDisplayName();
    }
}
