package com.titanperf.mixin.client;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.modules.entity.EntityCullerModule;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lightweight mixin to cull entity rendering.
 *
 * PERFORMANCE CRITICAL: This runs for every entity every frame.
 * Must add minimal overhead - cache the module reference.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Unique
    private static EntityCullerModule titanperf$cachedCuller = null;

    @Unique
    private static boolean titanperf$checkedCuller = false;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private <E extends Entity> void titanperf$beforeEntityRender(
            E entity, double x, double y, double z, float yaw,
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
            CallbackInfo ci) {

        // Get cached culler (lazy init, avoids lookup every call)
        if (!titanperf$checkedCuller) {
            titanperf$checkedCuller = true;
            try {
                var controller = TitanPerformanceMod.getController();
                if (controller != null && controller.isReady()) {
                    titanperf$cachedCuller = controller.getModule(
                        EntityCullerModule.MODULE_ID, EntityCullerModule.class);
                }
            } catch (Exception ignored) {}
        }

        // Fast path: no culler available
        if (titanperf$cachedCuller == null) return;

        // Check if entity should be culled
        if (titanperf$cachedCuller.shouldCullEntity(entity)) {
            ci.cancel();
        }
    }
}
