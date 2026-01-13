package com.titanperf.rendering;

import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;

/**
 * Level of Detail (LOD) management system.
 *
 * This is a standard computer graphics technique that reduces rendering
 * complexity for distant objects. The basic principle is that objects
 * far from the camera take up fewer pixels and thus don't need full detail.
 *
 * LOD Levels:
 * - FULL: Complete rendering with all effects
 * - HIGH: Full model but reduced animation updates
 * - MEDIUM: Simplified rendering (fewer particles, simpler shadows)
 * - LOW: Minimal rendering (billboard sprites, no animations)
 * - CULLED: Don't render at all
 *
 * Entity Priority System:
 * Some entities are more important than others and should be rendered
 * at higher detail even at distance:
 * - Players: Always FULL (important for PvP)
 * - Hostile mobs: Higher priority (player safety)
 * - Bosses: Always FULL (important gameplay elements)
 * - Decorative: Lower priority (armor stands, item frames)
 */
public class LODManager {

    /**
     * Level of Detail enum with distance thresholds.
     */
    public enum LODLevel {
        FULL(0, 16),           // 0-16 blocks: full detail
        HIGH(16, 32),          // 16-32 blocks: reduced animations
        MEDIUM(32, 64),        // 32-64 blocks: simplified rendering
        LOW(64, 128),          // 64-128 blocks: minimal rendering
        CULLED(128, Integer.MAX_VALUE); // Beyond: don't render

        public final int minDistance;
        public final int maxDistance;

        LODLevel(int minDistance, int maxDistance) {
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
        }

        /**
         * Gets the LOD level for a given distance.
         *
         * @param distance Distance in blocks
         * @return The appropriate LOD level
         */
        public static LODLevel forDistance(double distance) {
            if (distance < HIGH.minDistance) return FULL;
            if (distance < MEDIUM.minDistance) return HIGH;
            if (distance < LOW.minDistance) return MEDIUM;
            if (distance < CULLED.minDistance) return LOW;
            return CULLED;
        }
    }

    /**
     * Entity importance priority for LOD decisions.
     */
    public enum EntityPriority {
        CRITICAL(4),    // Never reduce LOD (players, bosses)
        HIGH(3),        // Reduce LOD slowly (hostile mobs)
        NORMAL(2),      // Standard LOD reduction (passive mobs, vehicles)
        LOW(1),         // Aggressive LOD reduction (decorative entities)
        MINIMAL(0);     // Most aggressive reduction (particles, small items)

        public final int value;

        EntityPriority(int value) {
            this.value = value;
        }
    }

    /**
     * Distance multiplier for each priority level.
     * Higher priority entities have their effective distance reduced,
     * keeping them at higher LOD levels for longer.
     */
    private static final float[] PRIORITY_MULTIPLIERS = {
        1.5f,   // MINIMAL: 1.5x distance (lower LOD sooner)
        1.2f,   // LOW: 1.2x distance
        1.0f,   // NORMAL: standard distance
        0.7f,   // HIGH: 0.7x distance (higher LOD longer)
        0.0f    // CRITICAL: always full LOD
    };

    /**
     * Animation tick intervals for each LOD level.
     * Higher LOD = more frequent animation updates.
     */
    private static final int[] ANIMATION_INTERVALS = {
        1,   // FULL: every tick
        2,   // HIGH: every 2 ticks
        4,   // MEDIUM: every 4 ticks
        8,   // LOW: every 8 ticks
        0    // CULLED: no animation
    };

    /**
     * Singleton instance.
     */
    private static final LODManager INSTANCE = new LODManager();

    /**
     * Gets the singleton instance.
     */
    public static LODManager getInstance() {
        return INSTANCE;
    }

    private LODManager() {
        // Private constructor for singleton
    }

    /**
     * Determines the LOD level for an entity based on distance and priority.
     *
     * @param entity The entity to evaluate
     * @param distanceSquared Squared distance from camera
     * @return The LOD level to use for rendering
     */
    public LODLevel getLODLevel(Entity entity, double distanceSquared) {
        double distance = Math.sqrt(distanceSquared);
        EntityPriority priority = getEntityPriority(entity);

        // Critical entities always render at full LOD
        if (priority == EntityPriority.CRITICAL) {
            return LODLevel.FULL;
        }

        // Apply priority multiplier to distance
        float multiplier = PRIORITY_MULTIPLIERS[priority.ordinal()];
        double effectiveDistance = distance * multiplier;

        return LODLevel.forDistance(effectiveDistance);
    }

    /**
     * Determines if an entity should be rendered based on LOD.
     *
     * @param entity The entity to check
     * @param distanceSquared Squared distance from camera
     * @return true if the entity should be rendered
     */
    public boolean shouldRender(Entity entity, double distanceSquared) {
        return getLODLevel(entity, distanceSquared) != LODLevel.CULLED;
    }

    /**
     * Gets the animation tick interval for an entity at the given LOD.
     *
     * @param lodLevel The current LOD level
     * @return Ticks between animation updates (0 = no animation)
     */
    public int getAnimationInterval(LODLevel lodLevel) {
        return ANIMATION_INTERVALS[lodLevel.ordinal()];
    }

    /**
     * Determines if an entity's animation should update this tick.
     *
     * @param lodLevel The current LOD level
     * @param tickCount Current game tick
     * @return true if animation should update
     */
    public boolean shouldUpdateAnimation(LODLevel lodLevel, long tickCount) {
        int interval = getAnimationInterval(lodLevel);
        if (interval == 0) return false;
        return tickCount % interval == 0;
    }

    /**
     * Determines the priority of an entity for LOD decisions.
     *
     * @param entity The entity to evaluate
     * @return The priority level
     */
    public EntityPriority getEntityPriority(Entity entity) {
        // Players and bosses are always critical
        if (entity instanceof PlayerEntity) {
            return EntityPriority.CRITICAL;
        }
        if (entity instanceof EnderDragonEntity || entity instanceof WitherEntity) {
            return EntityPriority.CRITICAL;
        }

        // Hostile mobs are high priority (player safety)
        if (entity instanceof HostileEntity) {
            return EntityPriority.HIGH;
        }

        // Vehicles and villagers are normal priority
        if (entity instanceof BoatEntity || entity instanceof MinecartEntity) {
            return EntityPriority.NORMAL;
        }
        if (entity instanceof VillagerEntity) {
            return EntityPriority.NORMAL;
        }

        // Decorative entities are low priority
        if (entity instanceof ArmorStandEntity || entity instanceof ItemFrameEntity) {
            return EntityPriority.LOW;
        }

        // Projectiles are minimal priority (small, fast-moving)
        if (entity instanceof ProjectileEntity) {
            return EntityPriority.MINIMAL;
        }

        // Default to normal priority
        return EntityPriority.NORMAL;
    }

    /**
     * Gets the shadow rendering mode for a LOD level.
     *
     * @param lodLevel The LOD level
     * @return true if shadows should be rendered
     */
    public boolean shouldRenderShadow(LODLevel lodLevel) {
        return lodLevel == LODLevel.FULL || lodLevel == LODLevel.HIGH;
    }

    /**
     * Gets the particle spawn rate multiplier for a LOD level.
     *
     * @param lodLevel The LOD level
     * @return Multiplier (0.0 to 1.0) for particle spawn rate
     */
    public float getParticleMultiplier(LODLevel lodLevel) {
        return switch (lodLevel) {
            case FULL -> 1.0f;
            case HIGH -> 0.75f;
            case MEDIUM -> 0.5f;
            case LOW -> 0.25f;
            case CULLED -> 0.0f;
        };
    }
}
