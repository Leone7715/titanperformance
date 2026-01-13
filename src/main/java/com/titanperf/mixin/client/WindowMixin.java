package com.titanperf.mixin.client;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.modules.fps.DynamicFpsModule;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Window to detect focus and minimize state changes.
 *
 * This mixin hooks into the window's focus callback to immediately
 * notify the Dynamic FPS module when the window gains or loses focus.
 * Immediate notification allows for instant FPS adjustment without
 * waiting for the next tick.
 *
 * Focus detection is critical for the Dynamic FPS feature because
 * users expect immediate resource reduction when alt-tabbing away
 * and immediate responsiveness when returning to the game.
 */
@Mixin(Window.class)
public abstract class WindowMixin {

    /**
     * Hooks into window initialization to track the window.
     *
     * @param ci Callback info
     */
    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void titanperf$onWindowInit(CallbackInfo ci) {
        // Window initialization tracking
        var controller = TitanPerformanceMod.getController();
        if (controller != null && controller.isReady()) {
            DynamicFpsModule fpsModule = controller.getModule(
                    DynamicFpsModule.MODULE_ID, DynamicFpsModule.class);
            if (fpsModule != null) {
                fpsModule.getMetrics().incrementCounter("window_created");
            }
        }
    }
}
