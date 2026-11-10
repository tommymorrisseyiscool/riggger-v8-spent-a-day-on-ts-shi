package com.example.gamblerig;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Finds the two nearest dispensers and decides which slot each one should pick. */
public final class Rigger {
    public static final int RADIUS = 8;
    public static final int HEIGHT = 4;

    public record Plan(BlockPos leftPos, BlockPos rightPos, int leftSlot, int rightSlot, int leftNum, int rightNum) {}

    public static List<BlockPos> nearestTwo(ServerWorld w, PlayerEntity p) {
        BlockPos c = p.getBlockPos();
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.iterate(c.add(-RADIUS, -HEIGHT, -RADIUS), c.add(RADIUS, HEIGHT, RADIUS))) {
            if (w.getBlockState(pos).isOf(Blocks.DISPENSER) && w.getBlockEntity(pos) instanceof DispenserBlockEntity) {
                found.add(pos.toImmutable());
            }
        }
        found.sort(Comparator.comparingDouble((BlockPos pos) -> pos.getSquaredDistance(p.getX(), p.getY(), p.getZ())));
        return found.size() > 2 ? new ArrayList<>(found.subList(0, 2)) : found;
    }

    /** Returns {left, right} relative to the way the player is facing, or null if fewer than 2 dispensers. */
    public static BlockPos[] pair(ServerWorld w, PlayerEntity p) {
        List<BlockPos> two = nearestTwo(w, p);
        if (two.size() < 2) return null;
        BlockPos a = two.get(0), b = two.get(1);
        double yaw = Math.toRadians(p.getYaw());
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);   // player's right-hand direction
        double sa = (a.getX() + 0.5 - p.getX()) * rx + (a.getZ() + 0.5 - p.getZ()) * rz;
        double sb = (b.getX() + 0.5 - p.getX()) * rx + (b.getZ() + 0.5 - p.getZ()) * rz;
        return sa < sb ? new BlockPos[]{a, b} : new BlockPos[]{b, a};
    }

    public static DispenserBlockEntity dispenser(ServerWorld w, BlockPos pos) {
        BlockEntity be = w.getBlockEntity(pos);
        return be instanceof DispenserBlockEntity d ? d : null;
    }

    /** The number on an item = its custom name if it is a number, otherwise its slot position (1-9). */
    public static int numberOf(DispenserBlockEntity d, int slot) {
        ItemStack s = d.getStack(slot);
        try {
            return Integer.parseInt(s.getName().getString().trim());
        } catch (Exception e) {
            return slot + 1;
        }
    }

    public static List<Integer> filled(DispenserBlockEntity d) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < d.size(); i++) if (!d.getStack(i).isEmpty()) out.add(i);
        return out;
    }

    public static String describe(DispenserBlockEntity d) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(d.getStack(i).isEmpty() ? "-" : String.valueOf(numberOf(d, i)));
        }
        return sb.toString();
    }

    /** Right dispenser stays random. Left is forced to a LOWER number (or the lowest it has if none are lower). */
    public static Plan plan(ServerWorld w, PlayerEntity p, Random random) {
        BlockPos[] pair = pair(w, p);
        if (pair == null) return null;
        DispenserBlockEntity left = dispenser(w, pair[0]);
        DispenserBlockEntity right = dispenser(w, pair[1]);
        if (left == null || right == null) return null;

        List<Integer> rf = filled(right), lf = filled(left);
        if (rf.isEmpty() || lf.isEmpty()) return null;

        int rightSlot = rf.get(random.nextInt(rf.size()));
        int rNum = numberOf(right, rightSlot);

        List<Integer> cands = new ArrayList<>();
        for (int s : lf) if (numberOf(left, s) < rNum) cands.add(s);
        if (cands.isEmpty()) {                       // right is the lowest: left gets its lowest number
            int min = Integer.MAX_VALUE;
            for (int s : lf) min = Math.min(min, numberOf(left, s));
            for (int s : lf) if (numberOf(left, s) == min) cands.add(s);
        }
        int leftSlot = cands.get(random.nextInt(cands.size()));
        return new Plan(pair[0], pair[1], leftSlot, rightSlot, numberOf(left, leftSlot), rNum);
    }

    private Rigger() {}
}
