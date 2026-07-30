package com.defendtheblock.compat;

import com.defendtheblock.client.TooltipLines;
import com.defendtheblock.network.InvasionSyncData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.function.Consumer;

/** Parte cliente da camada de compatibilidade de <b>1.21.x</b>. */
@Environment(EnvType.CLIENT)
public final class DtbClientCompat {

    private DtbClientCompat() {
    }

    public static void registerNetworking(Consumer<InvasionSyncData> handler) {
        ClientPlayNetworking.registerGlobalReceiver(InvasionSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> handler.accept(payload.data())));
    }

    public static void registerTooltips() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> TooltipLines.append(stack, lines));
    }
}
