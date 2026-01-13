package com.titanperf.client;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.client.gui.TitanSettingsScreen;
import com.titanperf.core.controller.PerformanceController;
import com.titanperf.modules.fps.DynamicFpsModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Keybind registration and handling for Titan Performance.
 *
 * Provides keyboard shortcuts for:
 * - Opening the settings screen
 * - Toggling Dynamic FPS
 * - Toggling all optimizations
 */
@Environment(EnvType.CLIENT)
public class TitanKeybinds {

    /**
     * Keybind category name shown in controls menu.
     */
    private static final String CATEGORY = "titanperf.keybinds.category";

    /**
     * Open settings screen keybind.
     */
    private static KeyBinding openSettingsKey;

    /**
     * Toggle Dynamic FPS keybind.
     */
    private static KeyBinding toggleDynamicFpsKey;

    /**
     * Toggle all modules keybind.
     */
    private static KeyBinding toggleAllKey;

    /**
     * Registers all keybinds with Fabric.
     */
    public static void register() {
        // Open Titan Performance Settings - Default: P
        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "titanperf.keybinds.open_settings",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            CATEGORY
        ));

        // Toggle Dynamic FPS - Default: F8
        toggleDynamicFpsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "titanperf.keybinds.toggle_dynamic_fps",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            CATEGORY
        ));

        // Toggle All Modules - Default: unbound
        toggleAllKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "titanperf.keybinds.toggle_all",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
        ));

        // Register tick handler for keybind processing
        ClientTickEvents.END_CLIENT_TICK.register(TitanKeybinds::onClientTick);
    }

    /**
     * Handles keybind presses each tick.
     */
    private static void onClientTick(MinecraftClient client) {
        // Check open settings
        while (openSettingsKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new TitanSettingsScreen(null));
            }
        }

        // Check toggle dynamic FPS
        while (toggleDynamicFpsKey.wasPressed()) {
            toggleDynamicFps(client);
        }

        // Check toggle all
        while (toggleAllKey.wasPressed()) {
            toggleAllModules(client);
        }
    }

    /**
     * Toggles the Dynamic FPS module.
     */
    private static void toggleDynamicFps(MinecraftClient client) {
        PerformanceController controller = TitanPerformanceMod.getController();
        if (controller == null) return;

        DynamicFpsModule module = controller.getModule(
            DynamicFpsModule.MODULE_ID, DynamicFpsModule.class);

        if (module != null) {
            if (module.isEnabled()) {
                controller.disableModule(DynamicFpsModule.MODULE_ID);
                if (client.player != null) {
                    client.player.sendMessage(
                        Text.translatable("titanperf.message.dynamic_fps_disabled"), true);
                }
            } else {
                controller.enableModule(DynamicFpsModule.MODULE_ID);
                if (client.player != null) {
                    client.player.sendMessage(
                        Text.translatable("titanperf.message.dynamic_fps_enabled"), true);
                }
            }
            TitanPerformanceMod.saveConfig();
        }
    }

    /**
     * Toggles all modules on or off.
     */
    private static void toggleAllModules(MinecraftClient client) {
        PerformanceController controller = TitanPerformanceMod.getController();
        if (controller == null) return;

        // Check if any module is enabled
        boolean anyEnabled = controller.getAllModules().stream()
            .anyMatch(m -> m.isEnabled());

        // Toggle all to opposite state
        for (var module : controller.getAllModules()) {
            if (anyEnabled) {
                controller.disableModule(module.getModuleId());
            } else {
                controller.enableModule(module.getModuleId());
            }
        }

        if (client.player != null) {
            if (anyEnabled) {
                client.player.sendMessage(
                    Text.translatable("titanperf.message.all_disabled"), true);
            } else {
                client.player.sendMessage(
                    Text.translatable("titanperf.message.all_enabled"), true);
            }
        }

        TitanPerformanceMod.saveConfig();
    }
}
