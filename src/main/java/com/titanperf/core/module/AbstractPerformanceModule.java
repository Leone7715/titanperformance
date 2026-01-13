package com.titanperf.core.module;

import com.titanperf.core.api.ModuleCategory;
import com.titanperf.core.api.ModuleMetrics;
import com.titanperf.core.api.PerformanceModule;
import com.titanperf.TitanPerformanceMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class providing common functionality for performance modules.
 *
 * This class handles the boilerplate aspects of module implementation including
 * state management, metrics tracking, logging setup, and lifecycle enforcement.
 * Concrete modules should extend this class and implement the abstract methods
 * for their specific optimization logic.
 *
 * The base class provides several benefits:
 * 1. Consistent lifecycle management with proper state checking
 * 2. Automatic metrics initialization and management
 * 3. Standardized logging with module-specific prefixes
 * 4. Default implementations for common functionality
 *
 * Subclasses must implement:
 * initializeModule() for one-time setup logic
 * enableModule() for activation logic
 * disableModule() for deactivation logic
 * tickModule() for per-tick processing
 * shutdownModule() for cleanup logic
 */
public abstract class AbstractPerformanceModule implements PerformanceModule {

    /**
     * Logger instance for this module.
     * Subclasses should use this logger for all logging operations.
     */
    protected final Logger logger;

    /**
     * Metrics collector for this module.
     * Subclasses should record their performance data here.
     */
    protected final ModuleMetrics metrics;

    /**
     * Current enabled state of this module.
     * Modified only through enable/disable lifecycle methods.
     */
    private volatile boolean enabled;

    /**
     * Flag indicating whether initialization has completed.
     * Prevents double initialization and ensures proper lifecycle ordering.
     */
    private volatile boolean initialized;

    /**
     * Module identifier used in configuration and logging.
     */
    private final String moduleId;

    /**
     * Human readable name for UI display.
     */
    private final String displayName;

    /**
     * Category for grouping in configuration interfaces.
     */
    private final ModuleCategory category;

    /**
     * Priority for initialization and tick ordering.
     * Higher values indicate higher priority.
     */
    private final int priority;

    /**
     * Constructs a new module with the specified identification and priority.
     *
     * @param moduleId Unique identifier for this module in lowercase with underscores
     * @param displayName Human readable name for display
     * @param category The optimization category this module belongs to
     * @param priority Initialization and tick priority where higher runs first
     */
    protected AbstractPerformanceModule(String moduleId, String displayName,
                                        ModuleCategory category, int priority) {
        this.moduleId = moduleId;
        this.displayName = displayName;
        this.category = category;
        this.priority = priority;
        this.metrics = new ModuleMetrics();
        this.enabled = false;
        this.initialized = false;
        this.logger = LoggerFactory.getLogger("TitanPerf/" + displayName);
    }

    @Override
    public final String getModuleId() {
        return moduleId;
    }

    @Override
    public final String getDisplayName() {
        return displayName;
    }

    @Override
    public final ModuleCategory getCategory() {
        return category;
    }

    @Override
    public final int getPriority() {
        return priority;
    }

    @Override
    public final boolean isEnabled() {
        return enabled;
    }

    @Override
    public final ModuleMetrics getMetrics() {
        return metrics;
    }

    @Override
    public final void resetMetrics() {
        metrics.reset();
        logger.debug("Metrics reset for module {}", moduleId);
    }

    /**
     * Initializes the module. This method handles lifecycle state management
     * and delegates to the subclass implementation.
     *
     * Thread Safety: This method should only be called from the main thread
     * during mod initialization. It is not thread-safe for concurrent calls.
     */
    @Override
    public final void onInitialize() {
        if (initialized) {
            logger.warn("Module {} already initialized, skipping", moduleId);
            return;
        }

        logger.info("Initializing module: {}", displayName);
        long startTime = System.nanoTime();

        try {
            initializeModule();
            initialized = true;
            long elapsed = (System.nanoTime() - startTime) / 1_000_000L;
            logger.info("Module {} initialized in {}ms", displayName, elapsed);
        } catch (Exception e) {
            logger.error("Failed to initialize module {}: {}", moduleId, e.getMessage(), e);
            // Module remains uninitialized and cannot be enabled
        }
    }

    /**
     * Enables the module. This method handles lifecycle state management
     * and delegates to the subclass implementation.
     *
     * Thread Safety: This method should only be called from the main thread.
     * Enabling a module from a worker thread may cause race conditions.
     */
    @Override
    public final void onEnable() {
        if (!initialized) {
            logger.error("Cannot enable module {} before initialization", moduleId);
            return;
        }

        if (enabled) {
            logger.debug("Module {} already enabled", moduleId);
            return;
        }

        logger.info("Enabling module: {}", displayName);
        try {
            enableModule();
            enabled = true;
            metrics.incrementCounter("enable_count");
            logger.info("Module {} enabled", displayName);
        } catch (Exception e) {
            logger.error("Failed to enable module {}: {}", moduleId, e.getMessage(), e);
        }
    }

    /**
     * Disables the module. This method handles lifecycle state management
     * and delegates to the subclass implementation.
     *
     * Thread Safety: This method should only be called from the main thread.
     */
    @Override
    public final void onDisable() {
        if (!enabled) {
            logger.debug("Module {} already disabled", moduleId);
            return;
        }

        logger.info("Disabling module: {}", displayName);
        try {
            disableModule();
            enabled = false;
            logger.info("Module {} disabled", displayName);
        } catch (Exception e) {
            logger.error("Failed to disable module {}: {}", moduleId, e.getMessage(), e);
            // Force disable even on error to maintain consistent state
            enabled = false;
        }
    }

    /**
     * Processes one tick of module logic. This method handles lifecycle
     * checking and delegates to the subclass implementation.
     *
     * Thread Safety: This method is called from the main thread during
     * the tick phase. Subclass implementations should avoid blocking operations.
     */
    @Override
    public final void onTick() {
        if (!enabled) {
            return;
        }

        long startTime = System.nanoTime();
        try {
            tickModule();
            long elapsed = System.nanoTime() - startTime;
            // Track tick timing in microseconds for precision
            metrics.setGauge("last_tick_time_us", elapsed / 1000L);
        } catch (Exception e) {
            logger.error("Error during tick for module {}: {}", moduleId, e.getMessage(), e);
            metrics.incrementCounter("tick_errors");
        }
    }

    /**
     * Shuts down the module and releases all resources.
     *
     * Thread Safety: This method should only be called from the main thread
     * during mod shutdown. It ensures proper cleanup regardless of current state.
     */
    @Override
    public final void onShutdown() {
        logger.info("Shutting down module: {}", displayName);

        // Ensure disabled before shutdown
        if (enabled) {
            onDisable();
        }

        try {
            shutdownModule();
            logger.info("Module {} shutdown complete", displayName);
        } catch (Exception e) {
            logger.error("Error during shutdown for module {}: {}", moduleId, e.getMessage(), e);
        }

        initialized = false;
    }

    /**
     * Called once during module initialization.
     * Subclasses should perform one-time setup here such as registering
     * event handlers, creating data structures, or checking compatibility.
     *
     * This method is called before the module can be enabled.
     * Throwing an exception will prevent the module from being usable.
     */
    protected abstract void initializeModule();

    /**
     * Called when the module is enabled.
     * Subclasses should start their optimization work here.
     * This may be called multiple times during a session as users
     * toggle modules on and off.
     */
    protected abstract void enableModule();

    /**
     * Called when the module is disabled.
     * Subclasses should stop their optimization work and release
     * any resources that are only needed while active.
     */
    protected abstract void disableModule();

    /**
     * Called once per game tick while the module is enabled.
     * Subclasses should implement their periodic processing here.
     * Heavy work should be distributed across ticks or delegated
     * to worker threads to avoid causing lag.
     */
    protected abstract void tickModule();

    /**
     * Called during mod shutdown to release all resources.
     * Subclasses should perform final cleanup here.
     * This is called after onDisable() if the module was enabled.
     */
    protected abstract void shutdownModule();

    /**
     * Utility method to check if the game is currently on the client side.
     * Useful for modules that have different behavior on client vs server.
     *
     * @return true if running on the client, false if on a dedicated server
     */
    protected boolean isClient() {
        return TitanPerformanceMod.isClient();
    }

    /**
     * Utility method to get the current game tick count.
     * Useful for modules that need to throttle operations to specific tick intervals.
     *
     * @return The current tick count from the performance controller
     */
    protected long getCurrentTick() {
        return TitanPerformanceMod.getController().getCurrentTick();
    }
}
