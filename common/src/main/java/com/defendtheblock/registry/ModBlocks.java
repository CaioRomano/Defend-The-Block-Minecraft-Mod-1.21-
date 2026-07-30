package com.defendtheblock.registry;

import com.defendtheblock.block.NexusBlock;
import com.defendtheblock.compat.DtbCompat;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {

    /**
     * Dureza -1 e resistencia a explosao gigante deixam o Nexus indestrutivel
     * pelas vias normais: quem tira vida dele e o {@code NexusManager}, chamado
     * pelas IAs de invasao.
     */
    public static final Block NEXUS_BLOCK = new NexusBlock(AbstractBlock.Settings.create()
            .strength(-1.0F, 3600000.0F)
            .luminance(state -> 10)
            .sounds(BlockSoundGroup.AMETHYST_BLOCK)
            .dropsNothing());

    private ModBlocks() {
    }

    public static void register() {
        Registry.register(Registries.BLOCK, DtbCompat.id("nexus_block"), NEXUS_BLOCK);
    }
}
