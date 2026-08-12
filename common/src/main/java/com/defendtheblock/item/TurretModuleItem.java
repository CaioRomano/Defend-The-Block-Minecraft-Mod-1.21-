package com.defendtheblock.item;

import com.defendtheblock.entity.turret.TurretModifier;
import net.minecraft.item.Item;

/**
 * Um livro de modulo da torreta: cada instancia carrega <b>um</b>
 * {@link TurretModifier}, e aplicar o livro numa torreta instala ou sobe o grau
 * daquele modulo (ver {@code TurretEntity#applyModule}).
 *
 * <p>Nao e um {@code EnchantedBookItem} do vanilla de proposito. Encantamento
 * de verdade exigiria registrar {@code Enchantment}s proprios, e o registro de
 * encantamento mudou completamente entre 1.20.1 (registro simples) e 1.21
 * (registry dinamico carregado por datapack) — precisaria de dois caminhos
 * inteiramente diferentes, e a torreta ja le encantamento vanilla por outro
 * caminho. Aqui o livro e so um item comum, e quem entende o efeito e a
 * torreta.
 *
 * <p>Pelo mesmo motivo o brilho de encantado <b>nao</b> e forcado por codigo:
 * {@code Item#hasGlint} existia no 1.20.1 e sumiu no 1.20.5, que passou a
 * controlar isso pelo componente {@code enchantment_glint_override}. Sao dois
 * caminhos incompativeis para um detalhe cosmetico, entao quem faz o livro
 * parecer encantado e a textura.
 */
public class TurretModuleItem extends Item {

    private final TurretModifier modifier;

    public TurretModuleItem(Settings settings, TurretModifier modifier) {
        super(settings);
        this.modifier = modifier;
    }

    public TurretModifier modifier() {
        return modifier;
    }
}
