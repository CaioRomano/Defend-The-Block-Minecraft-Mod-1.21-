package com.defendtheblock.client.render;

import com.defendtheblock.compat.DtbCompat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;

/**
 * Geometria da torreta: um trepe de pedra, um pedestal de ferro e, no topo, uma
 * besta que gira junto com a peca "head".
 *
 * <p>O layout de UV aqui espelha exatamente o de {@code tools/generate_textures.py}.
 * Nao herda {@code EntityModel} de proposito: o metodo {@code render} dessa
 * classe mudou de assinatura entre 1.20.1 e 1.21, entao o renderer desenha as
 * {@code ModelPart} na mao.
 */
@Environment(EnvType.CLIENT)
public final class TurretModel {

    public static final EntityModelLayer LAYER = new EntityModelLayer(DtbCompat.id("arrow_turret"), "main");

    public static final String HEAD = "head";

    private TurretModel() {
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        root.addChild("base",
                ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -4.0F, 8.0F, 4.0F, 8.0F, Dilation.NONE),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        root.addChild("pedestal",
                ModelPartBuilder.create().uv(0, 13).cuboid(-2.0F, -6.0F, -2.0F, 4.0F, 6.0F, 4.0F, Dilation.NONE),
                ModelTransform.pivot(0.0F, 20.0F, 0.0F));

        ModelPartData head = root.addChild(HEAD,
                ModelPartBuilder.create().uv(0, 24).cuboid(-2.5F, -3.0F, -2.5F, 5.0F, 3.0F, 5.0F, Dilation.NONE),
                ModelTransform.pivot(0.0F, 14.0F, 0.0F));

        // A coronha aponta para -Z, que e a frente do mob depois da rotacao do renderer.
        head.addChild("stock",
                ModelPartBuilder.create().uv(0, 34).cuboid(-1.0F, -2.5F, -9.0F, 2.0F, 2.0F, 10.0F, Dilation.NONE),
                ModelTransform.NONE);

        head.addChild("limb_left",
                ModelPartBuilder.create().uv(34, 14).cuboid(1.0F, -2.0F, -8.0F, 6.0F, 1.0F, 2.0F, Dilation.NONE),
                ModelTransform.NONE);

        head.addChild("limb_right",
                ModelPartBuilder.create().uv(34, 18).cuboid(-7.0F, -2.0F, -8.0F, 6.0F, 1.0F, 2.0F, Dilation.NONE),
                ModelTransform.NONE);

        return TexturedModelData.of(data, 64, 64);
    }
}
