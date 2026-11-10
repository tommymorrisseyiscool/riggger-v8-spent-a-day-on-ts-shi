package com.example.gamblerig;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Server -> client: what the HUD shows. */
public record StatusPayload(boolean enabled, String left, String right, String last) implements CustomPayload {
    public static final CustomPayload.Id<StatusPayload> ID =
        new CustomPayload.Id<>(Identifier.of("gamblerig", "status"));
    public static final PacketCodec<RegistryByteBuf, StatusPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.BOOLEAN, StatusPayload::enabled,
        PacketCodecs.STRING, StatusPayload::left,
        PacketCodecs.STRING, StatusPayload::right,
        PacketCodecs.STRING, StatusPayload::last,
        StatusPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
