package com.titanperf.mixin.common;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.modules.entity.EntityThrottlerModule;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Entity base class to integrate tick throttling.
 *
 * This mixin intercepts entity tick calls to apply our throttling logic.
 * Distant or idle entities can have their tick rate reduced without
 * significantly affecting gameplay, saving substantial CPU time.
 *
 * The Entity class is the base for all entities in Minecraft. By hooking
 * here, we can affect the tick rate of all entity types uniformly.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    /**
     * Intercepts the entity tick to apply throttling.
     *
     * This injection checks with our throttling module whether this
     * entity should tick this frame. If not, the tick is cancelled.
     *
     * @param ci Callback info for cancellation
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void titanperf$beforeEntityTick(CallbackInfo ci) {
        // Only apply on client side
        if (!TitanPerformanceMod.isClient()) {
            return;
        }

        var controller = TitanPerformanceMod.getController();
        if (controller == null || !controller.isReady()) {
            return;
        }

        EntityThrottlerModule throttlerModule = controller.getModule(
                EntityThrottlerModule.MODULE_ID, EntityThrottlerModule.class);

        if (throttlerModule == null || !throttlerModule.isEnabled()) {
            return;
        }

        Entity self = (Entity) (Object) this;

        // Check if this entity should tick
        if (!throttlerModule.shouldEntityTick(self)) {
            ci.cancel();
        }
    }
}
