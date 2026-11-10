package com.example.gamblerig.client;

import com.example.gamblerig.RigConfigPayload;
import com.example.gamblerig.TogglePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class RigScreen extends Screen {
    private static boolean winLeft = false;   // default: RIGHT side wins

    public RigScreen() { super(Text.literal("Gamble Rig")); }

    @Override
    protected void init() {
        int cx = width / 2 - 100, y = height / 2 - 40;

        addDrawableChild(ButtonWidget.builder(winLabel(), b -> {
            winLeft = !winLeft;
            b.setMessage(winLabel());
            ClientPlayNetworking.send(new RigConfigPayload(winLeft));   // saved on the server right away
        }).dimensions(cx, y, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(toggleLabel(), b -> {
            ClientState.enabled = !ClientState.enabled;
            ClientPlayNetworking.send(TogglePayload.INSTANCE);
            b.setMessage(toggleLabel());
        }).dimensions(cx, y + 26, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
            .dimensions(cx, y + 52, 200, 20).build());
    }

    private Text winLabel() { return Text.literal("Winning side: " + (winLeft ? "LEFT" : "RIGHT")); }
    private Text toggleLabel() { return Text.literal("Auto Win: " + (ClientState.enabled ? "ON" : "OFF")); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 62, 0xFFFFFFFF);
    }
}
