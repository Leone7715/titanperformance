package com.titanperf.modules.entity;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.core.api.ModuleCategory;
import com.titanperf.core.config.TitanConfig;
import com.titanperf.core.module.AbstractPerformanceModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Lightweight entity culling module.
 *
 * DESIGN PRINCIPLE: This module must add LESS overhead than the rendering it saves.
 * Every check must be extremely fast - no allocations, no map lookups, no complex math.
 *
 * Culling Strategy (in order of check cost):
 * 1. Skip players (never cull) - instant check
 * 2. Distance culling - single squared distance comparison
 * 3. Behind-player culling - simple dot product
 */
public class EntityCullerModule extends AbstractPerformanceModule {

    public static final String MODULE_ID = "entity_culler";

    private MinecraftClient client;
    private int cullingDistanceSquared;
    private int cullingDistance;
    private boolean aggressiveMode;

    // Stats tracked per-tick, not per-entity (to avoid overhead)
    private int entitiesCulledLastTick;
    private int entitiesCheckedLastTick;
    private int tempCulled;
    private int tempChecked;

    public EntityCullerModule() {
        super(MODULE_ID, "Entity Culler", ModuleCategory.ENTITY, 600);
    }

    @Override
    protected void initializeModule() {
        client = MinecraftClient.getInstance();
        TitanConfig config = TitanPerformanceMod.getConfig();
        cullingDistance = config.getModuleSettingInt(MODULE_ID, "cullingDistance", 64);
        cullingDistanceSquared = cullingDistance * cullingDistance;
        aggressiveMode = config.getModuleSettingBool(MODULE_ID, "aggressiveMode", false);
        logger.info("Entity culler: distance={}, aggressive={}", cullingDistance, aggressiveMode);
    }

    @Override
    protected void enableModule() {
        entitiesCulledLastTick = 0;
        entitiesCheckedLastTick = 0;
    }

    @Override
    protected void disableModule() {
        // Nothing to clean up
    }

    @Override
    protected void tickModule() {
        // Update stats once per tick (not per entity)
        entitiesCulledLastTick = tempCulled;
        entitiesCheckedLastTick = tempChecked;
        tempCulled = 0;
        tempChecked = 0;

        metrics.setGauge("entities_culled", entitiesCulledLastTick);
        metrics.setGauge("entities_checked", entitiesCheckedLastTick);
    }

    @Override
    protected void shutdownModule() {
        // Nothing to clean up
    }

    /**
     * FAST entity culling check. Called for every entity every frame.
     * Must be extremely lightweight - no allocations, no map lookups.
     *
     * @param entity The entity to check
     * @return true if entity should NOT be rendered (culled)
     */
    public boolean shouldCullEntity(Entity entity) {
        // Quick bail if disabled or no player
        if (!isEnabled()) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;

        tempChecked++;

        // NEVER cull players (critical for multiplayer)
        if (entity instanceof PlayerEntity) {
            return false;
        }

        // NEVER cull the entity the player is riding or that's riding player
        if (entity.hasPassenger(mc.player) || mc.player.hasPassenger(entity)) {
            return false;
        }

        // Fast distance check using squared distance (no sqrt)
        double dx = entity.getX() - mc.player.getX();
        double dy = entity.getY() - mc.player.getY();
        double dz = entity.getZ() - mc.player.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        // Cull if beyond distance
        if (distSq > cullingDistanceSquared) {
            tempCulled++;
            return true;
        }

        // Behind-player check (only for entities > 20 blocks away)
        if (distSq > 400) { // 20^2 = 400
            float yaw = mc.player.getYaw();
            double lookX = -Math.sin(Math.toRadians(yaw));
            double lookZ = Math.cos(Math.toRadians(yaw));

            // Dot product: negative = behind
            double dot = dx * lookX + dz * lookZ;
            if (dot < -10.0) { // Clearly behind (with margin)
                tempCulled++;
                return true;
            }
        }

        // Aggressive mode: cull decorative entities at medium distance
        if (aggressiveMode && distSq > 256) { // 16^2 = 256
            if (entity instanceof ArmorStandEntity || entity instanceof ItemFrameEntity) {
                tempCulled++;
                return true;
            }
        }

        return false;
    }

    /**
     * Updates culling distance at runtime.
     */
    public void setCullingDistance(int distance) {
        this.cullingDistance = distance;
        this.cullingDistanceSquared = distance * distance;
    }

    public int getCullingDistance() {
        return cullingDistance;
    }

    public int getEntitiesCulledLastTick() {
        return entitiesCulledLastTick;
    }

    public int getEntitiesCheckedLastTick() {
        return entitiesCheckedLastTick;
    }
}
