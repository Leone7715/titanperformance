package com.titanperf.core.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe container for performance metrics collected by optimization modules.
 *
 * Metrics serve multiple purposes in Titan Performance:
 * 1. Debug Display: Shown in the F3 debug screen overlay
 * 2. Auto-Configuration: Used to adjust optimization settings dynamically
 * 3. User Feedback: Displayed in configuration screens to show optimization impact
 * 4. Logging: Written to logs for performance analysis
 *
 * All metric updates are thread-safe to support modules that operate on
 * worker threads separate from the main game thread. Metrics use atomic
 * operations to avoid lock contention while maintaining consistency.
 *
 * Common metric patterns:
 * Counters track cumulative totals like "entities_culled" or "chunks_skipped"
 * Gauges track current values like "active_chunk_count" or "memory_saved_mb"
 * Timers track durations like "avg_tick_time_us" or "last_update_ms"
 */
public class ModuleMetrics {

    /**
     * Storage for counter metrics that accumulate over time.
     * Uses AtomicLong for thread-safe increment operations.
     */
    private final Map<String, AtomicLong> counters;

    /**
     * Storage for gauge metrics that represent current state.
     * Uses AtomicLong for thread-safe set operations.
     * Floating point values are stored as fixed-point integers with 3 decimal places.
     */
    private final Map<String, AtomicLong> gauges;

    /**
     * Timestamp when metrics collection began.
     * Used to calculate rates and averages over time.
     */
    private volatile long startTimeNanos;

    /**
     * Multiplier for storing floating point values as fixed-point integers.
     * Allows storing values like 3.141 as 3141 for atomic operations.
     */
    private static final long FIXED_POINT_SCALE = 1000L;

    /**
     * Creates a new ModuleMetrics instance with empty metric collections.
     * The start time is set to the current system time.
     */
    public ModuleMetrics() {
        this.counters = new ConcurrentHashMap<>();
        this.gauges = new ConcurrentHashMap<>();
        this.startTimeNanos = System.nanoTime();
    }

    /**
     * Increments a counter metric by one.
     * Counter metrics accumulate over time and are never decremented.
     * Use counters for tracking total occurrences of events.
     *
     * @param name The metric name such as "entities_culled"
     */
    public void incrementCounter(String name) {
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Increments a counter metric by a specified amount.
     * Use this when multiple events occur in a single operation.
     *
     * @param name The metric name
     * @param amount The amount to add to the counter
     */
    public void addToCounter(String name, long amount) {
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(amount);
    }

    /**
     * Gets the current value of a counter metric.
     *
     * @param name The metric name
     * @return The current counter value, or 0 if the counter does not exist
     */
    public long getCounter(String name) {
        AtomicLong counter = counters.get(name);
        return counter != null ? counter.get() : 0L;
    }

    /**
     * Sets a gauge metric to an integer value.
     * Gauge metrics represent current state and can increase or decrease.
     * Use gauges for values like "active_entities" or "cached_chunks".
     *
     * @param name The metric name
     * @param value The current value to set
     */
    public void setGauge(String name, long value) {
        gauges.computeIfAbsent(name, k -> new AtomicLong(0)).set(value);
    }

    /**
     * Sets a gauge metric to a floating point value.
     * The value is stored as a fixed-point integer internally.
     *
     * @param name The metric name
     * @param value The current value to set
     */
    public void setGaugeFloat(String name, double value) {
        long fixedPoint = (long) (value * FIXED_POINT_SCALE);
        gauges.computeIfAbsent(name, k -> new AtomicLong(0)).set(fixedPoint);
    }

    /**
     * Gets the current value of a gauge metric as an integer.
     *
     * @param name The metric name
     * @return The current gauge value, or 0 if the gauge does not exist
     */
    public long getGauge(String name) {
        AtomicLong gauge = gauges.get(name);
        return gauge != null ? gauge.get() : 0L;
    }

    /**
     * Gets the current value of a gauge metric as a floating point number.
     * Use this for gauges that were set with setGaugeFloat().
     *
     * @param name The metric name
     * @return The current gauge value as a double, or 0.0 if not found
     */
    public double getGaugeFloat(String name) {
        AtomicLong gauge = gauges.get(name);
        return gauge != null ? gauge.get() / (double) FIXED_POINT_SCALE : 0.0;
    }

    /**
     * Calculates the rate of a counter metric per second.
     * Useful for displaying rates like "chunks_per_second".
     *
     * @param name The counter metric name
     * @return The rate per second based on elapsed time since start
     */
    public double getCounterRatePerSecond(String name) {
        long elapsedNanos = System.nanoTime() - startTimeNanos;
        if (elapsedNanos <= 0) {
            return 0.0;
        }
        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        return getCounter(name) / elapsedSeconds;
    }

    /**
     * Returns the elapsed time since metrics collection began.
     *
     * @return Elapsed time in milliseconds
     */
    public long getElapsedTimeMs() {
        return (System.nanoTime() - startTimeNanos) / 1_000_000L;
    }

    /**
     * Resets all metrics to their initial state.
     * Clears all counters and gauges and resets the start time.
     */
    public void reset() {
        counters.clear();
        gauges.clear();
        startTimeNanos = System.nanoTime();
    }

    /**
     * Returns an immutable copy of all counter names and values.
     * Useful for serialization and display.
     *
     * @return A map of counter names to their current values
     */
    public Map<String, Long> getAllCounters() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        counters.forEach((name, value) -> result.put(name, value.get()));
        return result;
    }

    /**
     * Returns an immutable copy of all gauge names and values.
     * Useful for serialization and display.
     *
     * @return A map of gauge names to their current values
     */
    public Map<String, Long> getAllGauges() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        gauges.forEach((name, value) -> result.put(name, value.get()));
        return result;
    }

    /**
     * Creates a formatted string representation of all metrics.
     * Useful for logging and debug display.
     *
     * @return A multi-line string with all metric values
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Counters:\n");
        counters.forEach((name, value) ->
            sb.append("  ").append(name).append(": ").append(value.get()).append("\n"));
        sb.append("Gauges:\n");
        gauges.forEach((name, value) ->
            sb.append("  ").append(name).append(": ").append(value.get()).append("\n"));
        sb.append("Elapsed: ").append(getElapsedTimeMs()).append("ms\n");
        return sb.toString();
    }
}
