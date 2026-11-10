package com.example.gamblerig.client;

import com.example.gamblerig.RigConfigPayload;
import com.example.gamblerig.TogglePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.List;

public class RigScreen extends Screen {
    private static final List<Item> ITEMS = List.of(
        Items.DIAMOND, Items.TOTEM_OF_UNDYING, Items.NETHERITE_INGOT, Items.GOLDEN_APPLE,
        Items.ENCHANTED_GOLDEN_APPLE, Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT,
        Items.PAPER, Items.OBSIDIAN, Items.DIRT, Items.COAL);
    private static int leftIdx = 0;       // Diamond
    private static int rightIdx = 1;      // Totem of Undying
    private static boolean winLeft = false;

    public RigScreen() { super(Text.literal("Gamble Rig")); }

    @Override
    protected void init() {
        int cx = width / 2 - 100, y = height / 2 - 80;

        addDrawableChild(ButtonWidget.builder(leftLabel(), b -> {
            leftIdx = (leftIdx + 1) % ITEMS.size();
            b.setMessage(leftLabel());
        }).dimensions(cx, y, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(rightLabel(), b -> {
            rightIdx = (rightIdx + 1) % ITEMS.size();
            b.setMessage(rightLabel());
        }).dimensions(cx, y + 24, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(winLabel(), b -> {
            winLeft = !winLeft;
            b.setMessage(winLabel());
        }).dimensions(cx, y + 48, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), b -> send(false))
            .dimensions(cx, y + 76, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Fill the 2 nearest dispensers with these items"), b -> send(true))
            .dimensions(cx, y + 100, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(toggleLabel(), b -> {
            ClientState.enabled = !ClientState.enabled;
            ClientPlayNetworking.send(TogglePayload.INSTANCE);
            b.setMessage(toggleLabel());
        }).dimensions(cx, y + 124, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
            .dimensions(cx, y + 148, 200, 20).build());
    }

    private void send(boolean fill) {
        ClientPlayNetworking.send(new RigConfigPayload(
            Registries.ITEM.getId(ITEMS.get(leftIdx)),
            Registries.ITEM.getId(ITEMS.get(rightIdx)),
            winLeft, fill));
    }

    private Text leftLabel() { return Text.literal("Left dispenser: ").append(ITEMS.get(leftIdx).getName()); }
    private Text rightLabel() { return Text.literal("Right dispenser: ").append(ITEMS.get(rightIdx).getName()); }
    private Text winLabel() {
        return Text.literal("Winner: " + (winLeft ? "LEFT" : "RIGHT") + " (")
            .append((winLeft ? ITEMS.get(leftIdx) : ITEMS.get(rightIdx)).getName()).append(")");
    }
    private Text toggleLabel() { return Text.literal("Auto Win: " + (ClientState.enabled ? "ON" : "OFF")); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 100, 0xFFFFFFFF);
    }
}
