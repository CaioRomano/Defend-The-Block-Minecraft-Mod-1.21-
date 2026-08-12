package com.defendtheblock.invasion;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.invader.InvaderAbility;
import com.defendtheblock.entity.invader.InvaderData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
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

    /**
     * Chance de cada peca de equipamento cair quando o invasor morre.
     *
     * <p>Zero quando {@code invadersDropLoot} esta desligado (o padrao): a
     * invasao spawna centenas de mobs por noite, e sem isso o chao em volta do
     * Nexus vira uma montanha de armadura e espada. Isso resolve o equipamento
     * <b>sem nenhum mixin</b> — {@code setEquipmentDropChance} e API publica e
     * estavel, ao contrario de {@code dropEquipment}, cuja assinatura muda
     * entre 1.20.1 e 1.21.1.
     */
    private static float dropChance() {
        return DtbConfig.get().invadersDropLoot ? 0.03F : 0.0F;
    }

    public static void equip(ServerWorld world, MobEntity mob, InvaderData data, int wave, Random random) {
        int tier = tierForWave(wave);
        // Comeca bem baixo e sobe devagar: nas primeiras noites a maioria da
        // horda vem sem armadura nenhuma, que e parte de fazer a noite 1 ser
        // acessivel. Antes comecava em 18% por peca ja na estreia.
        double armorChance = Math.min(0.90D, 0.04D + wave * 0.05D);
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
                mob.setEquipmentDropChance(ARMOR_SLOTS[i], dropChance());
            }
        }

        ItemStack mainHand = mainHandFor(mob, data, tier, random);
        if (!mainHand.isEmpty()) {
            maybeEnchant(world, mainHand, random, enchantChance, enchantPower);
            mob.equipStack(EquipmentSlot.MAINHAND, mainHand);
            mob.setEquipmentDropChance(EquipmentSlot.MAINHAND, dropChance());
        }

        ItemStack offHand = offHandFor(data);
        if (!offHand.isEmpty()) {
            mob.equipStack(EquipmentSlot.OFFHAND, offHand);
            mob.setEquipmentDropChance(EquipmentSlot.OFFHAND, 0.0F);
        }

        scaleStats(mob, wave);
        rollSpeed(mob, wave, random);

        // O invasor nao troca de equipamento no meio da invasao.
        mob.setCanPickUpLoot(false);

        // Esqueletos escolhem entre a IA de arco e a de corpo a corpo pelo item
        // na mao, entao precisam reavaliar depois que trocamos a arma.
        if (mob instanceof AbstractSkeletonEntity skeleton) {
            skeleton.updateAttackType();
        }
    }

    /**
     * Vida e dano do invasor conforme a invasao avanca.
     *
     * <p>Faz par com o teto de invasores vivos: sozinho, o teto so muda
     * <i>quantos</i> mobs aparecem, e uma noite avancada acabaria sendo a
     * noite 1 com mais gente na tela. Aqui o mob tambem fica individualmente
     * mais duro, entao a escalada e nos dois eixos.
     *
     * <p>A vida sobe mais rapido que o dano de proposito: vida a mais alonga a
     * luta (o jogador tem tempo de reagir, a torreta precisa de mais tiros),
     * enquanto dano a mais mata. Dobrar o dano de toda a horda e muito mais
     * violento do que triplicar a vida dela.
     *
     * <p>Como {@code rollSpeed}, mexe no valor base do atributo em vez de usar
     * {@code EntityAttributeModifier}, porque o construtor do modifier mudou
     * entre 1.20.1 (UUID) e 1.21 (Identifier).
     */
    private static void scaleStats(MobEntity mob, int wave) {
        DtbConfig config = DtbConfig.get();
        int steps = Math.max(0, wave - 1);

        double healthFactor = Math.min(Math.max(1.0D, config.invaderHealthMultiplierMax),
                1.0D + steps * config.invaderHealthPerWave);
        EntityAttributeInstance health = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health != null && healthFactor > 1.0D) {
            health.setBaseValue(health.getBaseValue() * healthFactor);
            // Curar depois de subir o teto: sem isto o mob nasce com a vida
            // antiga e a barra ja aparece pela metade.
            mob.setHealth(mob.getMaxHealth());
        }

        double damageFactor = Math.min(Math.max(1.0D, config.invaderDamageMultiplierMax),
                1.0D + steps * config.invaderDamagePerWave);
        // Creeper e ghast nao tem este atributo: o dano deles e a explosao e a
        // bola de fogo, entao aqui simplesmente nao ha o que escalar.
        EntityAttributeInstance damage = mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (damage != null && damageFactor > 1.0D) {
            damage.setBaseValue(damage.getBaseValue() * damageFactor);
        }
    }

    /**
     * Sorteia a velocidade do zumbi: um pouco mais lento, normal, ou um pouco
     * mais rapido que o padrao da especie.
     *
     * <p>A chance de sair rapido <b>cresce a cada invasao</b> (ate um teto), o
     * que aperta o jogo com o tempo sem precisar de mais mobs na tela: a mesma
     * quantidade simplesmente chega antes. A horda tambem deixa de andar em
     * bloco uniforme — os rapidos abrem na frente, os lentos ficam para tras.
     *
     * <p>Mexe direto no valor base do atributo em vez de usar
     * {@code EntityAttributeModifier} de proposito: o construtor do modifier
     * mudou entre 1.20.1 (UUID) e 1.21 (Identifier), enquanto
     * {@code setBaseValue} tem a mesma assinatura nas duas.
     */
    private static void rollSpeed(MobEntity mob, int wave, Random random) {
        if (!(mob instanceof ZombieEntity)) {
            return;
        }
        EntityAttributeInstance speed = mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }

        DtbConfig config = DtbConfig.get();
        double fastChance = Math.min(config.zombieFastChanceMax,
                config.zombieFastChanceBase + Math.max(0, wave - 1) * config.zombieFastChancePerWave);

        double roll = random.nextDouble();
        double factor;
        if (roll < fastChance) {
            factor = config.zombieFastFactor;
        } else if (roll < fastChance + config.zombieSlowChance) {
            factor = config.zombieSlowFactor;
        } else {
            return;
        }
        speed.setBaseValue(speed.getBaseValue() * Math.max(0.1D, factor));
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
            // Uma unica TNT: ele arremessa uma vez (ThrowTntGoal) ou planta uma
            // vez num obstaculo, e depois vira um zumbi comum.
            return new ItemStack(Items.TNT, 1);
        }
        if (data.hasAbility(InvaderAbility.BLOCK_BUILDER)) {
            return new ItemStack(Items.COBBLESTONE, 64);
        }
        if (data.hasAbility(InvaderAbility.FIRE_STARTER)) {
            return new ItemStack(Items.FLINT_AND_STEEL);
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
