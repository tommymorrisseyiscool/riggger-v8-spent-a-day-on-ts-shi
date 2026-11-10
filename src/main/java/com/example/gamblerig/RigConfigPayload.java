package com.example.gamblerig;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: item in each dispenser, which side wins, and optionally fill the dispensers. */
public record RigConfigPayload(Identifier left, Identifier right, boolean winLeft, boolean fill) implements CustomPayload {
    public static final CustomPayload.Id<RigConfigPayload> ID =
        new CustomPayload.Id<>(Identifier.of("gamblerig", "rigconfig"));
    public static final PacketCodec<RegistryByteBuf, RigConfigPayload> CODEC = PacketCodec.tuple(
        Identifier.PACKET_CODEC, RigConfigPayload::left,
        Identifier.PACKET_CODEC, RigConfigPayload::right,
        PacketCodecs.BOOLEAN, RigConfigPayload::winLeft,
        PacketCodecs.BOOLEAN, RigConfigPayload::fill,
        RigConfigPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
