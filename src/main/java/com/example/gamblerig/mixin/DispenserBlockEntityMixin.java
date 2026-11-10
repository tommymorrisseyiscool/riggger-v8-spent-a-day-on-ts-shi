package com.example.gamblerig.mixin;

import com.example.gamblerig.GambleRig;
import com.example.gamblerig.Rigger;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
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
            12.0, e -> GambleRig.isEnabled(e.getUuid()));
        if (p == null) return;

        GambleRig.RigState st = GambleRig.state(p.getUuid());
        long now = sw.getTime();
        long key = self.getPos().asLong();

        // The two dispensers fire at about the same time: plan the pair once, then hand out each result.
        if (now - st.pendingTick > 20 || !st.pending.containsKey(key)) {
            Rigger.Plan plan = Rigger.plan(sw, p, random);
            if (plan == null) return;
            st.pending.clear();
            st.pending.put(plan.leftPos().asLong(), plan.leftSlot());
            st.pending.put(plan.rightPos().asLong(), plan.rightSlot());
            st.pendingTick = now;
            st.last = "L=" + plan.leftNum() + "  R=" + plan.rightNum();
        }

        Integer forced = st.pending.remove(key);
        if (forced != null && !self.getStack(forced).isEmpty()) cir.setReturnValue(forced);
    }
}
