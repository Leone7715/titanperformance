package com.titanperf.mixin.client;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.modules.rendering.RenderingOptimizerModule;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for ChunkBuilder to track chunk build completion.
 *
 * The ChunkBuilder manages the thread pool that builds chunk meshes.
 * By hooking into its completion callbacks, we can track when chunks
 * finish building and update our scheduling accordingly.
 *
 * This is important for our chunk update throttling system because
 * we need to know when build slots free up so we can schedule the
 * next batch of updates.
 */
@Mixin(ChunkBuilder.class)
public abstract class ChunkBuilderMixin {

    /**
     * Hook into chunk builder initialization.
     *
     * @param ci Callback info
     */
    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void titanperf$onInit(CallbackInfo ci) {
        var controller = TitanPerformanceMod.getController();
        if (controller == null || !controller.isReady()) {
            return;
        }

        RenderingOptimizerModule renderModule = controller.getModule(
                RenderingOptimizerModule.MODULE_ID, RenderingOptimizerModule.class);

        if (renderModule != null && renderModule.isEnabled()) {
            renderModule.getMetrics().incrementCounter("chunk_builder_created");
        }
    }
}
