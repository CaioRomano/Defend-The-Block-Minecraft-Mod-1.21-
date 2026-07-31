package com.defendtheblock.registry;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.item.ArrowTurretItem;
import com.defendtheblock.item.GatheringTotemItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

public final class ModItems {

    public static final BlockItem NEXUS_BLOCK = new BlockItem(ModBlocks.NEXUS_BLOCK,
            new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof());

    public static final Item ARROW_TURRET = new ArrowTurretItem(new Item.Settings().maxCount(16));

    public static final Item GATHERING_TOTEM = new GatheringTotemItem(
            new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));

    private ModItems() {
    }

    public static void register() {
        Registry.register(Registries.ITEM, DtbCompat.id("nexus_block"), NEXUS_BLOCK);
        Registry.register(Registries.ITEM, DtbCompat.id("arrow_turret"), ARROW_TURRET);
        Registry.register(Registries.ITEM, DtbCompat.id("gathering_totem"), GATHERING_TOTEM);

        // Faz Block#asItem() devolver o BlockItem certo (pick block, drops...).
        Item.BLOCK_ITEMS.put(ModBlocks.NEXUS_BLOCK, NEXUS_BLOCK);

        // Os itens vivem so na aba propria do mod (ModItemGroups), nao
        // espalhados pelas abas vanilla de Combate/Blocos Funcionais.
    }
}
