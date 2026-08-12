package com.defendtheblock.client;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.entity.turret.TurretEntity;
import com.defendtheblock.entity.turret.TurretModifier;
import com.defendtheblock.entity.turret.TurretModifiers;
import com.defendtheblock.item.InvaderEggItem;
import com.defendtheblock.item.TurretModuleItem;
import com.defendtheblock.registry.ModItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Map;

/**
 * Texto extra dos tooltips. Fica separado porque o callback de tooltip do Fabric
 * mudou de assinatura entre 1.20.1 e 1.21; cada camada de compat registra o
 * callback da sua versao e chama {@link #append}.
 */
@Environment(EnvType.CLIENT)
public final class TooltipLines {

    private TooltipLines() {
    }

    /** Resumo do estado guardado num item de torreta recolhida, se houver. */
    private static void appendSavedTurret(ItemStack stack, List<Text> lines) {
        NbtCompound saved = DtbCompat.getStackTag(stack, TurretEntity.STACK_TAG);
        if (saved == null) {
            return;
        }
        lines.add(Text.translatable("tooltip.defendtheblock.arrow_turret.saved",
                        Text.translatable("turret.defendtheblock.tier." + saved.getInt("Tier")),
                        Math.round(saved.getFloat("Health")))
                .formatted(Formatting.AQUA));

        TurretModifiers modules = new TurretModifiers();
        modules.readNbt(saved);
        for (Map.Entry<TurretModifier, Integer> entry : modules.installed().entrySet()) {
            lines.add(Text.literal(" • ")
                    .append(Text.translatable(entry.getKey().translationKey()))
                    .append(" " + TurretModifiers.grade(entry.getValue()))
                    .formatted(Formatting.LIGHT_PURPLE));
        }
    }

    public static void append(ItemStack stack, List<Text> lines) {
        if (stack.isOf(ModItems.NEXUS_BLOCK)) {
            lines.add(Text.translatable("tooltip.defendtheblock.nexus_block.1").formatted(Formatting.GRAY));
            lines.add(Text.translatable("tooltip.defendtheblock.nexus_block.2").formatted(Formatting.GRAY));
            lines.add(Text.translatable("tooltip.defendtheblock.nexus_block.3").formatted(Formatting.RED));
        } else if (stack.isOf(ModItems.ARROW_TURRET)) {
            // Uma torreta recolhida carrega nivel, encantamentos e modulos
            // dentro do item. Sem mostrar isso aqui, duas torretas visualmente
            // iguais no inventario poderiam ser uma de madeira crua e uma de
            // esmeralda com dois modulos de grau 3.
            appendSavedTurret(stack, lines);
            lines.add(Text.translatable("tooltip.defendtheblock.arrow_turret.1").formatted(Formatting.GRAY));
            lines.add(Text.translatable("tooltip.defendtheblock.arrow_turret.2").formatted(Formatting.DARK_GRAY));
            lines.add(Text.translatable("tooltip.defendtheblock.arrow_turret.3").formatted(Formatting.DARK_GRAY));
            lines.add(Text.translatable("tooltip.defendtheblock.arrow_turret.4").formatted(Formatting.DARK_GRAY));
            lines.add(Text.translatable("tooltip.defendtheblock.arrow_turret.5").formatted(Formatting.DARK_GRAY));
        } else if (stack.isOf(ModItems.GATHERING_TOTEM)) {
            lines.add(Text.translatable("tooltip.defendtheblock.gathering_totem.1").formatted(Formatting.GRAY));
        } else if (stack.getItem() instanceof InvaderEggItem) {
            lines.add(Text.translatable("tooltip.defendtheblock.invader_egg.1").formatted(Formatting.GRAY));
            lines.add(Text.translatable("tooltip.defendtheblock.invader_egg.2").formatted(Formatting.DARK_GRAY));
        } else if (stack.getItem() instanceof TurretModuleItem module) {
            // A descricao do efeito e por modulo; as duas linhas seguintes valem
            // para todos e explicam as duas regras que nao sao obvias: o teto de
            // dois tipos e o fato de o grau subir reaplicando o mesmo livro.
            lines.add(Text.translatable("tooltip." + module.modifier().translationKey())
                    .formatted(Formatting.GRAY));
            lines.add(Text.translatable("tooltip.defendtheblock.turret_module.grade",
                    module.modifier().maxGrade()).formatted(Formatting.DARK_GRAY));
            lines.add(Text.translatable("tooltip.defendtheblock.turret_module.slots",
                    TurretModifiers.MAX_SLOTS).formatted(Formatting.DARK_GRAY));
        }
    }
}
