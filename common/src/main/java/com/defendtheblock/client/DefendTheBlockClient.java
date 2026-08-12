package com.defendtheblock.client;

import com.defendtheblock.client.render.TurretModel;
import com.defendtheblock.client.render.TurretRenderer;
import com.defendtheblock.compat.DtbClientCompat;
import com.defendtheblock.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

@Environment(EnvType.CLIENT)
public class DefendTheBlockClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(TurretModel.LAYER, TurretModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.ARROW_TURRET, TurretRenderer::new);
        EntityRendererRegistry.register(ModEntities.WEB_SHOT, FlyingItemEntityRenderer::new);

        // Lambda em vez de method reference: o tipo do segundo parametro e
        // inferido, entao a mesma linha serve para o float do 1.20.1 e para o
        // RenderTickCounter do 1.21.
        HudRenderCallback.EVENT.register((context, tickDelta) -> InvasionHud.render(context));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientInvasionState.reset());

        DtbClientCompat.registerNetworking(ClientInvasionState::update);
        DtbClientCompat.registerTurretStatsNetworking(TurretStatsScreen::accept);
        DtbClientCompat.registerTooltips();
    }
}
