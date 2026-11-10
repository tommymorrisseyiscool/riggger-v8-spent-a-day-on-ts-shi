package com.example.gamblerig;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GambleRig implements ModInitializer {
    public static class RigState {
        public volatile boolean enabled = false;                     // X toggles this
        public volatile Item leftItem = Items.DIAMOND;               // what is in the LEFT dispenser
        public volatile Item rightItem = Items.TOTEM_OF_UNDYING;     // what is in the RIGHT dispenser
        public volatile boolean winLeft = false;                     // false = right side wins (totems)
    }

    public static final Map<UUID, RigState> STATES = new ConcurrentHashMap<>();

    public static RigState state(UUID id) { return STATES.computeIfAbsent(id, k -> new RigState()); }

    public static boolean isEnabled(UUID id) {
        RigState s = STATES.get(id);
        return s != null && s.enabled;
    }

    public static String name(Item item) { return Registries.ITEM.getId(item).getPath(); }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(TogglePayload.ID, TogglePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RigConfigPayload.ID, RigConfigPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatusPayload.ID, StatusPayload.CODEC);

        // X: toggle auto win
        ServerPlayNetworking.registerGlobalReceiver(TogglePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (!allowed(context.server(), player)) return;
            RigState s = state(player.getUuid());
            s.enabled = !s.enabled;
            player.sendMessage(Text.literal("[Rig] Auto win: " + (s.enabled ? "ENABLED" : "DISABLED")), true);
            ServerWorld w = worldOf(context.server(), player);
            if (w != null) sendStatus(player, s);
        });

        // Menu: set items + winner, optionally fill the dispensers
        ServerPlayNetworking.registerGlobalReceiver(RigConfigPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (!allowed(context.server(), player)) return;
            RigState s = state(player.getUuid());
            s.leftItem = Registries.ITEM.get(payload.left());
            s.rightItem = Registries.ITEM.get(payload.right());
            s.winLeft = payload.winLeft();

            if (payload.fill()) {
                ServerWorld w = worldOf(context.server(), player);
                BlockPos[] pair = w == null ? null : Rigger.pair(w, player);
                if (pair == null) {
                    player.sendMessage(Text.literal("[Rig] Need 2 dispensers within " + Rigger.RADIUS + " blocks"), true);
                    return;
                }
                fill(w, pair[0], s.leftItem);
                fill(w, pair[1], s.rightItem);
                player.sendMessage(Text.literal("[Rig] Filled left=" + name(s.leftItem) + " right=" + name(s.rightItem)), true);
            } else {
                player.sendMessage(Text.literal("[Rig] Left=" + name(s.leftItem) + "  Right=" + name(s.rightItem)
                    + "  Winner=" + (s.winLeft ? "LEFT" : "RIGHT")), true);
            }
            sendStatus(player, s);
        });

        // Keep the HUD updated (only for players who have used the mod)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 10 != 0) return;
            for (ServerWorld w : server.getWorlds()) {
                for (ServerPlayerEntity pl : w.getPlayers()) {
                    RigState s = STATES.get(pl.getUuid());
                    if (s != null) sendStatus(pl, s);
                }
            }
        });
    }

    private static void fill(ServerWorld w, BlockPos pos, Item item) {
        DispenserBlockEntity d = Rigger.dispenser(w, pos);
        if (d == null) return;
        for (int i = 0; i < d.size(); i++) d.setStack(i, new ItemStack(item, 1));
        d.markDirty();
    }

    public static void sendStatus(ServerPlayerEntity pl, RigState s) {
        if (!ServerPlayNetworking.canSend(pl, StatusPayload.ID)) return;
        String win = s.winLeft ? "LEFT (" + name(s.leftItem) + ")" : "RIGHT (" + name(s.rightItem) + ")";
        ServerPlayNetworking.send(pl, new StatusPayload(s.enabled, name(s.leftItem), name(s.rightItem), win));
    }

    public static ServerWorld worldOf(MinecraftServer server, ServerPlayerEntity player) {
        for (ServerWorld w : server.getWorlds()) {
            if (w.getPlayerByUuid(player.getUuid()) != null) return w;
        }
        return null;
    }

    /** Allowed = singleplayer/LAN host, or listed in config/gamblerig.txt (one username per line). */
    private static boolean allowed(MinecraftServer server, ServerPlayerEntity player) {
        if (server.isSingleplayer()) return true;
        String name = player.getName().getString().toLowerCase();
        if (loadAllowed().contains(name)) return true;
        player.sendMessage(Text.literal("[Rig] You are not in config/gamblerig.txt"), true);
        return false;
    }

    private static Set<String> loadAllowed() {
        Set<String> out = new HashSet<>();
        Path p = FabricLoader.getInstance().getConfigDir().resolve("gamblerig.txt");
        try {
            if (!Files.exists(p)) Files.writeString(p, "# One Minecraft username per line\n");
            for (String line : Files.readAllLines(p)) {
                line = line.trim().toLowerCase();
                if (!line.isEmpty() && !line.startsWith("#")) out.add(line);
            }
        } catch (Exception ignored) {}
        return out;
    }
}
