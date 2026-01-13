package com.titanperf.modules.smooth;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.core.api.ModuleCategory;
import com.titanperf.core.config.TitanConfig;
import com.titanperf.core.module.AbstractPerformanceModule;
import net.minecraft.client.MinecraftClient;

/**
 * Smooth FPS module for reducing stutters and frame time spikes.
 *
 * This module helps maintain consistent frame times by:
 * - Detecting and logging frame time spikes
 * - Triggering proactive garbage collection during safe moments
 * - Tracking performance metrics for analysis
 *
 * Frame Time Analysis:
 * Monitors frame times and identifies patterns that cause stuttering.
 * When a spike is detected, it can trigger cleanup operations during
 * the next idle moment to prevent further spikes.
 *
 * Proactive GC:
 * Instead of waiting for the JVM to trigger garbage collection at
 * inconvenient times (causing stutters), this module can request GC
 * during loading screens or when the player is idle.
 */
public class SmoothFpsModule extends AbstractPerformanceModule {

    public static final String MODULE_ID = "smooth_fps";

    private MinecraftClient client;

    // Frame time tracking
    private long[] frameTimes;
    private int frameIndex;
    private static final int FRAME_HISTORY = 60;

    // Stutter detection
    private long lastFrameTime;
    private int stutterCount;
    private static final long STUTTER_THRESHOLD_NS = 50_000_000L; // 50ms

    // Proactive GC
    private long lastGcTime;
    private static final long GC_INTERVAL_MS = 60_000L; // 1 minute minimum between GCs
    private boolean proactiveGcEnabled;

    // Stats
    private double averageFrameTime;
    private long maxFrameTime;
    private long minFrameTime;

    public SmoothFpsModule() {
        super(MODULE_ID, "Smooth FPS", ModuleCategory.FPS_CONTROL, 150);
    }

    @Override
    protected void initializeModule() {
        client = MinecraftClient.getInstance();
        frameTimes = new long[FRAME_HISTORY];
        frameIndex = 0;

        TitanConfig config = TitanPerformanceMod.getConfig();
        proactiveGcEnabled = config.getModuleSettingBool(MODULE_ID, "proactiveGc", true);

        logger.info("Smooth FPS initialized, proactive GC: {}", proactiveGcEnabled);
    }

    @Override
    protected void enableModule() {
        resetStats();
        lastGcTime = System.currentTimeMillis();
    }

    @Override
    protected void disableModule() {
        // Nothing to clean up
    }

    @Override
    protected void tickModule() {
        if (client == null) return;

        long currentTime = System.nanoTime();

        // Calculate frame time
        if (lastFrameTime > 0) {
            long frameTime = currentTime - lastFrameTime;
            recordFrameTime(frameTime);

            // Detect stutter
            if (frameTime > STUTTER_THRESHOLD_NS) {
                stutterCount++;
                metrics.incrementCounter("stutters_detected");
                logger.debug("Stutter detected: {}ms", frameTime / 1_000_000L);
            }
        }

        lastFrameTime = currentTime;

        // Proactive GC during loading or pause screens
        if (proactiveGcEnabled && shouldTriggerGc()) {
            triggerProactiveGc();
        }

        // Update metrics
        metrics.setGauge("avg_frame_time_us", (long) (averageFrameTime / 1000));
        metrics.setGauge("max_frame_time_us", maxFrameTime / 1000);
        metrics.setGauge("stutter_count", stutterCount);
    }

    @Override
    protected void shutdownModule() {
        // Nothing to clean up
    }

    /**
     * Records a frame time and updates statistics.
     */
    private void recordFrameTime(long frameTimeNs) {
        frameTimes[frameIndex] = frameTimeNs;
        frameIndex = (frameIndex + 1) % FRAME_HISTORY;

        // Update stats
        long sum = 0;
        long max = 0;
        long min = Long.MAX_VALUE;
        for (long ft : frameTimes) {
            if (ft > 0) {
                sum += ft;
                max = Math.max(max, ft);
                min = Math.min(min, ft);
            }
        }

        averageFrameTime = sum / (double) FRAME_HISTORY;
        maxFrameTime = max;
        minFrameTime = min == Long.MAX_VALUE ? 0 : min;
    }

    /**
     * Determines if it's a good time to trigger garbage collection.
     */
    private boolean shouldTriggerGc() {
        long now = System.currentTimeMillis();

        // Don't GC too frequently
        if (now - lastGcTime < GC_INTERVAL_MS) {
            return false;
        }

        // Good times to GC:
        // - Loading screen
        // - Pause menu
        // - Memory usage is high
        if (client.currentScreen != null) {
            // In a menu, relatively safe
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryPercent = (double) usedMemory / maxMemory;

            // Trigger if memory is above 70%
            return memoryPercent > 0.70;
        }

        return false;
    }

    /**
     * Triggers a proactive garbage collection.
     */
    private void triggerProactiveGc() {
        logger.debug("Triggering proactive GC");
        System.gc();
        lastGcTime = System.currentTimeMillis();
        metrics.incrementCounter("proactive_gc_triggered");
    }

    /**
     * Resets all statistics.
     */
    private void resetStats() {
        frameTimes = new long[FRAME_HISTORY];
        frameIndex = 0;
        lastFrameTime = 0;
        stutterCount = 0;
        averageFrameTime = 0;
        maxFrameTime = 0;
        minFrameTime = 0;
    }

    /**
     * Gets the current stutter count.
     */
    public int getStutterCount() {
        return stutterCount;
    }

    /**
     * Gets the average frame time in milliseconds.
     */
    public double getAverageFrameTimeMs() {
        return averageFrameTime / 1_000_000.0;
    }
}
