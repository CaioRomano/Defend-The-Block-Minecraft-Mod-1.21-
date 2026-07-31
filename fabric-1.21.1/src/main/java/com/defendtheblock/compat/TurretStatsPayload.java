package com.defendtheblock.compat;

import com.defendtheblock.DefendTheBlock;
import com.defendtheblock.network.TurretStatsData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Envelope {@code CustomPayload} exigido pelo sistema de rede do 1.20.5+. */
public record TurretStatsPayload(TurretStatsData data) implements CustomPayload {

    public static final CustomPayload.Id<TurretStatsPayload> ID =
            new CustomPayload.Id<>(Identifier.of(DefendTheBlock.MOD_ID, "turret_stats"));

    public static final PacketCodec<PacketByteBuf, TurretStatsPayload> CODEC = PacketCodec.of(
            (value, buf) -> value.data().write(buf),
            buf -> new TurretStatsPayload(TurretStatsData.read(buf)));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
