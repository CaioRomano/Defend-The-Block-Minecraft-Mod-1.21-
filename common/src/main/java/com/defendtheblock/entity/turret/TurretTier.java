package com.defendtheblock.entity.turret;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * Os cinco niveis da torreta. Cada material aplicado sobe um nivel e melhora
 * dano, alcance, vida e tempo de recarga ao mesmo tempo.
 *
 * <p>A quantidade de material cai conforme o material fica mais raro: ferro (o
 * mais facil de conseguir em quantidade) exige mais unidades, esmeralda (o mais
 * raro) exige menos.
 *
 * @param damage       dano base de cada flecha
 * @param range        alcance de tiro, em blocos
 * @param reload       ticks entre dois tiros
 * @param maxHealth    vida da torreta
 * @param maxAmmo      capacidade do carregador
 * @param upgrade      item necessario para chegar neste nivel (null no nivel base)
 * @param upgradeCount quantas unidades desse item sao necessarias
 */
public record TurretTier(double damage, double range, int reload, float maxHealth, int maxAmmo,
                         Item upgrade, int upgradeCount) {

    // O alcance sobe bastante por nivel de proposito: e o atributo que muda o
    // *papel* da torreta no mapa. Uma torreta de madeira cobre so o entorno
    // imediato do Nexus; uma de esmeralda vira artilharia, cobrindo boa parte
    // da area de ativacao da invasao. Note que o alcance e medido no plano
    // horizontal (ver TurretEntity#horizontalSquaredDistanceTo), entao esses
    // numeros valem por igual no chao ou no alto de uma torre.
    public static final TurretTier[] TIERS = {
            new TurretTier(2.0D, 14.0D, 40, 20.0F, 64, null, 0),
            new TurretTier(3.0D, 21.0D, 32, 30.0F, 96, Items.IRON_INGOT, 20),
            new TurretTier(4.0D, 28.0D, 24, 40.0F, 128, Items.GOLD_INGOT, 7),
            new TurretTier(5.5D, 36.0D, 16, 55.0F, 192, Items.DIAMOND, 4),
            new TurretTier(7.0D, 46.0D, 10, 75.0F, 256, Items.EMERALD, 2)};

    public static final int MAX_TIER = TIERS.length - 1;

    public static TurretTier of(int tier) {
        return TIERS[Math.max(0, Math.min(MAX_TIER, tier))];
    }

    /** Item que leva do nivel informado para o proximo, ou null se ja e o maximo. */
    public static Item nextUpgradeItem(int currentTier) {
        int next = currentTier + 1;
        return next > MAX_TIER ? null : TIERS[next].upgrade();
    }

    /** Quantas unidades desse item sao necessarias para o proximo nivel. */
    public static int nextUpgradeCount(int currentTier) {
        int next = currentTier + 1;
        return next > MAX_TIER ? 0 : TIERS[next].upgradeCount();
    }

    /**
     * Material usado para reparar a torreta neste nivel: o mesmo item que a
     * trouxe ate aqui (nivel madeira usa o mesmo material do nivel ferro, ja
     * que madeira nao tem item de upgrade proprio).
     */
    public static Item repairItem(int currentTier) {
        return TIERS[Math.max(1, Math.min(MAX_TIER, currentTier))].upgrade();
    }
}
