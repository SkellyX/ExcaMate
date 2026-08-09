package com.skelly.excamate.mixin;

import com.skelly.excamate.ExcaMateXpCapture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockXpMixin {
    @Inject(method = "popExperience", at = @At("HEAD"), cancellable = true)
    private void excamate$captureVeinXp(ServerLevel level, BlockPos pos, int amount, CallbackInfo ci) {
        if (!ExcaMateXpCapture.isCapturing()) return;

        // Vanilla has already calculated the XP amount, including Silk Touch suppression.
        // ExcaMate stores it directly and cancels orb spawning for extra vein-mined blocks.
        ExcaMateXpCapture.add(amount);
        ci.cancel();
    }
}
