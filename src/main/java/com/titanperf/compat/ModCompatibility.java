package com.titanperf.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Detects installed mods for compatibility handling.
 *
 * Titan Performance can operate in compatibility mode with shader mods like Iris.
 * When Iris is detected, conflicting rendering optimizations are automatically
 * disabled while non-conflicting features remain active.
 *
 * This approach allows users to benefit from Titan Performance's memory optimization,
 * entity throttling, and other non-rendering features even when using shaders.
 *
 * Compatibility Notes:
 * - Iris + Sodium: Rendering modules disabled, other modules enabled
 * - Standalone Sodium: All Titan modules disabled (hard conflict)
 * - No shader mod: Full functionality enabled
 *
 * Legal: Iris is licensed under LGPL-3.0 (same as Titan Performance).
 * This compatibility detection only checks if the mod is loaded and doesn't
 * include any Iris code.
 */
public final class ModCompatibility {

    private static final Logger LOGGER = LoggerFactory.getLogger("TitanPerf/Compat");

    /**
     * Cached detection results for performance.
     */
    private static Boolean irisLoaded = null;
    private static Boolean sodiumLoaded = null;

    /**
     * Modules that conflict with Sodium's rendering system.
     */
    private static final Set<String> SODIUM_CONFLICTING_MODULES = Set.of(
        "rendering_optimizer",
        "lighting_optimizer"
    );

    /**
     * Modules that are safe to use alongside Iris/Sodium.
     */
    private static final Set<String> IRIS_COMPATIBLE_MODULES = Set.of(
        "entity_culler",
        "entity_throttler",
        "memory_optimizer",
        "dynamic_fps",
        "particle_optimizer",
        "smooth_fps"
    );

    private ModCompatibility() {
        // Utility class
    }

    /**
     * Checks if Iris Shaders mod is installed.
     *
     * @return true if Iris is detected
     */
    public static boolean isIrisLoaded() {
        if (irisLoaded == null) {
            irisLoaded = FabricLoader.getInstance().isModLoaded("iris");
            if (irisLoaded) {
                LOGGER.info("Iris Shaders detected - enabling compatibility mode");
            }
        }
        return irisLoaded;
    }

    /**
     * Checks if Sodium mod is installed.
     *
     * @return true if Sodium is detected
     */
    public static boolean isSodiumLoaded() {
        if (sodiumLoaded == null) {
            sodiumLoaded = FabricLoader.getInstance().isModLoaded("sodium");
            if (sodiumLoaded) {
                LOGGER.info("Sodium detected");
            }
        }
        return sodiumLoaded;
    }

    /**
     * Determines if shader compatibility mode should be active.
     * This mode disables rendering optimizations that would conflict with
     * Iris/Sodium while keeping non-conflicting optimizations active.
     *
     * @return true if compatibility mode should be enabled
     */
    public static boolean isShaderCompatMode() {
        return isIrisLoaded() && isSodiumLoaded();
    }

    /**
     * Checks if a specific module should be disabled due to mod conflicts.
     *
     * @param moduleId The module identifier to check
     * @return true if the module should be disabled
     */
    public static boolean shouldDisableModule(String moduleId) {
        if (isShaderCompatMode()) {
            // In shader compat mode, disable rendering-related modules
            return SODIUM_CONFLICTING_MODULES.contains(moduleId);
        }
        return false;
    }

    /**
     * Checks if a module is compatible with the current mod environment.
     *
     * @param moduleId The module identifier to check
     * @return true if the module can be safely enabled
     */
    public static boolean isModuleCompatible(String moduleId) {
        if (!isShaderCompatMode()) {
            // No shader mods, everything is compatible
            return true;
        }
        return IRIS_COMPATIBLE_MODULES.contains(moduleId);
    }

    /**
     * Returns a set of module IDs that should be auto-disabled for compatibility.
     *
     * @return Set of module IDs to disable
     */
    public static Set<String> getModulesToDisable() {
        if (isShaderCompatMode()) {
            return new HashSet<>(SODIUM_CONFLICTING_MODULES);
        }
        return Set.of();
    }

    /**
     * Returns a description of the current compatibility status.
     *
     * @return Human-readable compatibility status
     */
    public static String getCompatibilityStatus() {
        if (isShaderCompatMode()) {
            return "Iris Compatibility Mode (some features disabled)";
        } else if (isSodiumLoaded()) {
            return "Sodium Conflict Detected";
        } else if (isIrisLoaded()) {
            return "Iris Detected (standalone)";
        }
        return "Full Functionality";
    }

    /**
     * Logs the current compatibility state for debugging.
     */
    public static void logCompatibilityStatus() {
        LOGGER.info("Mod Compatibility Status: {}", getCompatibilityStatus());
        LOGGER.info("  Iris loaded: {}", isIrisLoaded());
        LOGGER.info("  Sodium loaded: {}", isSodiumLoaded());
        LOGGER.info("  Shader compat mode: {}", isShaderCompatMode());

        if (isShaderCompatMode()) {
            LOGGER.info("  Disabled modules: {}", SODIUM_CONFLICTING_MODULES);
            LOGGER.info("  Active modules: {}", IRIS_COMPATIBLE_MODULES);
        }
    }
}
