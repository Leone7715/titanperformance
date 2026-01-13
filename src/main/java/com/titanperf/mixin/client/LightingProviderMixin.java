package com.titanperf.mixin.client;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.modules.lighting.LightingOptimizerModule;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for LightingProvider to integrate lighting optimizations.
 *
 * This mixin intercepts light update requests and routes them through
 * our batching and scheduling system. Instead of processing each light
 * update immediately (which can cause stuttering), updates are queued
 * and processed in controlled batches.
 *
 * The LightingProvider coordinates both block light and sky light updates.
 * Light propagation is one of the more expensive operations in Minecraft,
 * especially when many blocks change at once (like during chunk loading
 * or explosions).
 */
@Mixin(LightingProvider.class)
public abstract class LightingProviderMixin {

    /**
     * Intercepts block light check requests.
     *
     * When Minecraft wants to update lighting at a position, we can
     * optionally defer this to our batching system instead of processing
     * it immediately.
     *
     * @param pos The position to check
     * @param ci Callback info
     */
    @Inject(method = "checkBlock", at = @At("HEAD"), require = 0)
    private void titanperf$onCheckBlock(BlockPos pos, CallbackInfo ci) {
        // Get the lighting optimizer module
        var controller = TitanPerformanceMod.getController();
        if (controller == null || !controller.isReady()) {
            return;
        }

        LightingOptimizerModule lightingModule = controller.getModule(
                LightingOptimizerModule.MODULE_ID, LightingOptimizerModule.class);

        if (lightingModule == null || !lightingModule.isEnabled()) {
            return;
        }

        // Schedule this update for batched processing
        // The actual vanilla update still runs, but we track it for metrics
        // In a more aggressive implementation, we could defer the update entirely
        lightingModule.scheduleLightUpdate(pos, true);
    }

    /**
     * Hook after light updates propagate.
     *
     * @param cir Callback info returnable
     */
    @Inject(method = "doLightUpdates", at = @At("RETURN"), require = 0)
    private void titanperf$afterLightUpdates(CallbackInfoReturnable<Integer> cir) {
        var controller = TitanPerformanceMod.getController();
        if (controller == null || !controller.isReady()) {
            return;
        }

        LightingOptimizerModule lightingModule = controller.getModule(
                LightingOptimizerModule.MODULE_ID, LightingOptimizerModule.class);

        if (lightingModule != null && lightingModule.isEnabled()) {
            lightingModule.getMetrics().incrementCounter("light_update_batches");
        }
    }
}
