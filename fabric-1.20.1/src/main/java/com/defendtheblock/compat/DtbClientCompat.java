package com.defendtheblock.compat;

import com.defendtheblock.client.TooltipLines;
import com.defendtheblock.network.InvasionSyncData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.function.Consumer;

/** Parte cliente da camada de compatibilidade de <b>1.20.1</b>. */
@Environment(EnvType.CLIENT)
public final class DtbClientCompat {

    private DtbClientCompat() {
    }

    public static void registerNetworking(Consumer<InvasionSyncData> handler) {
        ClientPlayNetworking.registerGlobalReceiver(DtbCompat.INVASION_SYNC, (client, netHandler, buf, sender) -> {
            InvasionSyncData data = InvasionSyncData.read(buf);
            client.execute(() -> handler.accept(data));
        });
    }

    public static void registerTooltips() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> TooltipLines.append(stack, lines));
    }
}
