package com.example.gamblerig.client;

import com.example.gamblerig.FillPayload;
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
        Items.DIRT, Items.STONE, Items.COBBLESTONE, Items.OAK_PLANKS, Items.SAND, Items.PAPER,
        Items.APPLE, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.DIAMOND,
        Items.EMERALD, Items.GOLD_INGOT, Items.COAL, Items.OBSIDIAN);
    private static int itemIndex = 0;

    private ButtonWidget itemBtn, toggleBtn;

    public RigScreen() { super(Text.literal("Gamble Rig")); }

    @Override
    protected void init() {
        int cx = width / 2 - 100, y = height / 2 - 40;

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> shift(-1)).dimensions(cx, y, 20, 20).build());
        itemBtn = addDrawableChild(ButtonWidget.builder(itemLabel(), b -> shift(1))
            .dimensions(cx + 24, y, 152, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> shift(1)).dimensions(cx + 180, y, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Fill 2 nearest dispensers (1-9)"), b -> {
            ClientPlayNetworking.send(new FillPayload(Registries.ITEM.getId(ITEMS.get(itemIndex))));
        }).dimensions(cx, y + 26, 200, 20).build());

        toggleBtn = addDrawableChild(ButtonWidget.builder(toggleLabel(), b -> {
            ClientState.enabled = !ClientState.enabled;
            ClientPlayNetworking.send(TogglePayload.INSTANCE);
            b.setMessage(toggleLabel());
        }).dimensions(cx, y + 52, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
            .dimensions(cx, y + 78, 200, 20).build());
    }

    private void shift(int d) {
        itemIndex = Math.floorMod(itemIndex + d, ITEMS.size());
        itemBtn.setMessage(itemLabel());
    }

    private Text itemLabel() { return Text.literal("Item: ").append(ITEMS.get(itemIndex).getName()); }
    private Text toggleLabel() { return Text.literal("Auto Win: " + (ClientState.enabled ? "ON" : "OFF")); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 62, 0xFFFFFFFF);
    }
}
