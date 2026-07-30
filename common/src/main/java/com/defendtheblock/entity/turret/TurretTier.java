package com.defendtheblock.entity.turret;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * Os cinco niveis da torreta. Cada material aplicado sobe um nivel e melhora
 * dano, alcance, vida e tempo de recarga ao mesmo tempo.
 *
 * @param damage    dano base de cada flecha
 * @param range     alcance de tiro, em blocos
 * @param reload    ticks entre dois tiros
 * @param maxHealth vida da torreta
 * @param maxAmmo   capacidade do carregador
 * @param upgrade   item necessario para chegar neste nivel (null no nivel base)
 */
public record TurretTier(double damage, double range, int reload, float maxHealth, int maxAmmo, Item upgrade) {

    public static final TurretTier[] TIERS = {
            new TurretTier(2.0D, 12.0D, 40, 20.0F, 64, null),
            new TurretTier(3.0D, 16.0D, 32, 30.0F, 96, Items.IRON_INGOT),
            new TurretTier(4.0D, 20.0D, 24, 40.0F, 128, Items.GOLD_INGOT),
            new TurretTier(5.5D, 26.0D, 16, 55.0F, 192, Items.DIAMOND),
            new TurretTier(7.0D, 32.0D, 10, 75.0F, 256, Items.EMERALD)};

    public static final int MAX_TIER = TIERS.length - 1;

    public static TurretTier of(int tier) {
        return TIERS[Math.max(0, Math.min(MAX_TIER, tier))];
    }

    /** Item que leva do nivel informado para o proximo, ou null se ja e o maximo. */
    public static Item nextUpgradeItem(int currentTier) {
        int next = currentTier + 1;
        return next > MAX_TIER ? null : TIERS[next].upgrade();
    }
}
