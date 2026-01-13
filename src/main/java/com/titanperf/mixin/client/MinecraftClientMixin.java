package com.titanperf.mixin.client;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.modules.fps.DynamicFpsModule;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for MinecraftClient to integrate dynamic FPS control.
 *
 * This mixin hooks into the client's FPS limit getter to apply dynamic
 * limiting based on window focus and game state. When the window is
 * unfocused or the player is in a menu, the FPS limit is reduced to
 * save system resources.
 *
 * The approach of modifying the return value of getFramerateLimit() is
 * cleaner than modifying frame timing directly, as it works with
 * Minecraft's existing frame pacing code.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    /**
     * Modifies the frame rate limit based on current game state.
     *
     * This injection runs after the original method to potentially
     * override the return value with a lower limit when dynamic
     * FPS limiting is active.
     *
     * @param cir Callback info containing the original return value
     */
    @Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true, require = 0)
    private void titanperf$modifyFramerateLimit(CallbackInfoReturnable<Integer> cir) {
        // Get the dynamic FPS module from the controller
        var controller = TitanPerformanceMod.getController();
        if (controller == null || !controller.isReady()) {
            return;
        }

        DynamicFpsModule fpsModule = controller.getModule(
                DynamicFpsModule.MODULE_ID, DynamicFpsModule.class);

        if (fpsModule == null || !fpsModule.isEnabled()) {
            return;
        }

        // Check if we should apply a different FPS limit
        if (fpsModule.shouldLimitFps()) {
            int dynamicLimit = fpsModule.getCurrentFpsLimit();
            int originalLimit = cir.getReturnValue();

            // Only apply if our limit is lower than the original
            // This respects user-configured limits while adding dynamic reduction
            if (dynamicLimit < originalLimit) {
                cir.setReturnValue(dynamicLimit);
            }
        }
    }
}
