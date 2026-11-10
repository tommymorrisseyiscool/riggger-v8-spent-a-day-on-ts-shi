package com.example.gamblerig.client;

import com.example.gamblerig.StatusPayload;
import com.example.gamblerig.TogglePayload;
import com.example.gamblerig.client.modules.Modules;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class GambleRigClient implements ClientModInitializer {
    // Faint green (ARGB) for the corner dot. Lower the first two hex digits (0x50) to make it fainter.
    private static final int DOT_COLOR = 0x5000C800;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("gamblerig", "main"));
        KeyBinding menu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.gamblerig.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, category));
        KeyBinding toggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.gamblerig.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, category));
        KeyBinding hud = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.gamblerig.hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, category));
        KeyBinding modulesKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.gamblerig.modules", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        Modules.init();

        ClientPlayNetworking.registerGlobalReceiver(StatusPayload.ID, (payload, context) -> {
            ClientState.enabled = payload.enabled();
            ClientState.left = payload.left();
            ClientState.right = payload.right();
            ClientState.last = payload.last();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menu.wasPressed()) client.setScreen(new RigScreen());
            while (modulesKey.wasPressed()) client.setScreen(new ModulesScreen());
            while (hud.wasPressed()) ClientState.hudVisible = !ClientState.hudVisible;   // R: hide / unhide
            while (toggle.wasPressed() && client.player != null) {
                ClientState.enabled = !ClientState.enabled;           // instant feedback; server confirms
                ClientPlayNetworking.send(TogglePayload.INSTANCE);
            }
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of("gamblerig", "hud"),
            (ctx, tickCounter) -> draw(ctx));
    }

    private static void draw(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();

        // Tiny faint dot, bottom-right, while enabled
        if (ClientState.enabled) ctx.fill(w - 6, h - 6, w - 3, h - 3, DOT_COLOR);

        // Top-right info (R hides it)
        if (!ClientState.hudVisible) return;
        String[] lines = {
            ClientState.enabled ? "RIG: ENABLED" : "RIG: DISABLED",
            "L: " + ClientState.left,
            "R: " + ClientState.right,
            "Last: " + ClientState.last };
        int[] colors = { ClientState.enabled ? 0xFF55FF55 : 0xFFFF5555, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFF55 };
        int y = 6;
        for (int i = 0; i < lines.length; i++) {
            int x = w - mc.textRenderer.getWidth(lines[i]) - 6;
            ctx.drawTextWithShadow(mc.textRenderer, lines[i], x, y, colors[i]);
            y += 11;
        }
    }
}
