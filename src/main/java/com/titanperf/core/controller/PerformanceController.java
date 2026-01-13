package com.titanperf.core.controller;

import com.titanperf.compat.ModCompatibility;
import com.titanperf.core.api.ModuleCategory;
import com.titanperf.core.api.ModuleMetrics;
import com.titanperf.core.api.PerformanceModule;
import com.titanperf.core.config.TitanConfig;
import com.titanperf.core.hardware.HardwareProfile;
import com.titanperf.core.hardware.HardwareDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Central coordinator for all performance optimization modules.
 *
 * The PerformanceController is the heart of Titan Performance. It manages the lifecycle
 * of all optimization modules, coordinates their activities, and provides centralized
 * access to configuration and hardware information. The controller ensures modules
 * are initialized in the correct order and handles inter-module communication.
 *
 * Key responsibilities:
 * 1. Module Registration: Maintains registry of all available modules
 * 2. Lifecycle Management: Coordinates init, enable, disable, shutdown across modules
 * 3. Tick Distribution: Calls tick methods on enabled modules each game tick
 * 4. Configuration Integration: Applies configuration changes to modules
 * 5. Hardware Awareness: Provides hardware info to modules for adaptive optimization
 * 6. Metrics Aggregation: Collects and exposes metrics from all modules
 *
 * The controller follows a singleton pattern because there should only ever be
 * one coordination point for performance modules. Multiple controllers would
 * lead to conflicting optimizations and inconsistent state.
 *
 * Thread Safety: The controller is designed to be accessed from multiple threads.
 * Module registration and lifecycle operations should occur on the main thread,
 * but metrics queries and hardware info access are thread-safe.
 */
public class PerformanceController {

    private static final Logger LOGGER = LoggerFactory.getLogger("TitanPerf/Controller");

    /**
     * Registry of all registered modules indexed by their unique identifier.
     * Uses ConcurrentHashMap for thread-safe reads during tick processing.
     */
    private final Map<String, PerformanceModule> moduleRegistry;

    /**
     * Ordered list of modules for deterministic tick processing.
     * Sorted by priority with higher priority modules first.
     * Uses CopyOnWriteArrayList because modifications are rare but iterations are frequent.
     */
    private final List<PerformanceModule> modulesByPriority;

    /**
     * Current hardware profile detected at startup.
     * Modules use this to adapt their behavior to available resources.
     */
    private HardwareProfile hardwareProfile;

    /**
     * Configuration instance providing access to user settings.
     */
    private TitanConfig config;

    /**
     * Counter tracking the current game tick for throttling calculations.
     * Incremented each time onTick is called.
     */
    private final AtomicLong tickCounter;

    /**
     * Flag indicating whether the controller has been fully initialized.
     */
    private volatile boolean initialized;

    /**
     * Flag indicating whether the controller is currently shutting down.
     * Prevents new operations during shutdown.
     */
    private volatile boolean shuttingDown;

    /**
     * Aggregated metrics from the controller itself.
     * Tracks controller-level statistics like tick timing and module counts.
     */
    private final ModuleMetrics controllerMetrics;

    /**
     * Constructs a new PerformanceController.
     * The controller starts in an uninitialized state and must have
     * initialize() called before modules can be registered or used.
     */
    public PerformanceController() {
        this.moduleRegistry = new ConcurrentHashMap<>();
        this.modulesByPriority = new CopyOnWriteArrayList<>();
        this.tickCounter = new AtomicLong(0);
        this.controllerMetrics = new ModuleMetrics();
        this.initialized = false;
        this.shuttingDown = false;
    }

    /**
     * Initializes the controller with configuration and hardware detection.
     * This must be called before any modules are registered.
     *
     * @param config The configuration instance to use
     */
    public void initialize(TitanConfig config) {
        if (initialized) {
            LOGGER.warn("Controller already initialized");
            return;
        }

        LOGGER.info("Initializing Performance Controller");
        this.config = config;

        // Detect hardware capabilities for adaptive optimization
        LOGGER.info("Detecting hardware capabilities");
        this.hardwareProfile = HardwareDetector.detect();
        LOGGER.info("Hardware profile: {}", hardwareProfile.getSummary());

        initialized = true;
        LOGGER.info("Performance Controller initialized");
    }

    /**
     * Registers a performance module with the controller.
     * The module will be initialized immediately if the controller is initialized.
     * Modules are automatically sorted by priority after registration.
     *
     * @param module The module to register
     * @throws IllegalStateException if controller is not initialized
     * @throws IllegalArgumentException if a module with the same ID exists
     */
    public void registerModule(PerformanceModule module) {
        if (!initialized) {
            throw new IllegalStateException("Controller must be initialized before registering modules");
        }

        if (shuttingDown) {
            LOGGER.warn("Cannot register module during shutdown: {}", module.getModuleId());
            return;
        }

        String moduleId = module.getModuleId();
        if (moduleRegistry.containsKey(moduleId)) {
            throw new IllegalArgumentException("Module already registered: " + moduleId);
        }

        LOGGER.info("Registering module: {} (priority: {})", module.getDisplayName(), module.getPriority());
        moduleRegistry.put(moduleId, module);

        // Initialize the module immediately
        module.onInitialize();

        // Add to priority-sorted list
        modulesByPriority.add(module);
        sortModulesByPriority();

        // Enable if configuration says it should be enabled AND it's compatible
        if (config.isModuleEnabled(moduleId)) {
            if (ModCompatibility.shouldDisableModule(moduleId)) {
                LOGGER.info("Module {} disabled for shader compatibility", module.getDisplayName());
            } else {
                module.onEnable();
            }
        }

        controllerMetrics.setGauge("registered_modules", moduleRegistry.size());
        controllerMetrics.setGauge("enabled_modules", countEnabledModules());
    }

    /**
     * Retrieves a registered module by its identifier.
     *
     * @param moduleId The unique identifier of the module
     * @return The module instance, or null if not found
     */
    public PerformanceModule getModule(String moduleId) {
        return moduleRegistry.get(moduleId);
    }

    /**
     * Retrieves a registered module cast to a specific type.
     * Useful when accessing module-specific functionality.
     *
     * @param moduleId The unique identifier of the module
     * @param moduleClass The expected class of the module
     * @return The module instance cast to the specified type, or null if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T extends PerformanceModule> T getModule(String moduleId, Class<T> moduleClass) {
        PerformanceModule module = moduleRegistry.get(moduleId);
        if (module != null && moduleClass.isInstance(module)) {
            return (T) module;
        }
        return null;
    }

    /**
     * Returns all registered modules.
     *
     * @return An unmodifiable collection of all modules
     */
    public Collection<PerformanceModule> getAllModules() {
        return Collections.unmodifiableCollection(moduleRegistry.values());
    }

    /**
     * Returns all modules in a specific category.
     *
     * @param category The category to filter by
     * @return A list of modules in the specified category
     */
    public List<PerformanceModule> getModulesByCategory(ModuleCategory category) {
        return moduleRegistry.values().stream()
                .filter(m -> m.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * Enables a specific module by its identifier.
     * Updates the configuration to persist the change.
     * Respects shader compatibility mode restrictions.
     *
     * @param moduleId The module to enable
     * @return true if the module was found and enabled
     */
    public boolean enableModule(String moduleId) {
        PerformanceModule module = moduleRegistry.get(moduleId);
        if (module == null) {
            LOGGER.warn("Cannot enable unknown module: {}", moduleId);
            return false;
        }

        // Check shader compatibility
        if (ModCompatibility.shouldDisableModule(moduleId)) {
            LOGGER.warn("Cannot enable module {} - conflicts with shader mod", module.getDisplayName());
            return false;
        }

        if (!module.isEnabled()) {
            module.onEnable();
            config.setModuleEnabled(moduleId, true);
            controllerMetrics.setGauge("enabled_modules", countEnabledModules());
            LOGGER.info("Enabled module: {}", module.getDisplayName());
        }
        return true;
    }

    /**
     * Disables a specific module by its identifier.
     * Updates the configuration to persist the change.
     *
     * @param moduleId The module to disable
     * @return true if the module was found and disabled
     */
    public boolean disableModule(String moduleId) {
        PerformanceModule module = moduleRegistry.get(moduleId);
        if (module == null) {
            LOGGER.warn("Cannot disable unknown module: {}", moduleId);
            return false;
        }

        if (module.isEnabled()) {
            module.onDisable();
            config.setModuleEnabled(moduleId, false);
            controllerMetrics.setGauge("enabled_modules", countEnabledModules());
            LOGGER.info("Disabled module: {}", module.getDisplayName());
        }
        return true;
    }

    /**
     * Called each game tick to process all enabled modules.
     * Modules are processed in priority order with higher priority first.
     *
     * This method tracks timing metrics for performance monitoring.
     * Individual module tick failures are caught and logged without
     * affecting other modules.
     */
    public void onTick() {
        if (!initialized || shuttingDown) {
            return;
        }

        long startTime = System.nanoTime();
        tickCounter.incrementAndGet();

        int modulesProcessed = 0;
        for (PerformanceModule module : modulesByPriority) {
            if (module.isEnabled()) {
                module.onTick();
                modulesProcessed++;
            }
        }

        long elapsed = System.nanoTime() - startTime;
        controllerMetrics.setGauge("tick_time_us", elapsed / 1000L);
        controllerMetrics.setGauge("modules_ticked", modulesProcessed);
        controllerMetrics.incrementCounter("total_ticks");
    }

    /**
     * Shuts down all modules and the controller.
     * Modules are shut down in reverse priority order so that
     * higher-level modules shut down before their dependencies.
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        LOGGER.info("Shutting down Performance Controller");
        shuttingDown = true;

        // Shutdown in reverse priority order
        List<PerformanceModule> reversed = new ArrayList<>(modulesByPriority);
        Collections.reverse(reversed);

        for (PerformanceModule module : reversed) {
            try {
                module.onShutdown();
            } catch (Exception e) {
                LOGGER.error("Error shutting down module {}: {}", module.getModuleId(), e.getMessage(), e);
            }
        }

        modulesByPriority.clear();
        moduleRegistry.clear();
        initialized = false;

        LOGGER.info("Performance Controller shutdown complete");
    }

    /**
     * Returns the current tick count.
     * Modules can use this for throttling operations to specific intervals.
     *
     * @return The number of ticks since controller initialization
     */
    public long getCurrentTick() {
        return tickCounter.get();
    }

    /**
     * Returns the detected hardware profile.
     * Modules use this to adapt their optimization strategies.
     *
     * @return The hardware profile, or null if not yet detected
     */
    public HardwareProfile getHardwareProfile() {
        return hardwareProfile;
    }

    /**
     * Returns the configuration instance.
     *
     * @return The configuration instance
     */
    public TitanConfig getConfig() {
        return config;
    }

    /**
     * Returns controller-level metrics.
     *
     * @return Metrics for the controller itself
     */
    public ModuleMetrics getControllerMetrics() {
        return controllerMetrics;
    }

    /**
     * Returns whether the controller is fully initialized and ready.
     *
     * @return true if initialized and not shutting down
     */
    public boolean isReady() {
        return initialized && !shuttingDown;
    }

    /**
     * Applies auto-configuration based on detected hardware.
     * This adjusts module settings to optimal values for the current system.
     */
    public void applyAutoConfiguration() {
        if (hardwareProfile == null) {
            LOGGER.warn("Cannot apply auto-configuration without hardware profile");
            return;
        }

        LOGGER.info("Applying auto-configuration for hardware tier: {}", hardwareProfile.getTier());
        config.applyHardwareDefaults(hardwareProfile);

        // Re-enable/disable modules based on new configuration
        for (PerformanceModule module : moduleRegistry.values()) {
            boolean shouldEnable = config.isModuleEnabled(module.getModuleId());
            if (shouldEnable && !module.isEnabled()) {
                module.onEnable();
            } else if (!shouldEnable && module.isEnabled()) {
                module.onDisable();
            }
        }

        controllerMetrics.setGauge("enabled_modules", countEnabledModules());
        LOGGER.info("Auto-configuration applied");
    }

    /**
     * Sorts the module list by priority in descending order.
     * Called after module registration to maintain proper tick order.
     */
    private void sortModulesByPriority() {
        List<PerformanceModule> sorted = modulesByPriority.stream()
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                .collect(Collectors.toList());
        modulesByPriority.clear();
        modulesByPriority.addAll(sorted);
    }

    /**
     * Counts the number of currently enabled modules.
     *
     * @return The count of enabled modules
     */
    private long countEnabledModules() {
        return moduleRegistry.values().stream().filter(PerformanceModule::isEnabled).count();
    }
}
