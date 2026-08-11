package com.defendtheblock.compat;

import com.defendtheblock.DefendTheBlock;
import com.defendtheblock.invasion.InvasionData;
import com.defendtheblock.network.InvasionSyncData;
import com.defendtheblock.network.TurretStatsData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
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
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Camada de compatibilidade para <b>Minecraft 1.21.x</b> (testada contra 1.21.1).
 *
 * <p>Espelha a mesma API publica da versao 1.20.1: o codigo compartilhado em
 * {@code common/} nao sabe em qual versao esta rodando.
 */
public final class DtbCompat {

    /** No 1.21 os sons de besta viraram {@code RegistryEntry<SoundEvent>}. */
    public static final SoundEvent CROSSBOW_LOADED = SoundEvents.ITEM_CROSSBOW_LOADING_END.value();

    private DtbCompat() {
    }

    // ---------------------------------------------------------- identificadores

    public static Identifier id(String path) {
        return Identifier.of(DefendTheBlock.MOD_ID, path);
    }

    // ---------------------------------------------------------------- entidades

    public static <T extends Entity> EntityType<T> buildEntityType(EntityType.EntityFactory<T> factory,
                                                                   SpawnGroup group, float width, float height,
                                                                   int trackRangeChunks, String id) {
        return EntityType.Builder.create(factory, group)
                .dimensions(width, height)
                .maxTrackingRange(trackRangeChunks)
                .build(id);
    }

    public static void initializeMob(ServerWorld world, MobEntity mob, BlockPos pos) {
        mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null);
    }

    // --------------------------------------------------------- estado do mundo

    public static InvasionData getInvasionData(ServerWorld world) {
        return world.getPersistentStateManager()
                .getOrCreate(InvasionPersistentState.type(), InvasionPersistentState.KEY).data;
    }

    // ------------------------------------------------------------ encantamentos

    public static void enchantRandomly(ServerWorld world, ItemStack stack, int power) {
        EnchantmentHelper.enchant(world.getRandom(), stack, power, world.getRegistryManager(), Optional.empty());
    }

    /** Encantamentos ativos do item, ou os guardados quando ele e um livro. */
    public static Map<String, Integer> readEnchantments(ItemStack stack) {
        Map<String, Integer> result = new LinkedHashMap<>();
        ItemEnchantmentsComponent component = stack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT);
        if (component.isEmpty()) {
            component = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        }
        for (RegistryEntry<Enchantment> entry : component.getEnchantments()) {
            int level = component.getLevel(entry);
            entry.getKey().ifPresent(key -> result.put(key.getValue().toString(), level));
        }
        return result;
    }

    // ------------------------------------------------------------------ flechas

    public static PersistentProjectileEntity createArrow(World world, LivingEntity owner, ItemStack ammo) {
        if (ammo.isOf(Items.SPECTRAL_ARROW)) {
            return new SpectralArrowEntity(world, owner, ammo, null);
        }
        return new ArrowEntity(world, owner, ammo, null);
    }

    /**
     * Impacto (Punch) — <b>sem efeito no 1.21</b>.
     *
     * <p>O 1.20.5 tirou {@code setPunch} da flecha: repulsao e perfuracao agora
     * saem dos encantamentos do {@code ItemStack} da arma que disparou, passado
     * no construtor da flecha. A torreta guarda os niveis dela em NBT proprio,
     * nao numa arma, entao aqui isso vira no-op ate a torreta montar um stack de
     * besta encantado para passar como arma.
     */
    public static void applyPunch(PersistentProjectileEntity arrow, int level) {
        // ver javadoc: precisa do caminho de weapon stack no 1.21
    }

    /** Perfuracao (Piercing) — <b>sem efeito no 1.21</b>, mesmo motivo do Punch. */
    public static void applyPiercing(PersistentProjectileEntity arrow, int level) {
        // ver javadoc de applyPunch
    }

    // ------------------------------------------------------------ itens em nbt

    public static NbtCompound writeStack(Entity context, ItemStack stack) {
        return (NbtCompound) stack.encode(context.getRegistryManager());
    }

    public static ItemStack readStack(Entity context, NbtCompound nbt) {
        return ItemStack.fromNbt(context.getRegistryManager(), nbt).orElse(ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------ efeitos

    public static void applySlowness(LivingEntity target, int duration, int amplifier) {
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, amplifier));
    }

    /**
     * Faz a flecha carregar um efeito, como uma flecha com pocao.
     *
     * <p>Aqui no 1.21 {@code StatusEffects.X} e um
     * {@code RegistryEntry<StatusEffect>}; no 1.20.1 e um {@code StatusEffect}
     * direto. Por isso quem chama passa so o <b>nome</b> do efeito e cada
     * versao monta o seu — a mesma divergencia dos sons de besta.
     *
     * @param effect nome curto vindo de {@code TurretModifier#arrowEffect()}
     */
    public static void applyArrowEffect(PersistentProjectileEntity arrow, String effect,
                                        int duration, int amplifier) {
        if (!(arrow instanceof ArrowEntity tipped)) {
            // Flecha espectral nao carrega efeito: ela tem o brilho dela.
            return;
        }
        RegistryEntry<StatusEffect> type = switch (effect) {
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
        PayloadTypeRegistry.playS2C().register(InvasionSyncPayload.ID, InvasionSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TurretStatsPayload.ID, TurretStatsPayload.CODEC);
    }

    public static void sendInvasionSync(ServerPlayerEntity player, InvasionSyncData data) {
        ServerPlayNetworking.send(player, new InvasionSyncPayload(data));
    }

    public static void sendTurretStats(ServerPlayerEntity player, TurretStatsData data) {
        ServerPlayNetworking.send(player, new TurretStatsPayload(data));
    }
}
