package io.github.orlouge.landmarks.mixin;

import io.github.orlouge.landmarks.LandmarksMod;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkRegion.class)
public class ChunkRegionMixin {

    @Inject(method = "markBlockForPostProcessing", at = @At("HEAD"), cancellable = true)
    public void landmarks$disablePostProcessing(BlockPos pos, CallbackInfo ci) {
        if (LandmarksMod.DISABLE_POST_PROCESSING_ONCE) {
            LandmarksMod.DISABLE_POST_PROCESSING_ONCE = false;
            ci.cancel();
        }
    }
}
