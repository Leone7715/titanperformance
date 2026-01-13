package com.titanperf.mixin.common;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin for LivingEntity to optimize living entity processing.
 *
 * LivingEntity adds complex behaviors like AI, health, and equipment
 * to the base Entity class. Optimizations here can target the more
 * expensive living entity operations.
 *
 * This mixin is reserved for AI and pathfinding optimizations.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    // Living entity optimizations would be implemented here
    // Examples: AI throttling, pathfinding optimization

}
