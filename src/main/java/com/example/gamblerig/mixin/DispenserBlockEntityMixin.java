package com.example.gamblerig.mixin;

import com.example.gamblerig.GambleRig;
import com.example.gamblerig.Rigger;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
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
            16.0, e -> GambleRig.isEnabled(e.getUuid()));
        if (p == null) return;

        BlockPos[] pair = Rigger.pair(sw, p);          // {left, right} as seen from where you face
        if (pair == null) return;
        BlockPos me = self.getPos();
        boolean isLeft;
        if (me.equals(pair[0])) isLeft = true;
        else if (me.equals(pair[1])) isLeft = false;
        else return;                                   // not one of the two tracked dispensers

        GambleRig.RigState st = GambleRig.state(p.getUuid());
        boolean isWinner = (isLeft == st.winLeft);
        Item winItem = st.winLeft ? st.leftItem : st.rightItem;
        Item loseItem = st.winLeft ? st.rightItem : st.leftItem;

        if (isWinner) {
            // winning side: make sure the winning item is what comes out
            for (int i = 0; i < self.size(); i++) {
                if (self.getStack(i).isOf(winItem)) {
                    cir.setReturnValue(i);
                    p.sendMessage(Text.literal("[Rig] " + GambleRig.name(winItem) + " wins"), true);
                    return;
                }
            }
        } else {
            // losing side: never let the losing item come out
            for (int i = 0; i < self.size(); i++) {
                if (!self.getStack(i).isEmpty() && !self.getStack(i).isOf(loseItem)) {
                    cir.setReturnValue(i);
                    return;
                }
            }
            cir.setReturnValue(-1);   // only the losing item is in there: dispenser just clicks
        }
    }
}
