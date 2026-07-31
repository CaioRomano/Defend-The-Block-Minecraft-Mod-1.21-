package com.defendtheblock.registry;

import com.defendtheblock.compat.DtbCompat;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

/**
 * A aba propria do mod na tela de inventario/criativo, contendo so os itens do
 * Defend The Block — em vez de deixa-los espalhados pelas abas vanilla de
 * Combate e Blocos Funcionais.
 */
public final class ModItemGroups {

    public static final RegistryKey<ItemGroup> MAIN_KEY =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, DtbCompat.id("main"));

    public static final ItemGroup MAIN = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModBlocks.NEXUS_BLOCK))
            .displayName(Text.translatable("itemGroup.defendtheblock.main"))
            .entries((context, entries) -> {
                entries.add(ModItems.NEXUS_BLOCK);
                entries.add(ModItems.ARROW_TURRET);
                entries.add(ModItems.GATHERING_TOTEM);
                entries.add(ModItems.LADDER_ZOMBIE_EGG);
                entries.add(ModItems.TNT_ZOMBIE_EGG);
                entries.add(ModItems.BUILDER_ZOMBIE_EGG);
                entries.add(ModItems.FIRE_ZOMBIE_EGG);
                entries.add(ModItems.MINER_ZOMBIE_EGG);
            })
            .build();

    private ModItemGroups() {
    }

    public static void register() {
        Registry.register(Registries.ITEM_GROUP, MAIN_KEY, MAIN);
    }
}
