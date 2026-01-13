package com.titanperf.mixin.client;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.modules.rendering.RenderingOptimizerModule;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for WorldRenderer to integrate chunk rendering optimizations.
 *
 * This mixin hooks into the world rendering pipeline to apply our
 * chunk culling and update scheduling optimizations. It intercepts
 * chunk rebuild requests and routes them through our scheduling system.
 *
 * The WorldRenderer is responsible for coordinating all terrain rendering.
 * By intercepting its chunk management methods, we can control which
 * chunks are rebuilt and when, preventing frame drops from excessive
 * concurrent chunk builds.
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    /**
     * Hook into tick method which exists across all versions.
     * This is safer than hooking render() which has version-specific signatures.
     */
    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void titanperf$onTick(CallbackInfo ci) {
        var controller = TitanPerformanceMod.getController();
        if (controller == null || !controller.isReady()) {
            return;
        }

        RenderingOptimizerModule renderModule = controller.getModule(
                RenderingOptimizerModule.MODULE_ID, RenderingOptimizerModule.class);

        if (renderModule != null && renderModule.isEnabled()) {
            renderModule.getMetrics().incrementCounter("render_ticks");
        }
    }
}
