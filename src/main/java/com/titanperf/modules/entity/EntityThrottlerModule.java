package com.titanperf.modules.entity;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.core.api.ModuleCategory;
import com.titanperf.core.config.TitanConfig;
import com.titanperf.core.module.AbstractPerformanceModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entity tick throttling module for reducing update frequency of distant entities.
 *
 * Inspired by optimizations in Lithium, this module reduces how often entities
 * are ticked based on their distance from the player and their current activity.
 * Distant entities or idle entities don't need full tick rate processing.
 *
 * Throttling Strategy:
 * Entities are categorized into priority tiers based on multiple factors:
 * 1. Distance from player (closer = higher priority)
 * 2. Entity type (hostile mobs, players = higher priority)
 * 3. Current activity (moving, in combat = higher priority)
 * 4. Player attention (looked at = higher priority)
 *
 * Tick Rate Tiers:
 * FULL (every tick): Entities within 16 blocks or high priority
 * NORMAL (every 2 ticks): Entities within 32 blocks
 * REDUCED (every 3 ticks): Distant passive entities
 * MINIMAL (every 4 ticks): Very distant idle entities
 *
 * Safety Considerations:
 * Some entity types should never be throttled:
 * Players: Always need full tick rate for responsiveness
 * Projectiles: Need accurate physics simulation
 * Entities near player: Must respond to player actions
 *
 * Tick Compensation:
 * When an entity skips ticks, certain accumulated values (like movement)
 * may need compensation to prevent visual stuttering. The module tracks
 * skipped ticks and can provide compensation data to rendering code.
 *
 * Configuration Options:
 * tickIntervalIdle: Tick interval for idle entities
 * tickIntervalDistant: Tick interval for distant entities
 */
public class EntityThrottlerModule extends AbstractPerformanceModule {

    /**
     * Module identifier for configuration and registration.
     */
    public static final String MODULE_ID = "entity_throttler";

    /**
     * Tracking data for each entity to manage throttling.
     */
    private final Map<UUID, EntityThrottleData> entityData;

    /**
     * Tick interval for idle entities (not moving, no target).
     */
    private int tickIntervalIdle;

    /**
     * Tick interval for distant entities.
     */
    private int tickIntervalDistant;

    /**
     * Reference to Minecraft client.
     */
    private MinecraftClient client;

    /**
     * Counter of entities throttled this tick.
     */
    private int entitiesThrottledThisTick;

    /**
     * Counter of entities processed at full rate this tick.
     */
    private int entitiesFullRateThisTick;

    /**
     * Constructs the entity throttler module.
     */
    public EntityThrottlerModule() {
        super(MODULE_ID, "Entity Throttler", ModuleCategory.ENTITY, 550);
        this.entityData = new ConcurrentHashMap<>();
    }

    @Override
    protected void initializeModule() {
        client = MinecraftClient.getInstance();

        // Load configuration
        TitanConfig config = TitanPerformanceMod.getConfig();
        tickIntervalIdle = config.getModuleSettingInt(MODULE_ID, "tickIntervalIdle", 3);
        tickIntervalDistant = config.getModuleSettingInt(MODULE_ID, "tickIntervalDistant", 2);

        logger.info("Entity throttler configured: idle interval={}, distant interval={}",
                tickIntervalIdle, tickIntervalDistant);
    }

    @Override
    protected void enableModule() {
        entityData.clear();
        entitiesThrottledThisTick = 0;
        entitiesFullRateThisTick = 0;
    }

    @Override
    protected void disableModule() {
        entityData.clear();
    }

    @Override
    protected void tickModule() {
        if (client.world == null) {
            return;
        }

        // Clean up data for entities that no longer exist
        long currentTick = getCurrentTick();
        if (currentTick % 100 == 0) {
            cleanupStaleData();
        }

        // Update metrics
        metrics.setGauge("entities_throttled_tick", entitiesThrottledThisTick);
        metrics.setGauge("entities_full_rate_tick", entitiesFullRateThisTick);
        metrics.setGauge("tracked_entities", entityData.size());

        // Reset per-tick counters
        entitiesThrottledThisTick = 0;
        entitiesFullRateThisTick = 0;
    }

    @Override
    protected void shutdownModule() {
        entityData.clear();
    }

    /**
     * Determines whether an entity should be ticked this game tick.
     *
     * This method is called from entity tick mixins before each entity
     * processes its tick logic. Returning false causes the entity to
     * skip its tick, saving CPU time.
     *
     * @param entity The entity about to tick
     * @return true if the entity should tick, false to skip
     */
    public boolean shouldEntityTick(Entity entity) {
        if (!isEnabled() || client.player == null) {
            return true;
        }

        // Never throttle critical entity types
        if (isUnthrottleable(entity)) {
            entitiesFullRateThisTick++;
            return true;
        }

        // Get or create throttle data for this entity
        EntityThrottleData data = entityData.computeIfAbsent(
                entity.getUuid(),
                uuid -> new EntityThrottleData()
        );

        // Calculate the appropriate tick interval for this entity
        int tickInterval = calculateTickInterval(entity, data);
        data.currentInterval = tickInterval;

        // Check if this entity should tick based on interval
        long currentTick = getCurrentTick();
        if (tickInterval <= 1 || (currentTick % tickInterval == 0)) {
            data.lastTickedAt = currentTick;
            data.ticksSkipped = 0;
            entitiesFullRateThisTick++;
            return true;
        }

        // Entity is being throttled
        data.ticksSkipped++;
        entitiesThrottledThisTick++;
        metrics.incrementCounter("total_ticks_skipped");
        return false;
    }

    /**
     * Checks if an entity should never be throttled.
     *
     * Some entity types need consistent tick rates for game mechanics
     * or player experience. These are always ticked at full rate.
     *
     * @param entity The entity to check
     * @return true if entity should always tick
     */
    private boolean isUnthrottleable(Entity entity) {
        // Players must always tick for responsiveness
        if (entity instanceof PlayerEntity) {
            return true;
        }

        // Projectiles need accurate physics
        if (entity instanceof ProjectileEntity) {
            return true;
        }

        // Entities the player is riding must tick
        if (entity.hasPassenger(client.player)) {
            return true;
        }

        // Entities very close to player must tick for interaction
        if (entity.squaredDistanceTo(client.player) < 16.0) { // 4 blocks
            return true;
        }

        return false;
    }

    /**
     * Calculates the appropriate tick interval for an entity.
     *
     * The interval is determined by entity distance, type, and activity level.
     * Lower interval means more frequent ticks (1 = every tick).
     *
     * @param entity The entity to evaluate
     * @param data The throttle tracking data for this entity
     * @return Tick interval (1 = every tick, 2 = every other tick, etc.)
     */
    private int calculateTickInterval(Entity entity, EntityThrottleData data) {
        double distanceSquared = entity.squaredDistanceTo(client.player);

        // Very close entities tick every time
        if (distanceSquared < 256.0) { // 16 blocks
            return 1;
        }

        // Check if entity is idle (for additional throttling)
        boolean isIdle = isEntityIdle(entity, data);

        // Medium distance
        if (distanceSquared < 1024.0) { // 32 blocks
            return isIdle ? Math.min(tickIntervalIdle, 2) : 1;
        }

        // Far distance
        if (distanceSquared < 4096.0) { // 64 blocks
            return isIdle ? tickIntervalIdle : tickIntervalDistant;
        }

        // Very far: maximum throttling
        return tickIntervalIdle;
    }

    /**
     * Determines if an entity is currently idle.
     *
     * Idle entities are those not actively engaged in behavior that
     * requires frequent updates. Movement, combat, and targeting
     * all indicate activity.
     *
     * @param entity The entity to check
     * @param data The throttle tracking data
     * @return true if entity appears idle
     */
    private boolean isEntityIdle(Entity entity, EntityThrottleData data) {
        // Check velocity: moving entities are not idle
        if (entity.getVelocity().lengthSquared() > 0.01) {
            data.lastActiveAt = getCurrentTick();
            return false;
        }

        // For mobs, check if they have a target
        if (entity instanceof MobEntity mob) {
            if (mob.getTarget() != null) {
                data.lastActiveAt = getCurrentTick();
                return false;
            }
        }

        // Item entities on ground can be heavily throttled
        if (entity instanceof ItemEntity item) {
            if (item.isOnGround()) {
                return true;
            }
        }

        // Check how long since last activity
        long ticksSinceActive = getCurrentTick() - data.lastActiveAt;
        return ticksSinceActive > 20; // Idle after 1 second of inactivity
    }

    /**
     * Returns the number of ticks this entity has skipped.
     *
     * This information can be used by rendering code to compensate
     * for skipped ticks and smooth out visual movement.
     *
     * @param entity The entity to query
     * @return Number of consecutive ticks skipped
     */
    public int getSkippedTicks(Entity entity) {
        EntityThrottleData data = entityData.get(entity.getUuid());
        return data != null ? data.ticksSkipped : 0;
    }

    /**
     * Returns the current tick interval for an entity.
     *
     * @param entity The entity to query
     * @return Current tick interval, or 1 if not tracked
     */
    public int getCurrentInterval(Entity entity) {
        EntityThrottleData data = entityData.get(entity.getUuid());
        return data != null ? data.currentInterval : 1;
    }

    /**
     * Removes tracking data for entities that no longer exist.
     */
    private void cleanupStaleData() {
        if (client.world == null) {
            entityData.clear();
            return;
        }

        long currentTick = getCurrentTick();
        entityData.entrySet().removeIf(entry -> {
            // Remove if not ticked in a while (entity probably despawned)
            return currentTick - entry.getValue().lastTickedAt > 200;
        });
    }

    /**
     * Updates configuration at runtime.
     *
     * @param idleInterval New idle tick interval
     * @param distantInterval New distant tick interval
     */
    public void updateConfiguration(int idleInterval, int distantInterval) {
        this.tickIntervalIdle = idleInterval;
        this.tickIntervalDistant = distantInterval;
        logger.info("Entity throttler config updated: idle={}, distant={}",
                idleInterval, distantInterval);
    }

    /**
     * Tracking data for entity throttling decisions.
     */
    private static class EntityThrottleData {
        /**
         * Game tick when entity was last allowed to tick.
         */
        long lastTickedAt = 0;

        /**
         * Game tick when entity was last observed as active.
         */
        long lastActiveAt = 0;

        /**
         * Current tick interval assigned to this entity.
         */
        int currentInterval = 1;

        /**
         * Number of consecutive ticks this entity has skipped.
         */
        int ticksSkipped = 0;
    }
}
