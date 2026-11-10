package com.example.gamblerig.client.modules;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Modules {
    public static final List<Module> ALL = new ArrayList<>();

    public static void init() {
        ALL.add(new AimAssist());
        ALL.add(new BridgeAssist());
        ALL.add(new ToggleSprint());
        ALL.add(new Fullbright());
        ALL.add(new FpsBoost());

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            for (Module m : ALL) {
                if (!m.isEnabled()) continue;
                if (m.singleplayerOnly && !mc.isInSingleplayer()) { m.forceOff(mc); continue; }  // left the solo world
                if (mc.player == null || mc.world == null) continue;
                m.tick(mc);
            }
        });
    }

    // ---- Aim Assist: only while you hold attack, only hostile mobs, only singleplayer ----
    static class AimAssist extends Module {
        AimAssist() { super("Aim Assist (mobs)", true); }
        @Override public void tick(MinecraftClient mc) {
            if (mc.currentScreen != null || !mc.options.attackKey.isPressed()) return;
            ClientPlayerEntity p = mc.player;
            Box box = p.getBoundingBox().expand(4.5);
            List<HostileEntity> list = mc.world.getEntitiesByClass(HostileEntity.class, box, e -> e.isAlive() && p.canSee(e));
            if (list.isEmpty()) return;
            HostileEntity t = list.stream()
                .min(Comparator.comparingDouble((HostileEntity e) -> e.squaredDistanceTo(p))).get();
            Vec3d eye = p.getEyePos();
            Vec3d tgt = t.getBoundingBox().getCenter();
            double dx = tgt.x - eye.x, dy = tgt.y - eye.y, dz = tgt.z - eye.z;
            float targetYaw = (float) (MathHelper.atan2(dz, dx) * 57.29577951308232) - 90.0f;
            float targetPitch = (float) (-(MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 57.29577951308232));
            float dYaw = MathHelper.wrapDegrees(targetYaw - p.getYaw());
            float dPitch = MathHelper.wrapDegrees(targetPitch - p.getPitch());
            p.setYaw(p.getYaw() + MathHelper.clamp(dYaw, -8.0f, 8.0f));
            p.setPitch(MathHelper.clamp(p.getPitch() + MathHelper.clamp(dPitch, -8.0f, 8.0f), -90.0f, 90.0f));
        }
    }

    // ---- Bridge Assist: places the held block under you when you step onto air (singleplayer only) ----
    static class BridgeAssist extends Module {
        private int cooldown = 0;
        BridgeAssist() { super("Bridge Assist", true); }
        @Override public void tick(MinecraftClient mc) {
            if (mc.currentScreen != null || mc.interactionManager == null) return;
            if (cooldown-- > 0) return;
            ClientPlayerEntity p = mc.player;
            if (!(p.getMainHandStack().getItem() instanceof BlockItem) || !p.isOnGround()) return;
            BlockPos target = p.getBlockPos().down();
            if (!mc.world.getBlockState(target).isAir()) return;
            Direction[] sides = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN };
            for (Direction d : sides) {
                BlockPos n = target.offset(d);
                BlockState s = mc.world.getBlockState(n);
                if (s.isAir()) continue;
                Direction face = d.getOpposite();
                Vec3d hitPos = Vec3d.ofCenter(n).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
                mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hitPos, face, n, false));
                p.swingHand(Hand.MAIN_HAND);
                cooldown = 2;
                break;
            }
        }
    }

    // ---- Toggle Sprint ----
    static class ToggleSprint extends Module {
        ToggleSprint() { super("Toggle Sprint", false); }
        @Override public void tick(MinecraftClient mc) {
            ClientPlayerEntity p = mc.player;
            if (mc.options.forwardKey.isPressed() && !p.isSneaking() && p.getHungerManager().getFoodLevel() > 6) {
                p.setSprinting(true);
            }
        }
    }

    // ---- Fullbright (local night vision) ----
    static class Fullbright extends Module {
        Fullbright() { super("Fullbright", false); }
        @Override public void tick(MinecraftClient mc) {
            ClientPlayerEntity p = mc.player;
            StatusEffectInstance cur = p.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (cur == null || cur.getDuration() < 260) {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 600, 0, false, false, false));
            }
        }
        @Override public void onDisable(MinecraftClient mc) {
            if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    // ---- FPS Boost: lowers a few video settings, restores them when turned off ----
    static class FpsBoost extends Module {
        private int view, sim;
        private double entityScale;
        private ParticlesMode particles;
        FpsBoost() { super("FPS Boost", false); }
        @Override public void onEnable(MinecraftClient mc) {
            view = mc.options.getViewDistance().getValue();
            sim = mc.options.getSimulationDistance().getValue();
            entityScale = mc.options.getEntityDistanceScaling().getValue();
            particles = mc.options.getParticles().getValue();
            mc.options.getViewDistance().setValue(Math.min(view, 8));
            mc.options.getSimulationDistance().setValue(Math.min(sim, 8));
            mc.options.getEntityDistanceScaling().setValue(0.5);
            mc.options.getParticles().setValue(ParticlesMode.MINIMAL);
        }
        @Override public void onDisable(MinecraftClient mc) {
            mc.options.getViewDistance().setValue(view);
            mc.options.getSimulationDistance().setValue(sim);
            mc.options.getEntityDistanceScaling().setValue(entityScale);
            mc.options.getParticles().setValue(particles);
        }
        @Override public void tick(MinecraftClient mc) {}
    }

    private Modules() {}
}
