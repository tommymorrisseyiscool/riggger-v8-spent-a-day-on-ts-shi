package com.example.gamblerig;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TogglePayload() implements CustomPayload {
    public static final TogglePayload INSTANCE = new TogglePayload();
    public static final CustomPayload.Id<TogglePayload> ID =
        new CustomPayload.Id<>(Identifier.of("gamblerig", "toggle"));
    public static final PacketCodec<RegistryByteBuf, TogglePayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
