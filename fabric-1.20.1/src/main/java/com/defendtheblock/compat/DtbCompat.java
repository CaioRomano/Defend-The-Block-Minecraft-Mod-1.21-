package com.defendtheblock.compat;

import com.defendtheblock.DefendTheBlock;
import com.defendtheblock.invasion.InvasionData;
import com.defendtheblock.network.InvasionSyncData;
import com.defendtheblock.network.TurretStatsData;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Camada de compatibilidade para <b>Minecraft 1.20.1</b>.
 *
 * <p>Todo o codigo compartilhado em {@code common/} fala apenas com esta classe
 * quando precisa de alguma API que mudou entre 1.20.1 e 1.21 (identificadores,
 * pacotes, encantamentos, componentes de item, persistent state...).
 */
public final class DtbCompat {

    public static final Identifier INVASION_SYNC = new Identifier(DefendTheBlock.MOD_ID, "invasion_sync");
    public static final Identifier TURRET_STATS = new Identifier(DefendTheBlock.MOD_ID, "turret_stats");

    /** No 1.21 este som virou {@code RegistryEntry<SoundEvent>}; aqui ainda e direto. */
    public static final SoundEvent CROSSBOW_LOADED = SoundEvents.ITEM_CROSSBOW_LOADING_END;

    private DtbCompat() {
    }

    // ---------------------------------------------------------- identificadores

    public static Identifier id(String path) {
        return new Identifier(DefendTheBlock.MOD_ID, path);
    }

    // ---------------------------------------------------------------- entidades

    public static <T extends Entity> EntityType<T> buildEntityType(EntityType.EntityFactory<T> factory,
                                                                   SpawnGroup group, float width, float height,
                                                                   int trackRangeChunks, String id) {
        return FabricEntityTypeBuilder.<T>create(group, factory)
                .dimensions(EntityDimensions.fixed(width, height))
                .trackRangeChunks(trackRangeChunks)
                .build();
    }

    public static void initializeMob(ServerWorld world, MobEntity mob, BlockPos pos) {
        mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null, null);
    }

    // --------------------------------------------------------- estado do mundo

    public static InvasionData getInvasionData(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                InvasionPersistentState::fromNbt,
                InvasionPersistentState::new,
                InvasionPersistentState.KEY).data;
    }

    // ------------------------------------------------------------ encantamentos

    public static void enchantRandomly(ServerWorld world, ItemStack stack, int power) {
        EnchantmentHelper.enchant(world.getRandom(), stack, power, false);
    }

    /** Encantamentos ativos do item, ou os guardados quando ele e um livro. */
    public static Map<String, Integer> readEnchantments(ItemStack stack) {
        Map<String, Integer> result = new LinkedHashMap<>();
        NbtList list = stack.isOf(Items.ENCHANTED_BOOK)
                ? EnchantedBookItem.getEnchantmentNbt(stack)
                : stack.getEnchantments();
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            Identifier key = Identifier.tryParse(entry.getString("id"));
            if (key != null) {
                result.put(key.toString(), entry.getInt("lvl"));
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ flechas

    public static PersistentProjectileEntity createArrow(World world, LivingEntity owner, ItemStack ammo) {
        if (ammo.isOf(Items.SPECTRAL_ARROW)) {
            return new SpectralArrowEntity(world, owner);
        }
        ArrowEntity arrow = new ArrowEntity(world, owner);
        arrow.initFromStack(ammo);
        return arrow;
    }

    /** Impacto (Punch): repulsao extra da flecha. */
    public static void applyPunch(PersistentProjectileEntity arrow, int level) {
        if (level > 0) {
            arrow.setPunch(level);
        }
    }

    /** Perfuracao (Piercing): quantos alvos a flecha atravessa. */
    public static void applyPiercing(PersistentProjectileEntity arrow, int level) {
        if (level > 0) {
            arrow.setPierceLevel((byte) level);
        }
    }

    // ------------------------------------------------------------ itens em nbt

    public static NbtCompound writeStack(Entity context, ItemStack stack) {
        return stack.writeNbt(new NbtCompound());
    }

    public static ItemStack readStack(Entity context, NbtCompound nbt) {
        return ItemStack.fromNbt(nbt);
    }

    // ------------------------------------------------------------------ efeitos

    public static void applySlowness(LivingEntity target, int duration, int amplifier) {
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, amplifier));
    }

    /**
     * Faz a flecha carregar um efeito, como uma flecha com pocao.
     *
     * <p>Aqui no 1.20.1 {@code StatusEffects.X} e um {@code StatusEffect}
     * direto; no 1.21 e {@code RegistryEntry<StatusEffect>}. Por isso quem
     * chama passa so o <b>nome</b> do efeito e cada versao monta o seu.
     *
     * @param effect nome curto vindo de {@code TurretModifier#arrowEffect()}
     */
    public static void applyArrowEffect(PersistentProjectileEntity arrow, String effect,
                                        int duration, int amplifier) {
        if (!(arrow instanceof ArrowEntity tipped)) {
            // Flecha espectral nao carrega efeito: ela tem o brilho dela.
            return;
        }
        StatusEffect type = switch (effect) {
            case "slowness" -> StatusEffects.SLOWNESS;
            case "poison" -> StatusEffects.POISON;
            default -> null;
        };
        if (type != null) {
            tipped.addEffect(new StatusEffectInstance(type, duration, Math.max(0, amplifier)));
        }
    }

    // ------------------------------------------------------------------- rede

    public static void registerServerNetworking() {
        // Em 1.20.1 pacotes S2C nao precisam de registro previo.
    }

    public static void sendInvasionSync(ServerPlayerEntity player, InvasionSyncData data) {
        PacketByteBuf buf = PacketByteBufs.create();
        data.write(buf);
        ServerPlayNetworking.send(player, INVASION_SYNC, buf);
    }

    public static void sendTurretStats(ServerPlayerEntity player, TurretStatsData data) {
        PacketByteBuf buf = PacketByteBufs.create();
        data.write(buf);
        ServerPlayNetworking.send(player, TURRET_STATS, buf);
    }
}
