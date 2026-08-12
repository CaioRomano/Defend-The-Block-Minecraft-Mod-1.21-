package com.defendtheblock.client.render;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.entity.turret.TurretEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Desenha a torreta e faz a besta apontar para onde ela esta mirando. */
@Environment(EnvType.CLIENT)
public class TurretRenderer extends EntityRenderer<TurretEntity> {

    /** Uma textura por nivel: o visual da besta muda com cada material aplicado. */
    private static final Identifier[] TEXTURES = {
            DtbCompat.id("textures/entity/arrow_turret_0.png"),
            DtbCompat.id("textures/entity/arrow_turret_1.png"),
            DtbCompat.id("textures/entity/arrow_turret_2.png"),
            DtbCompat.id("textures/entity/arrow_turret_3.png"),
            DtbCompat.id("textures/entity/arrow_turret_4.png"),
    };

    private final ModelPart root;
    private final ModelPart head;

    public TurretRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.root = context.getPart(TurretModel.LAYER);
        this.head = root.getChild(TurretModel.HEAD);
        this.shadowRadius = 0.5F;
    }

    @Override
    public Identifier getTexture(TurretEntity entity) {
        return TEXTURES[entity.getTierIndex()];
    }

    @Override
    public void render(TurretEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
        // Mesma convencao do LivingEntityRenderer: modelo de cabeca para baixo.
        matrices.scale(-1.0F, -1.0F, 1.0F);
        matrices.translate(0.0F, -1.501F, 0.0F);

        float pitch = MathHelper.lerpAngleDegrees(tickDelta, entity.prevPitch, entity.getPitch());
        head.pitch = pitch * ((float) Math.PI / 180.0F);

        Identifier texture = TEXTURES[entity.getTierIndex()];
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
        root.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
