package com.titanperf.client.gui;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.client.gui.widgets.TitanCategoryButton;
import com.titanperf.client.gui.widgets.TitanToggleWidget;
import com.titanperf.client.gui.widgets.TitanSliderWidget;
import com.titanperf.compat.ModCompatibility;
import com.titanperf.core.api.ModuleCategory;
import com.titanperf.core.api.PerformanceModule;
import com.titanperf.core.config.TitanConfig;
import com.titanperf.core.controller.PerformanceController;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Modern three-panel settings screen for Titan Performance.
 *
 * The screen is divided into three main sections:
 * Left panel contains category navigation buttons.
 * Center panel displays module toggles and sliders for the selected category.
 * Right panel shows descriptions when hovering over options.
 * Header displays title, status, and hardware tier.
 * Footer contains Apply, Reset, and Done buttons.
 */
@Environment(EnvType.CLIENT)
public class TitanSettingsScreen extends Screen {

    private final Screen parent;

    // Layout constants
    private static final int HEADER_HEIGHT = 50;
    private static final int FOOTER_HEIGHT = 35;
    private static final int SIDEBAR_WIDTH = 120;
    private static final int DESCRIPTION_WIDTH = 160;
    private static final int PADDING = 8;
    private static final int OPTION_HEIGHT = 24;
    private static final int OPTION_SPACING = 4;

    // Current state
    private ModuleCategory selectedCategory = ModuleCategory.RENDERING;
    private String hoveredOptionId = null;
    private String hoveredOptionDescription = null;

    public TitanSettingsScreen(Screen parent) {
        super(Text.literal("Titan Performance"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        PerformanceController controller = TitanPerformanceMod.getController();
        TitanConfig config = TitanPerformanceMod.getConfig();

        if (controller == null || config == null) {
            addFooterButtons();
            return;
        }

        // Calculate panel dimensions
        int contentHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT;
        int optionsPanelWidth = this.width - SIDEBAR_WIDTH - DESCRIPTION_WIDTH;

        // === SIDEBAR (Left Panel) ===
        initSidebar(0, HEADER_HEIGHT, SIDEBAR_WIDTH, contentHeight);

        // === OPTIONS PANEL (Center) ===
        initOptionsPanel(SIDEBAR_WIDTH, HEADER_HEIGHT, optionsPanelWidth, contentHeight, controller, config);

        // === FOOTER ===
        addFooterButtons();
    }

    /**
     * Initializes the sidebar with category buttons.
     */
    private void initSidebar(int x, int y, int width, int height) {
        int buttonY = y + PADDING;
        int buttonHeight = 28;

        for (ModuleCategory category : ModuleCategory.values()) {
            TitanCategoryButton categoryBtn = new TitanCategoryButton(
                x + PADDING / 2, buttonY,
                width - PADDING, buttonHeight,
                category,
                btn -> {
                    selectedCategory = category;
                    this.clearAndInit();
                },
                () -> selectedCategory == category
            );
            this.addDrawableChild(categoryBtn);
            buttonY += buttonHeight + 4;
        }

        // Add presets section
        buttonY += 20;

        // Reset to Defaults button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Reset Defaults"),
                btn -> {
                    TitanConfig config = TitanPerformanceMod.getConfig();
                    if (config != null) {
                        PerformanceController controller = TitanPerformanceMod.getController();
                        if (controller != null) {
                            controller.applyAutoConfiguration();
                        }
                    }
                    this.clearAndInit();
                })
            .dimensions(x + PADDING / 2, buttonY, width - PADDING, 20)
            .build()
        );
    }

    /**
     * Initializes the options panel with module toggles for the selected category.
     */
    private void initOptionsPanel(int x, int y, int width, int height,
                                   PerformanceController controller, TitanConfig config) {
        // Get modules for selected category
        List<PerformanceModule> categoryModules = new ArrayList<>();
        for (PerformanceModule module : controller.getAllModules()) {
            if (module.getCategory() == selectedCategory) {
                categoryModules.add(module);
            }
        }
        categoryModules.sort(Comparator.comparing(PerformanceModule::getDisplayName));

        int optionY = y + PADDING + 25; // Space for category title
        int toggleWidth = width - PADDING * 2;

        for (PerformanceModule module : categoryModules) {
            String moduleId = module.getModuleId();
            boolean enabled = module.isEnabled();
            boolean isCompatible = ModCompatibility.isModuleCompatible(moduleId);

            String label = module.getDisplayName();
            if (!isCompatible) {
                label += " (N/A)";
            }

            final String finalLabel = label;
            final String description = getModuleDescription(moduleId);

            TitanToggleWidget toggle = new TitanToggleWidget(
                x + PADDING, optionY,
                toggleWidth, OPTION_HEIGHT,
                Text.literal(finalLabel),
                enabled && isCompatible,
                value -> {
                    if (!isCompatible) return;
                    if (value) {
                        controller.enableModule(moduleId);
                    } else {
                        controller.disableModule(moduleId);
                    }
                }
            ) {
                @Override
                public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                    super.renderWidget(context, mouseX, mouseY, delta);
                    if (this.isHovered()) {
                        hoveredOptionId = moduleId;
                        hoveredOptionDescription = description;
                    }
                }
            };

            if (!isCompatible) {
                toggle.active = false;
            }

            this.addDrawableChild(toggle);
            optionY += OPTION_HEIGHT + OPTION_SPACING;
        }

        // Add category-specific settings
        optionY += 15;
        addCategorySettings(x, optionY, width, config);
    }

    /**
     * Adds category-specific slider settings.
     */
    private void addCategorySettings(int x, int y, int width, TitanConfig config) {
        int sliderWidth = width - PADDING * 2;

        switch (selectedCategory) {
            case RENDERING -> {
                // Chunk Build Threads slider
                addSliderWithDescription(x + PADDING, y, sliderWidth, 20,
                    1, 4,
                    config.getModuleSettingInt("rendering_optimizer", "chunkBuildThreads", 2),
                    " threads",
                    value -> config.setModuleSetting("rendering_optimizer", "chunkBuildThreads", value),
                    "Chunk Build Threads",
                    "Number of background threads for building chunk geometry. More threads = faster chunk loading but higher CPU usage."
                );
                y += 28;

                // Max Updates Per Frame slider
                addSliderWithDescription(x + PADDING, y, sliderWidth, 20,
                    1, 16,
                    config.getModuleSettingInt("rendering_optimizer", "maxChunkUpdatesPerFrame", 4),
                    " updates",
                    value -> config.setModuleSetting("rendering_optimizer", "maxChunkUpdatesPerFrame", value),
                    "Max Chunk Updates",
                    "Maximum chunk updates per frame. Lower values = smoother FPS but slower chunk loading."
                );
            }
            case ENTITY -> {
                // Culling Distance slider
                addSliderWithDescription(x + PADDING, y, sliderWidth, 20,
                    16, 128,
                    config.getModuleSettingInt("entity_culler", "cullingDistance", 48),
                    " blocks",
                    value -> config.setModuleSetting("entity_culler", "cullingDistance", value),
                    "Culling Distance",
                    "Entities beyond this distance are not rendered. Lower = better FPS but entities pop in closer."
                );
            }
            case FPS_CONTROL -> {
                // Unfocused FPS slider
                addSliderWithDescription(x + PADDING, y, sliderWidth, 20,
                    1, 60,
                    config.getModuleSettingInt("dynamic_fps", "unfocusedFps", 10),
                    " FPS",
                    value -> config.setModuleSetting("dynamic_fps", "unfocusedFps", value),
                    "Unfocused FPS",
                    "Frame rate limit when the game window is not focused. Lower = saves power and reduces heat."
                );
                y += 28;

                // Menu FPS slider
                addSliderWithDescription(x + PADDING, y, sliderWidth, 20,
                    15, 120,
                    config.getModuleSettingInt("dynamic_fps", "menuFps", 60),
                    " FPS",
                    value -> config.setModuleSetting("dynamic_fps", "menuFps", value),
                    "Menu FPS",
                    "Frame rate limit in menus and pause screen. No need for high FPS in menus."
                );
            }
            default -> {
                // No additional settings for other categories
            }
        }
    }

    /**
     * Adds a slider with hover description support.
     */
    private void addSliderWithDescription(int x, int y, int width, int height,
                                          int min, int max, int current, String suffix,
                                          java.util.function.Consumer<Integer> onChange,
                                          String name, String description) {
        TitanSliderWidget slider = new TitanSliderWidget(x, y, width, height, min, max, current, suffix, onChange) {
            @Override
            public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                super.renderWidget(context, mouseX, mouseY, delta);
                if (this.isHovered()) {
                    hoveredOptionId = name;
                    hoveredOptionDescription = description;
                }
            }
        };
        this.addDrawableChild(slider);
    }

    /**
     * Adds footer buttons (Apply, Reset, Done).
     */
    private void addFooterButtons() {
        int buttonWidth = 80;
        int buttonSpacing = 10;
        int totalWidth = buttonWidth * 3 + buttonSpacing * 2;
        int startX = (this.width - totalWidth) / 2;
        int buttonY = this.height - FOOTER_HEIGHT + 8;

        // Apply button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Apply"),
                btn -> {
                    TitanPerformanceMod.saveConfig();
                })
            .dimensions(startX, buttonY, buttonWidth, 20)
            .build()
        );

        // Reset button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Reset"),
                btn -> this.clearAndInit())
            .dimensions(startX + buttonWidth + buttonSpacing, buttonY, buttonWidth, 20)
            .build()
        );

        // Done button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                btn -> this.close())
            .dimensions(startX + (buttonWidth + buttonSpacing) * 2, buttonY, buttonWidth, 20)
            .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Reset hover state
        hoveredOptionId = null;
        hoveredOptionDescription = null;

        // Draw backgrounds
        renderBackgrounds(context);

        // Draw header
        renderHeader(context);

        // Draw widgets
        super.render(context, mouseX, mouseY, delta);

        // Draw description panel
        renderDescriptionPanel(context);

        // Draw category title in options panel
        renderCategoryTitle(context);
    }

    /**
     * Renders the panel backgrounds.
     */
    private void renderBackgrounds(DrawContext context) {
        int contentHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT;
        int optionsPanelWidth = this.width - SIDEBAR_WIDTH - DESCRIPTION_WIDTH;

        // Main dark background
        context.fill(0, 0, this.width, this.height, TitanTheme.BG_DARK);

        // Sidebar background (slightly different)
        context.fill(0, HEADER_HEIGHT, SIDEBAR_WIDTH, HEADER_HEIGHT + contentHeight, TitanTheme.SIDEBAR_BG_START);

        // Options panel background
        context.fill(SIDEBAR_WIDTH, HEADER_HEIGHT, SIDEBAR_WIDTH + optionsPanelWidth, HEADER_HEIGHT + contentHeight, TitanTheme.BG_MEDIUM);

        // Description panel background
        context.fill(this.width - DESCRIPTION_WIDTH, HEADER_HEIGHT, this.width, HEADER_HEIGHT + contentHeight, TitanTheme.BG_DARK);

        // Header background
        context.fill(0, 0, this.width, HEADER_HEIGHT, TitanTheme.BG_SEMI_DARK);

        // Footer background
        context.fill(0, this.height - FOOTER_HEIGHT, this.width, this.height, TitanTheme.BG_SEMI_DARK);

        // Separator lines
        context.fill(SIDEBAR_WIDTH - 1, HEADER_HEIGHT, SIDEBAR_WIDTH, this.height - FOOTER_HEIGHT, TitanTheme.BORDER_DEFAULT);
        context.fill(this.width - DESCRIPTION_WIDTH, HEADER_HEIGHT, this.width - DESCRIPTION_WIDTH + 1, this.height - FOOTER_HEIGHT, TitanTheme.BORDER_DEFAULT);
        context.fill(0, HEADER_HEIGHT - 1, this.width, HEADER_HEIGHT, TitanTheme.BORDER_DEFAULT);
        context.fill(0, this.height - FOOTER_HEIGHT, this.width, this.height - FOOTER_HEIGHT + 1, TitanTheme.BORDER_DEFAULT);
    }

    /**
     * Renders the header section.
     */
    private void renderHeader(DrawContext context) {
        int centerX = this.width / 2;
        PerformanceController controller = TitanPerformanceMod.getController();

        // Title with red accent
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("TITAN PERFORMANCE"),
            centerX, 12,
            TitanTheme.PRIMARY_RED
        );

        // Status line
        if (controller != null) {
            long enabled = controller.getAllModules().stream()
                .filter(PerformanceModule::isEnabled).count();
            long total = controller.getAllModules().size();

            String status = enabled + "/" + total + " optimizations active";
            int statusColor = (enabled == total) ? TitanTheme.SUCCESS_GREEN : TitanTheme.WARNING_YELLOW;

            context.drawCenteredTextWithShadow(
                this.textRenderer, status, centerX, 26, statusColor
            );

            // Hardware tier (right side)
            if (controller.getHardwareProfile() != null) {
                String tier = "Hardware: " + controller.getHardwareProfile().getTier().name();
                context.drawTextWithShadow(
                    this.textRenderer, tier,
                    this.width - this.textRenderer.getWidth(tier) - 10, 12,
                    TitanTheme.TEXT_SECONDARY
                );
            }
        }

        // Version (left side)
        context.drawTextWithShadow(
            this.textRenderer, "v1.0.0",
            10, 12, TitanTheme.TEXT_MUTED
        );

        // Iris compatibility note
        if (ModCompatibility.isShaderCompatMode()) {
            context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Iris Mode"),
                centerX, 38,
                TitanTheme.INFO_BLUE
            );
        }
    }

    /**
     * Renders the category title in the options panel.
     */
    private void renderCategoryTitle(DrawContext context) {
        int titleX = SIDEBAR_WIDTH + PADDING;
        int titleY = HEADER_HEIGHT + PADDING;

        String categoryName = getCategoryDisplayName(selectedCategory);
        context.drawTextWithShadow(
            this.textRenderer, categoryName,
            titleX, titleY,
            TitanTheme.PRIMARY_RED
        );

        // Underline
        int textWidth = this.textRenderer.getWidth(categoryName);
        context.fill(titleX, titleY + 10, titleX + textWidth, titleY + 11, TitanTheme.PRIMARY_RED_DARK);
    }

    /**
     * Renders the description panel.
     */
    private void renderDescriptionPanel(DrawContext context) {
        int panelX = this.width - DESCRIPTION_WIDTH + PADDING;
        int panelY = HEADER_HEIGHT + PADDING;
        int panelWidth = DESCRIPTION_WIDTH - PADDING * 2;

        // Panel title
        context.drawTextWithShadow(
            this.textRenderer, "Description",
            panelX, panelY,
            TitanTheme.TEXT_SECONDARY
        );

        panelY += 15;

        // Draw description if hovering over an option
        if (hoveredOptionDescription != null && !hoveredOptionDescription.isEmpty()) {
            // Wrap text to fit panel
            List<String> lines = wrapText(hoveredOptionDescription, panelWidth);
            for (String line : lines) {
                context.drawTextWithShadow(
                    this.textRenderer, line,
                    panelX, panelY,
                    TitanTheme.TEXT_PRIMARY
                );
                panelY += 10;
            }
        } else {
            context.drawTextWithShadow(
                this.textRenderer, "Hover over an",
                panelX, panelY,
                TitanTheme.TEXT_MUTED
            );
            context.drawTextWithShadow(
                this.textRenderer, "option for details",
                panelX, panelY + 10,
                TitanTheme.TEXT_MUTED
            );
        }
    }

    /**
     * Simple text wrapping.
     */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (this.textRenderer.getWidth(testLine) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    /**
     * Gets the display name for a category.
     */
    private String getCategoryDisplayName(ModuleCategory category) {
        return switch (category) {
            case RENDERING -> "Rendering";
            case ENTITY -> "Entities";
            case LIGHTING -> "Lighting";
            case MEMORY -> "Memory";
            case FPS_CONTROL -> "FPS Control";
            case SYSTEM -> "System";
        };
    }

    /**
     * Gets the description for a module.
     */
    private String getModuleDescription(String moduleId) {
        return switch (moduleId) {
            case "rendering_optimizer" -> "Optimizes chunk rendering with intelligent scheduling and frustum culling. Reduces stuttering during chunk loading.";
            case "entity_culler" -> "Skips rendering entities that cannot be seen. Uses frustum culling and distance checks to save GPU time.";
            case "entity_throttler" -> "Reduces tick frequency for distant and idle entities. Saves CPU time without affecting nearby gameplay.";
            case "lighting_optimizer" -> "Batches and defers light calculations to prevent stuttering when many lights change.";
            case "memory_optimizer" -> "Reduces garbage collection pauses through object pooling. Results in smoother frame times.";
            case "dynamic_fps" -> "Limits frame rate when the window is unfocused or in menus. Saves power and reduces heat.";
            case "particle_optimizer" -> "Culls distant particles and reduces particle count when FPS is low. Helps in particle-heavy scenes.";
            case "smooth_fps" -> "Detects and reduces frame time spikes. Performs proactive garbage collection during safe moments.";
            default -> "No description available.";
        };
    }

    @Override
    public void close() {
        TitanPerformanceMod.saveConfig();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
