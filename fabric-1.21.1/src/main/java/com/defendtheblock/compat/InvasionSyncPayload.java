package com.defendtheblock.compat;

import com.defendtheblock.DefendTheBlock;
import com.defendtheblock.network.InvasionSyncData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Envelope {@code CustomPayload} exigido pelo sistema de rede do 1.20.5+. */
public record InvasionSyncPayload(InvasionSyncData data) implements CustomPayload {

    public static final CustomPayload.Id<InvasionSyncPayload> ID =
            new CustomPayload.Id<>(Identifier.of(DefendTheBlock.MOD_ID, "invasion_sync"));

    public static final PacketCodec<PacketByteBuf, InvasionSyncPayload> CODEC = PacketCodec.of(
            (value, buf) -> value.data().write(buf),
            buf -> new InvasionSyncPayload(InvasionSyncData.read(buf)));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
