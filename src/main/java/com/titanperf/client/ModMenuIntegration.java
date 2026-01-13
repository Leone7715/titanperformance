package com.titanperf.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.titanperf.client.gui.TitanSettingsScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * ModMenu integration for Titan Performance.
 *
 * Provides a configuration screen factory that ModMenu uses to show
 * the Titan Performance settings button in the mod list.
 *
 * Users can access settings through the Video Settings menu
 * or through the Mods menu by selecting Titan Performance and clicking Configure.
 */
@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TitanSettingsScreen::new;
    }
}
