package com.defendtheblock.client;

import com.defendtheblock.item.InvaderEggItem;
import com.defendtheblock.registry.ModItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Texto extra dos tooltips. Fica separado porque o callback de tooltip do Fabric
 * mudou de assinatura entre 1.20.1 e 1.21; cada camada de compat registra o
 * callback da sua versao e chama {@link #append}.
 */
@Environment(EnvType.CLIENT)
public final class TooltipLines {

    private TooltipLines() {
    }

    public static void append(ItemStack stack, List<Text> lines) {
        if (stack.isOf(ModItems.NEXUS_BLOCK)) {
            lines.add(Text.translatable("tooltip.defendtheblock.nexus_block.1").formatted(Formatting.GRAY));
            lines.add(Text.translatable("tooltip.defendtheblock.nexus_block.2").formatted(Formatting.GRAY));
            lines.add(Text.translatable("tooltip.defendtheblock.nexus_block.3").formatted(Formatting.RED));
        } else if (stack.isOf(ModItems.ARROW_TURRET)) {
            lines.add(Text.translatable("tooltip.defendtheblock.arrow_turret.1").formatted(Formatting.GRAY));
            lines.add(Text.translatable("tooltip.defendtheblock.arrow_turret.2").formatted(Formatting.DARK_GRAY));
            lines.add(Text.translatable("tooltip.defendtheblock.arrow_turret.3").formatted(Formatting.DARK_GRAY));
            lines.add(Text.translatable("tooltip.defendtheblock.arrow_turret.4").formatted(Formatting.DARK_GRAY));
        } else if (stack.isOf(ModItems.GATHERING_TOTEM)) {
            lines.add(Text.translatable("tooltip.defendtheblock.gathering_totem.1").formatted(Formatting.GRAY));
        } else if (stack.getItem() instanceof InvaderEggItem) {
            lines.add(Text.translatable("tooltip.defendtheblock.invader_egg.1").formatted(Formatting.GRAY));
            lines.add(Text.translatable("tooltip.defendtheblock.invader_egg.2").formatted(Formatting.DARK_GRAY));
        }
    }
}
