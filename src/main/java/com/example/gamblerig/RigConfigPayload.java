package com.example.gamblerig;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: which side should win (true = LEFT, false = RIGHT). */
public record RigConfigPayload(boolean winLeft) implements CustomPayload {
    public static final CustomPayload.Id<RigConfigPayload> ID =
        new CustomPayload.Id<>(Identifier.of("gamblerig", "rigconfig"));
    public static final PacketCodec<RegistryByteBuf, RigConfigPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.BOOLEAN, RigConfigPayload::winLeft,
        RigConfigPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
