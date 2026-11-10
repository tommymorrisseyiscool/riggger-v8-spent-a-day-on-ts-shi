package com.example.gamblerig;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: fill the two nearest dispensers with this item, named 1-9. */
public record FillPayload(Identifier item) implements CustomPayload {
    public static final CustomPayload.Id<FillPayload> ID =
        new CustomPayload.Id<>(Identifier.of("gamblerig", "fill"));
    public static final PacketCodec<RegistryByteBuf, FillPayload> CODEC = PacketCodec.tuple(
        Identifier.PACKET_CODEC, FillPayload::item,
        FillPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
