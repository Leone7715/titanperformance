package com.titanperf.modules.memory;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.core.api.ModuleCategory;
import com.titanperf.core.config.TitanConfig;
import com.titanperf.core.module.AbstractPerformanceModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memory optimization module for reducing allocation pressure and GC pauses.
 *
 * Inspired by FerriteCore, this module reduces memory usage and garbage
 * collection pressure through several strategies:
 *
 * 1. Object Pooling:
 * Frequently allocated short-lived objects are pooled and reused instead
 * of being created and garbage collected. This is particularly effective
 * for Vec3d, BlockPos, and similar math objects that are created thousands
 * of times per frame.
 *
 * 2. Allocation Avoidance:
 * Some allocations can be avoided entirely by using mutable objects or
 * primitive operations. This module provides utilities for allocation-free
 * alternatives to common operations.
 *
 * 3. Cache Management:
 * Internal caches are sized appropriately for available memory and cleaned
 * up proactively to prevent memory pressure buildup.
 *
 * 4. String Deduplication:
 * Duplicate strings (common in NBT data and translations) are deduplicated
 * to reduce memory footprint.
 *
 * Impact on GC:
 * Java's garbage collector must track and collect short-lived objects.
 * When many objects are created per frame, GC pauses can cause stuttering.
 * By pooling objects, we reduce the number of allocations and thus reduce
 * GC frequency and duration.
 *
 * Thread Safety:
 * Object pools use thread-local storage to avoid synchronization overhead.
 * Each thread has its own pool, eliminating contention.
 *
 * Configuration Options:
 * aggressivePooling: Enable more aggressive object reuse
 * cacheSize: Maximum size for internal caches
 */
public class MemoryOptimizerModule extends AbstractPerformanceModule {

    /**
     * Module identifier for configuration and registration.
     */
    public static final String MODULE_ID = "memory_optimizer";

    /**
     * Thread-local pool of BlockPos.Mutable objects.
     * Using mutable BlockPos avoids allocating new objects for temporary positions.
     */
    private static final ThreadLocal<Deque<BlockPos.Mutable>> blockPosPool =
            ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * Pool of reusable double arrays for vector operations.
     * Sized for Vec3d components (3 doubles).
     */
    private static final ThreadLocal<Deque<double[]>> vectorPool =
            ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * String interning cache for deduplication.
     * Uses weak references to allow GC of unused strings.
     */
    private final Map<String, WeakReference<String>> stringCache;

    /**
     * Maximum size for object pools per thread.
     */
    private int poolSize;

    /**
     * Whether aggressive pooling is enabled.
     */
    private boolean aggressivePooling;

    /**
     * Maximum cache size.
     */
    private int cacheSize;

    /**
     * Reference to Minecraft client.
     */
    private MinecraftClient client;

    /**
     * Counter of objects borrowed from pools this tick.
     */
    private int objectsBorrowedThisTick;

    /**
     * Counter of objects returned to pools this tick.
     */
    private int objectsReturnedThisTick;

    /**
     * Counter of pool misses (new allocations) this tick.
     */
    private int poolMissesThisTick;

    /**
     * Constructs the memory optimizer module.
     */
    public MemoryOptimizerModule() {
        super(MODULE_ID, "Memory Optimizer", ModuleCategory.MEMORY, 300);
        this.stringCache = new ConcurrentHashMap<>();
    }

    @Override
    protected void initializeModule() {
        client = MinecraftClient.getInstance();

        // Load configuration
        TitanConfig config = TitanPerformanceMod.getConfig();
        aggressivePooling = config.getModuleSettingBool(MODULE_ID, "aggressivePooling", false);
        cacheSize = config.getModuleSettingInt(MODULE_ID, "cacheSize", 128);

        // Pool size scales with aggressiveness
        poolSize = aggressivePooling ? 64 : 32;

        logger.info("Memory optimizer configured: aggressive={}, cacheSize={}, poolSize={}",
                aggressivePooling, cacheSize, poolSize);
    }

    @Override
    protected void enableModule() {
        objectsBorrowedThisTick = 0;
        objectsReturnedThisTick = 0;
        poolMissesThisTick = 0;
    }

    @Override
    protected void disableModule() {
        // Clear pools to release memory
        clearPools();
        stringCache.clear();
    }

    @Override
    protected void tickModule() {
        // Update metrics
        metrics.setGauge("objects_borrowed_tick", objectsBorrowedThisTick);
        metrics.setGauge("objects_returned_tick", objectsReturnedThisTick);
        metrics.setGauge("pool_misses_tick", poolMissesThisTick);
        metrics.setGauge("string_cache_size", stringCache.size());

        // Reset per-tick counters
        objectsBorrowedThisTick = 0;
        objectsReturnedThisTick = 0;
        poolMissesThisTick = 0;

        // Periodic cache cleanup
        if (getCurrentTick() % 600 == 0) { // Every 30 seconds
            cleanupCaches();
        }

        // Log memory stats periodically in debug mode
        if (TitanPerformanceMod.getConfig().isDebugMode() && getCurrentTick() % 200 == 0) {
            logMemoryStats();
        }
    }

    @Override
    protected void shutdownModule() {
        clearPools();
        stringCache.clear();
    }

    /**
     * Borrows a mutable BlockPos from the pool.
     *
     * The returned BlockPos should be returned to the pool after use via
     * returnBlockPos(). Failure to return objects will cause pool depletion
     * and new allocations.
     *
     * @return A mutable BlockPos instance
     */
    public BlockPos.Mutable borrowBlockPos() {
        if (!isEnabled()) {
            return new BlockPos.Mutable();
        }

        Deque<BlockPos.Mutable> pool = blockPosPool.get();
        BlockPos.Mutable result = pool.pollFirst();

        if (result != null) {
            objectsBorrowedThisTick++;
            metrics.incrementCounter("blockpos_borrowed");
            return result;
        }

        // Pool empty, must allocate
        poolMissesThisTick++;
        metrics.incrementCounter("blockpos_allocated");
        return new BlockPos.Mutable();
    }

    /**
     * Borrows a mutable BlockPos initialized to the specified position.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return A mutable BlockPos set to the given coordinates
     */
    public BlockPos.Mutable borrowBlockPos(int x, int y, int z) {
        BlockPos.Mutable pos = borrowBlockPos();
        pos.set(x, y, z);
        return pos;
    }

    /**
     * Borrows a mutable BlockPos initialized from a Vec3i.
     *
     * @param vec The vector to copy coordinates from
     * @return A mutable BlockPos set to the vector's coordinates
     */
    public BlockPos.Mutable borrowBlockPos(Vec3i vec) {
        return borrowBlockPos(vec.getX(), vec.getY(), vec.getZ());
    }

    /**
     * Returns a mutable BlockPos to the pool for reuse.
     *
     * @param pos The BlockPos to return (will be reset)
     */
    public void returnBlockPos(BlockPos.Mutable pos) {
        if (!isEnabled() || pos == null) {
            return;
        }

        Deque<BlockPos.Mutable> pool = blockPosPool.get();

        // Don't overfill the pool
        if (pool.size() < poolSize) {
            // Reset to origin to avoid stale data
            pos.set(0, 0, 0);
            pool.offerFirst(pos);
            objectsReturnedThisTick++;
            metrics.incrementCounter("blockpos_returned");
        }
    }

    /**
     * Borrows a double array for vector operations.
     *
     * @return A double array of length 3
     */
    public double[] borrowVectorArray() {
        if (!isEnabled()) {
            return new double[3];
        }

        Deque<double[]> pool = vectorPool.get();
        double[] result = pool.pollFirst();

        if (result != null) {
            objectsBorrowedThisTick++;
            return result;
        }

        poolMissesThisTick++;
        return new double[3];
    }

    /**
     * Returns a vector array to the pool.
     *
     * @param array The array to return
     */
    public void returnVectorArray(double[] array) {
        if (!isEnabled() || array == null || array.length != 3) {
            return;
        }

        Deque<double[]> pool = vectorPool.get();
        if (pool.size() < poolSize) {
            pool.offerFirst(array);
            objectsReturnedThisTick++;
        }
    }

    /**
     * Deduplicates a string using the internal cache.
     *
     * If an equal string already exists in the cache, returns that instance.
     * This reduces memory usage when the same strings appear many times
     * (common in NBT data and translations).
     *
     * @param str The string to deduplicate
     * @return The canonical instance of the string
     */
    public String deduplicateString(String str) {
        if (!isEnabled() || str == null) {
            return str;
        }

        // Check cache first
        WeakReference<String> ref = stringCache.get(str);
        if (ref != null) {
            String cached = ref.get();
            if (cached != null) {
                metrics.incrementCounter("string_cache_hits");
                return cached;
            }
        }

        // String not in cache, add it if cache isn't full
        if (stringCache.size() < cacheSize) {
            stringCache.put(str, new WeakReference<>(str));
            metrics.incrementCounter("string_cache_stores");
        }

        return str;
    }

    /**
     * Performs a calculation using pooled objects to avoid allocation.
     *
     * This demonstrates how to use the pool for temporary calculations.
     * The pattern is: borrow, calculate, return.
     *
     * @param start Starting position
     * @param direction Direction vector
     * @param distance Distance to travel
     * @return New position (newly allocated, not pooled)
     */
    public BlockPos calculateOffsetPosition(BlockPos start, Vec3d direction, double distance) {
        // Borrow a mutable pos for the calculation
        BlockPos.Mutable working = borrowBlockPos(start);

        try {
            // Perform calculation
            int offsetX = (int) (direction.x * distance);
            int offsetY = (int) (direction.y * distance);
            int offsetZ = (int) (direction.z * distance);

            working.move(offsetX, offsetY, offsetZ);

            // Return an immutable copy (the result the caller keeps)
            return working.toImmutable();
        } finally {
            // Always return borrowed objects
            returnBlockPos(working);
        }
    }

    /**
     * Returns current memory usage statistics.
     *
     * @return A string describing current memory state
     */
    public String getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        return String.format(
                "Memory: %dMB / %dMB (max %dMB)",
                usedMemory / (1024 * 1024),
                totalMemory / (1024 * 1024),
                maxMemory / (1024 * 1024)
        );
    }

    /**
     * Suggests garbage collection when memory pressure is high.
     *
     * This is called periodically when memory usage exceeds a threshold.
     * It's a suggestion to the JVM, not a guarantee of immediate collection.
     */
    public void suggestGarbageCollection() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();

        // Only suggest GC if we're using more than 70% of max memory
        if (usedMemory > maxMemory * 0.7) {
            logger.debug("Memory usage high ({}%), suggesting GC",
                    (usedMemory * 100) / maxMemory);
            System.gc();
            metrics.incrementCounter("gc_suggestions");
        }
    }

    /**
     * Clears all object pools.
     */
    private void clearPools() {
        blockPosPool.get().clear();
        vectorPool.get().clear();
    }

    /**
     * Cleans up stale entries from caches.
     */
    private void cleanupCaches() {
        // Remove entries whose weak references have been collected
        stringCache.entrySet().removeIf(entry ->
                entry.getValue().get() == null);

        metrics.incrementCounter("cache_cleanups");
    }

    /**
     * Logs current memory statistics for debugging.
     */
    private void logMemoryStats() {
        logger.debug(getMemoryStats());
        logger.debug("BlockPos pool size: {}", blockPosPool.get().size());
        logger.debug("String cache size: {}", stringCache.size());
    }

    /**
     * Updates configuration at runtime.
     *
     * @param aggressive New aggressive pooling setting
     * @param newCacheSize New cache size
     */
    public void updateConfiguration(boolean aggressive, int newCacheSize) {
        this.aggressivePooling = aggressive;
        this.cacheSize = newCacheSize;
        this.poolSize = aggressive ? 64 : 32;

        // Trim caches if new size is smaller
        while (stringCache.size() > cacheSize) {
            stringCache.keySet().iterator().remove();
        }

        logger.info("Memory optimizer config updated: aggressive={}, cacheSize={}",
                aggressive, newCacheSize);
    }
}
