package com.titanperf.core.api;

/**
 * Base interface for all performance optimization modules in Titan Performance.
 *
 * Each module represents a distinct area of optimization such as rendering,
 * entity processing, lighting, memory management, or FPS control. Modules
 * are designed to be independent units that can be enabled or disabled
 * without affecting other modules, though they may communicate through
 * the central PerformanceController for coordinated optimization decisions.
 *
 * The module lifecycle follows a strict pattern:
 * 1. Construction: Module is instantiated but not yet active
 * 2. Initialization: onInitialize() is called, module prepares resources
 * 3. Enable: onEnable() is called when the module becomes active
 * 4. Tick: onTick() is called each game tick while enabled
 * 5. Disable: onDisable() is called when module is deactivated
 * 6. Shutdown: onShutdown() is called during mod unload
 *
 * Modules should be designed to handle rapid enable/disable cycles gracefully,
 * as users may toggle modules through the configuration interface.
 */
public interface PerformanceModule {

    /**
     * Returns the unique identifier for this module.
     * This identifier is used in configuration files, logging, and inter-module
     * communication. It should be lowercase with underscores separating words.
     *
     * @return A unique string identifier such as "rendering_optimizer" or "entity_culler"
     */
    String getModuleId();

    /**
     * Returns a human-readable name for display in user interfaces.
     * This name appears in configuration screens and log messages.
     *
     * @return A display name such as "Rendering Optimizer" or "Entity Culler"
     */
    String getDisplayName();

    /**
     * Returns the category this module belongs to for organizational purposes.
     * Categories help group related modules in configuration interfaces.
     *
     * @return The module category enum value
     */
    ModuleCategory getCategory();

    /**
     * Called once when the mod is first loaded.
     * Use this for one-time setup such as registering event handlers,
     * creating data structures, or performing compatibility checks.
     * This method is called before any modules are enabled.
     */
    void onInitialize();

    /**
     * Called when the module is enabled either at startup or through
     * runtime configuration changes. The module should begin its
     * optimization work after this method completes.
     *
     * Implementations should be idempotent. Calling onEnable() multiple
     * times without intervening onDisable() calls should have no effect.
     */
    void onEnable();

    /**
     * Called when the module is disabled through configuration changes.
     * The module should cease all optimization work and release any
     * resources that are only needed while active.
     *
     * Implementations should be idempotent. Calling onDisable() multiple
     * times without intervening onEnable() calls should have no effect.
     */
    void onDisable();

    /**
     * Called once per game tick while the module is enabled.
     * This is the primary hook for modules that need to perform
     * periodic work. Heavy computations should be distributed across
     * multiple ticks to avoid causing lag spikes.
     *
     * The tick rate is nominally 20 ticks per second but may vary
     * under load. Modules should not assume a fixed tick interval.
     */
    void onTick();

    /**
     * Called during mod shutdown to release all resources.
     * This is the final lifecycle method called on the module.
     * After this method returns, the module instance may be discarded.
     */
    void onShutdown();

    /**
     * Returns whether this module is currently enabled and actively
     * performing optimizations.
     *
     * @return true if the module is enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Returns the current priority level of this module.
     * Higher priority modules are initialized and ticked first.
     * Priority can be used to ensure dependency ordering between modules.
     *
     * @return An integer priority value where higher means more important
     */
    int getPriority();

    /**
     * Returns performance metrics collected by this module.
     * Metrics are used for display in debug screens and for
     * auto-configuration decisions.
     *
     * @return A ModuleMetrics object containing current statistics
     */
    ModuleMetrics getMetrics();

    /**
     * Resets all performance metrics to their initial state.
     * Called when the user requests a metrics reset or when
     * significant configuration changes occur.
     */
    void resetMetrics();
}
