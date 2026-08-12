package com.defendtheblock.registry;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.entity.invader.InvaderAbility;
import com.defendtheblock.entity.turret.TurretModifier;
import com.defendtheblock.item.ArrowTurretItem;
import com.defendtheblock.item.GatheringTotemItem;
import com.defendtheblock.item.InvaderEggItem;
import com.defendtheblock.item.TurretModuleItem;
import net.minecraft.entity.EntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {

    public static final BlockItem NEXUS_BLOCK = new BlockItem(ModBlocks.NEXUS_BLOCK,
            new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof());

    public static final Item ARROW_TURRET = new ArrowTurretItem(new Item.Settings().maxCount(16));

    public static final Item GATHERING_TOTEM = new GatheringTotemItem(
            new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));

    // --- ovos dos invasores especiais (um por habilidade)
    public static final Item LADDER_ZOMBIE_EGG = egg(InvaderAbility.LADDER_BUILDER);
    public static final Item TNT_ZOMBIE_EGG = egg(InvaderAbility.TNT_SAPPER);
    public static final Item BUILDER_ZOMBIE_EGG = egg(InvaderAbility.BLOCK_BUILDER);
    public static final Item FIRE_ZOMBIE_EGG = egg(InvaderAbility.FIRE_STARTER);
    public static final Item MINER_ZOMBIE_EGG = egg(InvaderAbility.PICKAXE_MINER);

    private static Item egg(InvaderAbility ability) {
        return new InvaderEggItem(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON),
                EntityType.ZOMBIE, ability);
    }

    /**
     * Um livro por modulo de torreta, criado direto a partir do enum: acrescentar
     * um modulo novo em {@link TurretModifier} ja cria, registra e poe na aba o
     * item dele, sem tocar em mais nada aqui.
     */
    public static final Map<TurretModifier, Item> TURRET_MODULES = new EnumMap<>(TurretModifier.class);

    static {
        for (TurretModifier modifier : TurretModifier.values()) {
            TURRET_MODULES.put(modifier,
                    new TurretModuleItem(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON), modifier));
        }
    }

    private ModItems() {
    }

    public static void register() {
        Registry.register(Registries.ITEM, DtbCompat.id("nexus_block"), NEXUS_BLOCK);
        Registry.register(Registries.ITEM, DtbCompat.id("arrow_turret"), ARROW_TURRET);
        Registry.register(Registries.ITEM, DtbCompat.id("gathering_totem"), GATHERING_TOTEM);
        Registry.register(Registries.ITEM, DtbCompat.id("ladder_zombie_egg"), LADDER_ZOMBIE_EGG);
        Registry.register(Registries.ITEM, DtbCompat.id("tnt_zombie_egg"), TNT_ZOMBIE_EGG);
        Registry.register(Registries.ITEM, DtbCompat.id("builder_zombie_egg"), BUILDER_ZOMBIE_EGG);
        Registry.register(Registries.ITEM, DtbCompat.id("fire_zombie_egg"), FIRE_ZOMBIE_EGG);
        Registry.register(Registries.ITEM, DtbCompat.id("miner_zombie_egg"), MINER_ZOMBIE_EGG);

        for (Map.Entry<TurretModifier, Item> entry : TURRET_MODULES.entrySet()) {
            Registry.register(Registries.ITEM, DtbCompat.id(entry.getKey().itemId()), entry.getValue());
        }

        // Faz Block#asItem() devolver o BlockItem certo (pick block, drops...).
        Item.BLOCK_ITEMS.put(ModBlocks.NEXUS_BLOCK, NEXUS_BLOCK);

        // Os itens vivem so na aba propria do mod (ModItemGroups), nao
        // espalhados pelas abas vanilla de Combate/Blocos Funcionais.
    }
}
