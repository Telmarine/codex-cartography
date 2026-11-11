package com.tac5studios.codexcartography.mixin;

import com.tac5studios.codexcartography.client.MapSession;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client only: tells the map when a block changes, so that chunk is read again. */
@Mixin(Level.class)
@SuppressWarnings({"unused", "DataFlowIssue", "ConstantValue"}) // called by Mixin; (Object) this cast
public abstract class LevelSetBlockMixin {

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("RETURN"))
    private void codexCartography$blockChanged(BlockPos pos, BlockState state, int flags, int recursionLeft,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && ((Level) (Object) this).isClientSide()) MapSession.blockChanged(pos);
    }
}
