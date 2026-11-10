package com.example.gamblerig.mixin;

import com.example.gamblerig.GambleRig;
import com.example.gamblerig.Rigger;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DispenserBlockEntity.class)
public abstract class DispenserBlockEntityMixin {
    @Inject(method = "chooseNonEmptySlot", at = @At("HEAD"), cancellable = true)
    private void gamblerig$rig(Random random, CallbackInfoReturnable<Integer> cir) {
        DispenserBlockEntity self = (DispenserBlockEntity) (Object) this;
        World world = self.getWorld();
        if (!(world instanceof ServerWorld sw)) return;

        PlayerEntity p = world.getClosestPlayer(
            self.getPos().getX() + 0.5, self.getPos().getY() + 0.5, self.getPos().getZ() + 0.5,
            16.0, e -> GambleRig.isEnabled(e.getUuid()));
        if (p == null) return;

        GambleRig.RigState st = GambleRig.state(p.getUuid());
        long now = sw.getTime();
        long key = self.getPos().asLong();

        // The two dispensers fire at about the same time: plan the pair once, then hand out each result.
        if (now - st.pendingTick > 20 || !st.pending.containsKey(key)) {
            Rigger.Plan plan = Rigger.plan(sw, p, random, st.winLeft);
            if (plan == null) {
                p.sendMessage(Text.literal("[Rig] Need 2 dispensers/droppers with items within " + Rigger.RADIUS + " blocks"), true);
                return;
            }
            st.pending.clear();
            st.pending.put(plan.winPos().asLong(), plan.winSlot());
            st.pending.put(plan.losePos().asLong(), plan.loseSlot());
            st.pendingTick = now;
            int l = st.winLeft ? plan.winNum() : plan.loseNum();
            int r = st.winLeft ? plan.loseNum() : plan.winNum();
            st.last = "L=" + l + " R=" + r;
            GambleRig.LOGGER.info("[GambleRig] rigged for {}: {} wins L={} R={}", p.getName().getString(),
                st.winLeft ? "LEFT" : "RIGHT", l, r);
            p.sendMessage(Text.literal("[Rig] " + (st.winLeft ? "LEFT" : "RIGHT") + " wins:  L=" + l + "  R=" + r), true);
        }

        Integer forced = st.pending.remove(key);
        if (forced != null && !self.getStack(forced).isEmpty()) cir.setReturnValue(forced);
    }
}
