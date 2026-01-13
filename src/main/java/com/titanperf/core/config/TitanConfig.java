package com.titanperf.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.titanperf.core.hardware.HardwareProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified configuration system for Titan Performance.
 *
 * This class manages all configuration options for the mod including module
 * enable/disable states, per-module settings, and global options. Configuration
 * is persisted to a single JSON file and supports both automatic and manual
 * configuration modes.
 *
 * Configuration Architecture:
 * 1. Global Settings: Apply to the entire mod (auto-config mode, debug flags)
 * 2. Module States: Enable/disable individual modules
 * 3. Module Settings: Per-module configuration values
 * 4. Hardware Presets: Pre-configured settings for different hardware tiers
 *
 * Auto-Configuration Mode:
 * When enabled, the mod automatically adjusts settings based on detected hardware.
 * Users who want manual control can disable auto-config and set values explicitly.
 * Auto-config can be re-triggered at any time to reset to recommended values.
 *
 * Thread Safety:
 * Configuration reads are thread-safe through ConcurrentHashMap usage.
 * Configuration writes should occur on the main thread to avoid conflicts.
 * The save operation is atomic to prevent corruption.
 */
public class TitanConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("TitanPerf/Config");

    /**
     * Configuration file version for migration support.
     * Increment when making breaking changes to configuration structure.
     */
    private static final int CONFIG_VERSION = 1;

    /**
     * Gson instance for JSON serialization with pretty printing.
     */
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /**
     * Path to the configuration file.
     */
    private final Path configPath;

    /**
     * Global configuration options.
     */
    private final GlobalSettings globalSettings;

    /**
     * Module enable/disable states indexed by module ID.
     */
    private final Map<String, Boolean> moduleStates;

    /**
     * Per-module configuration settings.
     * Outer map: module ID, Inner map: setting name to value
     */
    private final Map<String, Map<String, Object>> moduleSettings;

    /**
     * Flag indicating if configuration has unsaved changes.
     */
    private volatile boolean dirty;

    /**
     * Constructs a new configuration manager.
     *
     * @param configPath Path to the configuration file
     */
    public TitanConfig(Path configPath) {
        this.configPath = configPath;
        this.globalSettings = new GlobalSettings();
        this.moduleStates = new ConcurrentHashMap<>();
        this.moduleSettings = new ConcurrentHashMap<>();
        this.dirty = false;
    }

    /**
     * Loads configuration from disk or creates defaults if not present.
     */
    public void load() {
        if (Files.exists(configPath)) {
            try {
                String content = Files.readString(configPath);
                parseConfig(content);
                LOGGER.info("Configuration loaded from {}", configPath);
            } catch (IOException e) {
                LOGGER.error("Failed to load configuration, using defaults: {}", e.getMessage());
                applyDefaults();
            }
        } else {
            LOGGER.info("No configuration file found, creating defaults");
            applyDefaults();
            save();
        }
    }

    /**
     * Saves current configuration to disk.
     * Uses atomic write to prevent corruption.
     */
    public void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("configVersion", CONFIG_VERSION);

            // Global settings
            JsonObject globalJson = new JsonObject();
            globalJson.addProperty("autoConfigEnabled", globalSettings.autoConfigEnabled);
            globalJson.addProperty("debugMode", globalSettings.debugMode);
            globalJson.addProperty("showMetricsOverlay", globalSettings.showMetricsOverlay);
            globalJson.addProperty("hardwareTier", globalSettings.hardwareTier);
            root.add("global", globalJson);

            // Module states
            JsonObject statesJson = new JsonObject();
            moduleStates.forEach(statesJson::addProperty);
            root.add("moduleStates", statesJson);

            // Module settings
            JsonObject settingsJson = new JsonObject();
            moduleSettings.forEach((moduleId, settings) -> {
                JsonObject moduleJson = new JsonObject();
                settings.forEach((key, value) -> {
                    if (value instanceof Number) {
                        moduleJson.addProperty(key, (Number) value);
                    } else if (value instanceof Boolean) {
                        moduleJson.addProperty(key, (Boolean) value);
                    } else {
                        moduleJson.addProperty(key, String.valueOf(value));
                    }
                });
                settingsJson.add(moduleId, moduleJson);
            });
            root.add("moduleSettings", settingsJson);

            // Atomic write: write to temp file then rename
            Path tempPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");
            Files.writeString(tempPath, GSON.toJson(root));
            Files.move(tempPath, configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);

            dirty = false;
            LOGGER.info("Configuration saved to {}", configPath);

        } catch (IOException e) {
            LOGGER.error("Failed to save configuration: {}", e.getMessage(), e);
        }
    }

    /**
     * Parses configuration from JSON string.
     */
    private void parseConfig(String content) {
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();

        int version = root.has("configVersion") ? root.get("configVersion").getAsInt() : 0;
        if (version < CONFIG_VERSION) {
            LOGGER.info("Migrating configuration from version {} to {}", version, CONFIG_VERSION);
            // Add migration logic here when needed
        }

        // Parse global settings
        if (root.has("global")) {
            JsonObject globalJson = root.getAsJsonObject("global");
            if (globalJson.has("autoConfigEnabled")) {
                globalSettings.autoConfigEnabled = globalJson.get("autoConfigEnabled").getAsBoolean();
            }
            if (globalJson.has("debugMode")) {
                globalSettings.debugMode = globalJson.get("debugMode").getAsBoolean();
            }
            if (globalJson.has("showMetricsOverlay")) {
                globalSettings.showMetricsOverlay = globalJson.get("showMetricsOverlay").getAsBoolean();
            }
            if (globalJson.has("hardwareTier")) {
                globalSettings.hardwareTier = globalJson.get("hardwareTier").getAsString();
            }
        }

        // Parse module states
        if (root.has("moduleStates")) {
            JsonObject statesJson = root.getAsJsonObject("moduleStates");
            statesJson.entrySet().forEach(entry ->
                moduleStates.put(entry.getKey(), entry.getValue().getAsBoolean()));
        }

        // Parse module settings
        if (root.has("moduleSettings")) {
            JsonObject settingsJson = root.getAsJsonObject("moduleSettings");
            settingsJson.entrySet().forEach(moduleEntry -> {
                Map<String, Object> settings = new HashMap<>();
                moduleEntry.getValue().getAsJsonObject().entrySet().forEach(settingEntry -> {
                    var value = settingEntry.getValue();
                    if (value.isJsonPrimitive()) {
                        var primitive = value.getAsJsonPrimitive();
                        if (primitive.isBoolean()) {
                            settings.put(settingEntry.getKey(), primitive.getAsBoolean());
                        } else if (primitive.isNumber()) {
                            settings.put(settingEntry.getKey(), primitive.getAsNumber());
                        } else {
                            settings.put(settingEntry.getKey(), primitive.getAsString());
                        }
                    }
                });
                moduleSettings.put(moduleEntry.getKey(), new ConcurrentHashMap<>(settings));
            });
        }
    }

    /**
     * Applies default configuration values.
     */
    private void applyDefaults() {
        globalSettings.autoConfigEnabled = true;
        globalSettings.debugMode = false;
        globalSettings.showMetricsOverlay = false;
        globalSettings.hardwareTier = "MEDIUM";

        // Default module states: all performance modules enabled
        setModuleEnabledDefault("rendering_optimizer", true);
        setModuleEnabledDefault("entity_culler", true);
        setModuleEnabledDefault("entity_throttler", true);
        setModuleEnabledDefault("lighting_optimizer", true);
        setModuleEnabledDefault("memory_optimizer", true);
        setModuleEnabledDefault("dynamic_fps", true);
        setModuleEnabledDefault("particle_optimizer", true);
        setModuleEnabledDefault("smooth_fps", true);

        dirty = true;
    }

    /**
     * Applies hardware-specific defaults based on detected profile.
     * Called during auto-configuration.
     *
     * @param profile The detected hardware profile
     */
    public void applyHardwareDefaults(HardwareProfile profile) {
        globalSettings.hardwareTier = profile.getTier().name();

        // Adjust settings based on hardware tier
        // These settings provide optimal defaults for each tier
        switch (profile.getTier()) {
            case LOW -> {
                // Aggressive optimization for low-end hardware
                setModuleSetting("rendering_optimizer", "chunkBuildThreads", 1);
                setModuleSetting("rendering_optimizer", "maxChunkUpdatesPerFrame", 2);
                setModuleSetting("rendering_optimizer", "aggressiveCulling", true);

                setModuleSetting("entity_culler", "cullingDistance", 32);
                setModuleSetting("entity_culler", "aggressiveMode", true);

                setModuleSetting("entity_throttler", "tickIntervalIdle", 4);
                setModuleSetting("entity_throttler", "tickIntervalDistant", 3);

                setModuleSetting("lighting_optimizer", "batchSize", 64);
                setModuleSetting("lighting_optimizer", "deferUpdates", true);

                setModuleSetting("memory_optimizer", "aggressivePooling", true);
                setModuleSetting("memory_optimizer", "cacheSize", 64);

                setModuleSetting("dynamic_fps", "unfocusedFps", 5);
                setModuleSetting("dynamic_fps", "menuFps", 30);
            }
            case MEDIUM -> {
                // Balanced optimization
                setModuleSetting("rendering_optimizer", "chunkBuildThreads", 2);
                setModuleSetting("rendering_optimizer", "maxChunkUpdatesPerFrame", 4);
                setModuleSetting("rendering_optimizer", "aggressiveCulling", false);

                setModuleSetting("entity_culler", "cullingDistance", 48);
                setModuleSetting("entity_culler", "aggressiveMode", false);

                setModuleSetting("entity_throttler", "tickIntervalIdle", 3);
                setModuleSetting("entity_throttler", "tickIntervalDistant", 2);

                setModuleSetting("lighting_optimizer", "batchSize", 128);
                setModuleSetting("lighting_optimizer", "deferUpdates", true);

                setModuleSetting("memory_optimizer", "aggressivePooling", false);
                setModuleSetting("memory_optimizer", "cacheSize", 128);

                setModuleSetting("dynamic_fps", "unfocusedFps", 10);
                setModuleSetting("dynamic_fps", "menuFps", 60);
            }
            case HIGH -> {
                // Quality-focused with smoothness optimizations
                setModuleSetting("rendering_optimizer", "chunkBuildThreads", 3);
                setModuleSetting("rendering_optimizer", "maxChunkUpdatesPerFrame", 8);
                setModuleSetting("rendering_optimizer", "aggressiveCulling", false);

                setModuleSetting("entity_culler", "cullingDistance", 64);
                setModuleSetting("entity_culler", "aggressiveMode", false);

                setModuleSetting("entity_throttler", "tickIntervalIdle", 2);
                setModuleSetting("entity_throttler", "tickIntervalDistant", 1);

                setModuleSetting("lighting_optimizer", "batchSize", 256);
                setModuleSetting("lighting_optimizer", "deferUpdates", false);

                setModuleSetting("memory_optimizer", "aggressivePooling", false);
                setModuleSetting("memory_optimizer", "cacheSize", 256);

                setModuleSetting("dynamic_fps", "unfocusedFps", 15);
                setModuleSetting("dynamic_fps", "menuFps", 60);
            }
            case ULTRA -> {
                // Minimal optimization for high-end systems
                setModuleSetting("rendering_optimizer", "chunkBuildThreads", 4);
                setModuleSetting("rendering_optimizer", "maxChunkUpdatesPerFrame", 16);
                setModuleSetting("rendering_optimizer", "aggressiveCulling", false);

                setModuleSetting("entity_culler", "cullingDistance", 96);
                setModuleSetting("entity_culler", "aggressiveMode", false);

                setModuleSetting("entity_throttler", "tickIntervalIdle", 1);
                setModuleSetting("entity_throttler", "tickIntervalDistant", 1);

                setModuleSetting("lighting_optimizer", "batchSize", 512);
                setModuleSetting("lighting_optimizer", "deferUpdates", false);

                setModuleSetting("memory_optimizer", "aggressivePooling", false);
                setModuleSetting("memory_optimizer", "cacheSize", 512);

                setModuleSetting("dynamic_fps", "unfocusedFps", 30);
                setModuleSetting("dynamic_fps", "menuFps", 120);
            }
        }

        dirty = true;
        save();
    }

    // Global Settings Accessors

    public boolean isAutoConfigEnabled() {
        return globalSettings.autoConfigEnabled;
    }

    public void setAutoConfigEnabled(boolean enabled) {
        globalSettings.autoConfigEnabled = enabled;
        dirty = true;
    }

    public boolean isDebugMode() {
        return globalSettings.debugMode;
    }

    public void setDebugMode(boolean enabled) {
        globalSettings.debugMode = enabled;
        dirty = true;
    }

    public boolean isShowMetricsOverlay() {
        return globalSettings.showMetricsOverlay;
    }

    public void setShowMetricsOverlay(boolean show) {
        globalSettings.showMetricsOverlay = show;
        dirty = true;
    }

    // Module State Accessors

    public boolean isModuleEnabled(String moduleId) {
        return moduleStates.getOrDefault(moduleId, true);
    }

    public void setModuleEnabled(String moduleId, boolean enabled) {
        moduleStates.put(moduleId, enabled);
        dirty = true;
    }

    private void setModuleEnabledDefault(String moduleId, boolean enabled) {
        moduleStates.putIfAbsent(moduleId, enabled);
    }

    // Module Setting Accessors

    public void setModuleSetting(String moduleId, String key, Object value) {
        moduleSettings.computeIfAbsent(moduleId, k -> new ConcurrentHashMap<>())
                .put(key, value);
        dirty = true;
    }

    public int getModuleSettingInt(String moduleId, String key, int defaultValue) {
        Map<String, Object> settings = moduleSettings.get(moduleId);
        if (settings != null && settings.containsKey(key)) {
            Object value = settings.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        return defaultValue;
    }

    public boolean getModuleSettingBool(String moduleId, String key, boolean defaultValue) {
        Map<String, Object> settings = moduleSettings.get(moduleId);
        if (settings != null && settings.containsKey(key)) {
            Object value = settings.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        }
        return defaultValue;
    }

    public String getModuleSettingString(String moduleId, String key, String defaultValue) {
        Map<String, Object> settings = moduleSettings.get(moduleId);
        if (settings != null && settings.containsKey(key)) {
            return String.valueOf(settings.get(key));
        }
        return defaultValue;
    }

    public double getModuleSettingDouble(String moduleId, String key, double defaultValue) {
        Map<String, Object> settings = moduleSettings.get(moduleId);
        if (settings != null && settings.containsKey(key)) {
            Object value = settings.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        }
        return defaultValue;
    }

    /**
     * Saves configuration if there are unsaved changes.
     * Call this periodically or when the game is closing.
     */
    public void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    /**
     * Container for global settings.
     */
    private static class GlobalSettings {
        boolean autoConfigEnabled = true;
        boolean debugMode = false;
        boolean showMetricsOverlay = false;
        String hardwareTier = "MEDIUM";
    }
}
