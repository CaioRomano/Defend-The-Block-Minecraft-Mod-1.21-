package com.defendtheblock.client;

import com.defendtheblock.network.InvasionSyncData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Ultimo estado de invasao recebido do servidor, usado pelo HUD. */
@Environment(EnvType.CLIENT)
public final class ClientInvasionState {

    private static volatile InvasionSyncData current = InvasionSyncData.empty();

    private ClientInvasionState() {
    }

    public static InvasionSyncData get() {
        return current;
    }

    public static void update(InvasionSyncData data) {
        current = data;
    }

    public static void reset() {
        current = InvasionSyncData.empty();
    }
}
