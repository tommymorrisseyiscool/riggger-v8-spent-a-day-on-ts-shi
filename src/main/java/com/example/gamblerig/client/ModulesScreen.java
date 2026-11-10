package com.example.gamblerig.client;

import com.example.gamblerig.client.modules.Module;
import com.example.gamblerig.client.modules.Modules;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Right Shift menu: click a module to turn it on/off. */
public class ModulesScreen extends Screen {
    public ModulesScreen() { super(Text.literal("Modules")); }

    @Override
    protected void init() {
        int cx = width / 2 - 100;
        int y = height / 2 - (Modules.ALL.size() * 12) - 10;
        for (Module m : Modules.ALL) {
            addDrawableChild(ButtonWidget.builder(label(m), b -> {
                m.toggle(client);
                b.setMessage(label(m));
            }).dimensions(cx, y, 200, 20).build());
            y += 24;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Gamble Rig menu"), b -> client.setScreen(new RigScreen()))
            .dimensions(cx, y + 6, 200, 20).build());
    }

    private static Text label(Module m) {
        return Text.literal(m.name + ": " + (m.isEnabled() ? "ON" : "OFF"));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);
    }
}
