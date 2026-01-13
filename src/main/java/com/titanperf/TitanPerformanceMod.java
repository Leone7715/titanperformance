package com.titanperf;

import com.titanperf.compat.ModCompatibility;
import com.titanperf.core.config.TitanConfig;
import com.titanperf.core.controller.PerformanceController;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Main entry point for Titan Performance mod.
 *
 * This class handles the server-side and common initialization of the mod.
 * Client-specific initialization is handled by TitanPerformanceClient.
 *
 * Titan Performance is a comprehensive performance optimization mod that unifies
 * the optimization strategies of multiple popular mods into a single cohesive system.
 * It provides rendering optimization, entity culling and throttling, lighting engine
 * improvements, memory usage reduction, and dynamic FPS control.
 *
 * Architecture Overview:
 * The mod uses a modular architecture where each optimization area is encapsulated
 * in a separate module. Modules communicate through the central PerformanceController
 * which coordinates their activities and provides shared services like configuration
 * and hardware detection.
 *
 * Initialization Flow:
 * 1. Common initialization (this class) sets up controller and configuration
 * 2. Client initialization registers client-side modules and rendering hooks
 * 3. Server/World initialization registers server-side modules
 * 4. Modules are auto-configured based on detected hardware
 *
 * The design prioritizes:
 * Modularity: Each optimization can be enabled/disabled independently
 * Adaptability: Settings auto-adjust based on detected hardware
 * Compatibility: Clean architecture minimizes conflicts with other mods
 * Performance: Optimization code itself is optimized for minimal overhead
 */
public class TitanPerformanceMod implements ModInitializer {

    /**
     * Mod identifier used in logging and registration.
     */
    public static final String MOD_ID = "titanperf";

    /**
     * Logger for mod-wide messages.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger("TitanPerf");

    /**
     * Singleton controller instance managing all performance modules.
     * Accessed through getController() to ensure initialization ordering.
     */
    private static PerformanceController controller;

    /**
     * Singleton configuration instance.
     */
    private static TitanConfig config;

    /**
     * Flag tracking if we are on the client side.
     * Set during initialization based on environment.
     */
    private static boolean isClientEnvironment;

    @Override
    public void onInitialize() {
        LOGGER.info("Titan Performance initializing");
        long startTime = System.nanoTime();

        // Determine if we are on client or dedicated server
        isClientEnvironment = FabricLoader.getInstance().getEnvironmentType() ==
                net.fabricmc.api.EnvType.CLIENT;

        // Initialize configuration
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configPath = configDir.resolve("titan-performance.json");
        config = new TitanConfig(configPath);
        config.load();

        // Initialize performance controller
        controller = new PerformanceController();
        controller.initialize(config);

        // Check mod compatibility and log status
        ModCompatibility.logCompatibilityStatus();

        // Apply auto-configuration if enabled
        if (config.isAutoConfigEnabled()) {
            controller.applyAutoConfiguration();
        }

        // Register server lifecycle events
        registerServerEvents();

        long elapsed = (System.nanoTime() - startTime) / 1_000_000L;
        LOGGER.info("Titan Performance initialized in {}ms", elapsed);
    }

    /**
     * Registers server lifecycle and tick events.
     * These events drive the periodic tick processing of modules.
     */
    private void registerServerEvents() {
        // Handle server tick for server-side modules
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (controller != null && controller.isReady()) {
                controller.onTick();
            }
        });

        // Handle server shutdown for cleanup
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping, saving configuration");
            if (config != null) {
                config.saveIfDirty();
            }
        });
    }

    /**
     * Returns the performance controller instance.
     *
     * @return The controller, or null if not yet initialized
     */
    public static PerformanceController getController() {
        return controller;
    }

    /**
     * Returns the configuration instance.
     *
     * @return The configuration, or null if not yet initialized
     */
    public static TitanConfig getConfig() {
        return config;
    }

    /**
     * Returns whether the mod is running on the client side.
     *
     * @return true if on client, false if on dedicated server
     */
    public static boolean isClient() {
        return isClientEnvironment;
    }

    /**
     * Triggers a configuration save if there are pending changes.
     * Called periodically and during shutdown.
     */
    public static void saveConfig() {
        if (config != null) {
            config.saveIfDirty();
        }
    }
}
