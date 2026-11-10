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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds the two nearest dispensers, reads the number on each item, and decides which slot each should pick. */
public final class Rigger {
    public static final int RADIUS = 8;
    public static final int HEIGHT = 4;
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    public record Plan(BlockPos winPos, BlockPos losePos, int winSlot, int loseSlot, int winNum, int loseNum) {}

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

    /**
     * The number on an item:
     *  1) the first number found in its name ("7", "#7", "Dirt 7", "§a7" all give 7),
     *  2) else its stack size, if the stacks in that dispenser have different sizes,
     *  3) else its slot position (1-9).
     */
    public static int numberOf(DispenserBlockEntity d, int slot) {
        ItemStack s = d.getStack(slot);
        String name = s.getName().getString().replaceAll("§.", "");
        Matcher m = DIGITS.matcher(name);
        if (m.find()) {
            try { return Integer.parseInt(m.group()); } catch (NumberFormatException ignored) {}
        }
        if (countsDiffer(d)) return s.getCount();
        return slot + 1;
    }

    private static boolean countsDiffer(DispenserBlockEntity d) {
        Set<Integer> counts = new HashSet<>();
        for (int i = 0; i < d.size(); i++) if (!d.getStack(i).isEmpty()) counts.add(d.getStack(i).getCount());
        return counts.size() > 1;
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

    /**
     * The winning side gets a random number that still leaves room for the other side to be lower,
     * then the losing side is forced to a strictly LOWER number (or its lowest, if none is lower).
     */
    public static Plan plan(ServerWorld w, PlayerEntity p, Random random, boolean winLeft) {
        BlockPos[] pair = pair(w, p);
        if (pair == null) return null;
        BlockPos winPos = winLeft ? pair[0] : pair[1];
        BlockPos losePos = winLeft ? pair[1] : pair[0];
        DispenserBlockEntity win = dispenser(w, winPos);
        DispenserBlockEntity lose = dispenser(w, losePos);
        if (win == null || lose == null) return null;

        List<Integer> wf = filled(win), lf = filled(lose);
        if (wf.isEmpty() || lf.isEmpty()) return null;

        int minLose = Integer.MAX_VALUE;
        for (int s : lf) minLose = Math.min(minLose, numberOf(lose, s));

        List<Integer> wc = new ArrayList<>();
        for (int s : wf) if (numberOf(win, s) > minLose) wc.add(s);
        if (wc.isEmpty()) {                          // nothing beats the loser's lowest: take the winner's highest
            int max = Integer.MIN_VALUE;
            for (int s : wf) max = Math.max(max, numberOf(win, s));
            for (int s : wf) if (numberOf(win, s) == max) wc.add(s);
        }
        int winSlot = wc.get(random.nextInt(wc.size()));
        int winNum = numberOf(win, winSlot);

        List<Integer> lc = new ArrayList<>();
        for (int s : lf) if (numberOf(lose, s) < winNum) lc.add(s);
        if (lc.isEmpty()) {
            for (int s : lf) if (numberOf(lose, s) == minLose) lc.add(s);
        }
        int loseSlot = lc.get(random.nextInt(lc.size()));
        return new Plan(winPos, losePos, winSlot, loseSlot, winNum, numberOf(lose, loseSlot));
    }

    private Rigger() {}
}
