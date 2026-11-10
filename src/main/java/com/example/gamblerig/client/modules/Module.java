package com.example.gamblerig.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public abstract class Module {
    public final String name;
    public final boolean singleplayerOnly;
    private boolean enabled;

    protected Module(String name, boolean singleplayerOnly) {
        this.name = name;
        this.singleplayerOnly = singleplayerOnly;
    }

    public boolean isEnabled() { return enabled; }

    public void toggle(MinecraftClient mc) {
        if (!enabled && singleplayerOnly && !mc.isInSingleplayer()) {
            if (mc.player != null) mc.player.sendMessage(Text.literal(name + " only works in singleplayer"), true);
            return;
        }
        enabled = !enabled;
        if (enabled) onEnable(mc); else onDisable(mc);
    }

    /** Called by the manager when a singleplayer-only module has to shut off. */
    void forceOff(MinecraftClient mc) {
        if (enabled) { enabled = false; onDisable(mc); }
    }

    public void onEnable(MinecraftClient mc) {}
    public void onDisable(MinecraftClient mc) {}
    public abstract void tick(MinecraftClient mc);
}
