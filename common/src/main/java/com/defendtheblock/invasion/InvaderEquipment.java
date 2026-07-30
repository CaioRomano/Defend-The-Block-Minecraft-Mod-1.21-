package com.defendtheblock.invasion;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.entity.invader.InvaderAbility;
import com.defendtheblock.entity.invader.InvaderData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;

/**
 * Sorteia armadura e arma dos invasores humanoides.
 *
 * <p>A qualidade sobe conforme as noites passam: leather/madeira na primeira
 * invasao, netherite/diamante nas ultimas, e a partir da invasao 4 as pecas
 * comecam a vir encantadas.
 */
public final class InvaderEquipment {

    private static final Item[][] ARMOR = {
            {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS},
            {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS},
            {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS},
            {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
            {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS}};

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static final Item[] SWORDS = {
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD};

    private static final Item[] AXES = {
            Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE};

    private static final Item[] PICKAXES = {
            Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE,
            Items.NETHERITE_PICKAXE};

    private InvaderEquipment() {
    }

    public static int tierForWave(int wave) {
        return Math.max(0, Math.min(4, (wave - 1) / 3));
    }

    public static void equip(ServerWorld world, MobEntity mob, InvaderData data, int wave, Random random) {
        int tier = tierForWave(wave);
        double armorChance = Math.min(0.90D, 0.12D + wave * 0.06D);
        double enchantChance = wave < 4 ? 0.0D : Math.min(0.65D, (wave - 3) * 0.07D);
        int enchantPower = 5 + wave * 2;

        boolean humanoid = mob instanceof ZombieEntity || mob instanceof AbstractSkeletonEntity;
        if (humanoid) {
            for (int i = 0; i < ARMOR_SLOTS.length; i++) {
                if (random.nextDouble() > armorChance) {
                    continue;
                }
                // Peca as vezes vem um nivel abaixo, para a onda nao ficar uniforme.
                int pieceTier = Math.max(0, tier - (random.nextInt(4) == 0 ? 1 : 0));
                ItemStack piece = new ItemStack(ARMOR[pieceTier][i]);
                maybeEnchant(world, piece, random, enchantChance, enchantPower);
                mob.equipStack(ARMOR_SLOTS[i], piece);
                mob.setEquipmentDropChance(ARMOR_SLOTS[i], 0.03F);
            }
        }

        ItemStack mainHand = mainHandFor(mob, data, tier, random);
        if (!mainHand.isEmpty()) {
            maybeEnchant(world, mainHand, random, enchantChance, enchantPower);
            mob.equipStack(EquipmentSlot.MAINHAND, mainHand);
            mob.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.03F);
        }

        ItemStack offHand = offHandFor(data);
        if (!offHand.isEmpty()) {
            mob.equipStack(EquipmentSlot.OFFHAND, offHand);
            mob.setEquipmentDropChance(EquipmentSlot.OFFHAND, 0.0F);
        }

        // O invasor nao troca de equipamento no meio da invasao.
        mob.setCanPickUpLoot(false);

        // Esqueletos escolhem entre a IA de arco e a de corpo a corpo pelo item
        // na mao, entao precisam reavaliar depois que trocamos a arma.
        if (mob instanceof AbstractSkeletonEntity skeleton) {
            skeleton.updateAttackType();
        }
    }

    private static ItemStack mainHandFor(MobEntity mob, InvaderData data, int tier, Random random) {
        // Zumbi mineiro sempre carrega a picareta: e a ferramenta da habilidade.
        if (data.hasAbility(InvaderAbility.PICKAXE_MINER)) {
            return new ItemStack(PICKAXES[tier]);
        }
        if (mob instanceof WitherSkeletonEntity) {
            // Wither skeleton e corpo a corpo por natureza: mantemos a espada.
            return new ItemStack(SWORDS[Math.max(1, tier)]);
        }
        if (mob instanceof AbstractSkeletonEntity) {
            // Esqueletos ja nascem com arco; so trocamos por espada nos tiers altos
            // de vez em quando, para variar a onda.
            if (tier >= 3 && random.nextInt(5) == 0) {
                return new ItemStack(SWORDS[tier]);
            }
            return new ItemStack(Items.BOW);
        }
        if (mob instanceof ZombieEntity) {
            if (random.nextInt(4) == 0) {
                return new ItemStack(AXES[tier]);
            }
            if (random.nextDouble() < 0.55D) {
                return new ItemStack(SWORDS[tier]);
            }
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack offHandFor(InvaderData data) {
        if (data.hasAbility(InvaderAbility.LADDER_BUILDER)) {
            return new ItemStack(Items.LADDER, 16);
        }
        if (data.hasAbility(InvaderAbility.TNT_SAPPER)) {
            return new ItemStack(Items.TNT, 3);
        }
        return ItemStack.EMPTY;
    }

    private static void maybeEnchant(ServerWorld world, ItemStack stack, Random random, double chance, int power) {
        if (stack.isEmpty() || chance <= 0.0D || random.nextDouble() > chance) {
            return;
        }
        DtbCompat.enchantRandomly(world, stack, power);
    }
}
