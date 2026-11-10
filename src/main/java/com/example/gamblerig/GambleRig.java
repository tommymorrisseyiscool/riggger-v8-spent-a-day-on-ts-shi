package com.example.gamblerig;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GambleRig implements ModInitializer {
    public static class RigState {
        public volatile boolean enabled = false;                     // X toggles this
        public volatile boolean winLeft = false;                     // false = RIGHT side wins
        public final Map<Long, Integer> pending = new HashMap<>();  // dispenser pos -> forced slot
        public long pendingTick = -1000;
        public volatile String last = "-";
    }

    public static final Map<UUID, RigState> STATES = new ConcurrentHashMap<>();

    public static RigState state(UUID id) { return STATES.computeIfAbsent(id, k -> new RigState()); }

    public static boolean isEnabled(UUID id) {
        RigState s = STATES.get(id);
        return s != null && s.enabled;
    }

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
            if (w != null) sendStatus(w, player, s);
        });

        // Menu: choose the winning side
        ServerPlayNetworking.registerGlobalReceiver(RigConfigPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (!allowed(context.server(), player)) return;
            RigState s = state(player.getUuid());
            s.winLeft = payload.winLeft();
            player.sendMessage(Text.literal("[Rig] Winning side: " + (s.winLeft ? "LEFT" : "RIGHT")), true);
            ServerWorld w = worldOf(context.server(), player);
            if (w != null) sendStatus(w, player, s);
        });

        // Keep the HUD updated (only for players who have used the mod)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 10 != 0) return;
            for (ServerWorld w : server.getWorlds()) {
                for (ServerPlayerEntity pl : w.getPlayers()) {
                    RigState s = STATES.get(pl.getUuid());
                    if (s != null) sendStatus(w, pl, s);
                }
            }
        });
    }

    public static void sendStatus(ServerWorld w, ServerPlayerEntity pl, RigState s) {
        if (!ServerPlayNetworking.canSend(pl, StatusPayload.ID)) return;
        String l = "none", r = "none";
        BlockPos[] pair = Rigger.pair(w, pl);
        if (pair != null) {
            DispenserBlockEntity L = Rigger.dispenser(w, pair[0]);
            DispenserBlockEntity R = Rigger.dispenser(w, pair[1]);
            if (L != null) l = Rigger.describe(L);
            if (R != null) r = Rigger.describe(R);
        }
        String win = (s.winLeft ? "LEFT" : "RIGHT") + "   last " + s.last;
        ServerPlayNetworking.send(pl, new StatusPayload(s.enabled, l, r, win));
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
