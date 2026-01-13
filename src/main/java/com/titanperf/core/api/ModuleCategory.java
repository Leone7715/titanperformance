package com.titanperf.core.api;

/**
 * Categories for grouping performance modules by their optimization target.
 *
 * Categories serve two purposes:
 * 1. Organizational: Modules are grouped in configuration UIs by category
 * 2. Coordination: The PerformanceController may make decisions based on
 *    which categories of modules are active and their collective impact
 *
 * The categories are ordered by their typical impact on game performance,
 * with rendering typically having the largest effect and memory having
 * more subtle but cumulative benefits.
 */
public enum ModuleCategory {

    /**
     * Modules that optimize the rendering pipeline including chunk rendering,
     * frustum culling, draw call batching, and shader optimizations.
     * These modules typically have the largest impact on FPS.
     */
    RENDERING("Rendering", "Optimizations for the rendering pipeline and graphics"),

    /**
     * Modules that optimize entity processing including tick throttling,
     * visibility culling, and AI computation reduction.
     * Critical for servers and worlds with many entities.
     */
    ENTITY("Entity", "Optimizations for entity updates and rendering"),

    /**
     * Modules that optimize the lighting engine including light propagation,
     * sky light calculation, and light update batching.
     * Important for dynamic lighting scenarios and chunk loading.
     */
    LIGHTING("Lighting", "Optimizations for light calculation and propagation"),

    /**
     * Modules that reduce memory allocation and improve garbage collection
     * behavior including object pooling, cache management, and data deduplication.
     * Reduces GC pauses and overall memory footprint.
     */
    MEMORY("Memory", "Optimizations for memory usage and allocation patterns"),

    /**
     * Modules that control frame rate and resource usage based on game state
     * including background FPS limiting, menu throttling, and idle detection.
     * Reduces power consumption and system resource usage when appropriate.
     */
    FPS_CONTROL("FPS Control", "Dynamic frame rate and resource management"),

    /**
     * Modules that provide supporting functionality such as hardware detection,
     * auto-configuration, and performance monitoring.
     * These modules support other optimization modules rather than
     * directly improving performance themselves.
     */
    SYSTEM("System", "System detection, configuration, and monitoring");

    private final String displayName;
    private final String description;

    ModuleCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns the human-readable name for this category.
     *
     * @return The display name for UI presentation
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a description of what this category encompasses.
     *
     * @return A brief description of the category
     */
    public String getDescription() {
        return description;
    }
}
