package com.titanperf.client;

import com.titanperf.client.gui.TitanSettingsScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles adding Titan Performance button to vanilla screens using Fabric Screen API.
 */
@Environment(EnvType.CLIENT)
public class TitanScreenHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("TitanPerf/Screen");

    /**
     * Registers screen event handlers.
     */
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            // Add to Video Options screen
            if (screen instanceof VideoOptionsScreen) {
                LOGGER.debug("Adding Titan button to VideoOptionsScreen");
                addTitanButton(screen, scaledWidth, scaledHeight, false);
            }
            // Also add to main Options screen for easier access
            else if (screen instanceof OptionsScreen) {
                LOGGER.debug("Adding Titan button to OptionsScreen");
                addTitanButton(screen, scaledWidth, scaledHeight, true);
            }
        });
    }

    /**
     * Adds the Titan Performance button to a screen.
     */
    private static void addTitanButton(Screen screen, int width, int height, boolean isMainOptions) {
        // Position: top-left corner, clearly visible
        int buttonX = 5;
        int buttonY = 5;
        int buttonWidth = 120;
        int buttonHeight = 20;

        // For main options, place it differently to not overlap
        if (isMainOptions) {
            buttonX = 5;
            buttonY = 5;
        }

        ButtonWidget titanButton = ButtonWidget.builder(
                Text.literal("§bTitan Perf"),
                button -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc != null) {
                        mc.setScreen(new TitanSettingsScreen(screen));
                    }
                })
            .dimensions(buttonX, buttonY, buttonWidth, buttonHeight)
            .build();

        try {
            Screens.getButtons(screen).add(titanButton);
            LOGGER.debug("Titan button added successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to add Titan button: {}", e.getMessage());
        }
    }
}
