package com.titanperf.client.gui.widgets;

import com.titanperf.client.gui.TitanTheme;
import com.titanperf.core.api.ModuleCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.function.Supplier;

/**
 * Custom category button for the sidebar with Titan Performance red theme.
 *
 * Features:
 * - Flat design with hover effect
 * - Red left border when selected
 * - Icon support
 * - Compact layout for sidebar
 */
public class TitanCategoryButton extends ButtonWidget {

    private static final int INDICATOR_WIDTH = 3;

    private final ModuleCategory category;
    private final Supplier<Boolean> isSelected;

    /**
     * Creates a new Titan-styled category button.
     *
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     * @param category The module category this button represents
     * @param onPress Action when pressed
     * @param isSelected Supplier that returns true if this category is selected
     */
    public TitanCategoryButton(int x, int y, int width, int height,
                               ModuleCategory category,
                               PressAction onPress,
                               Supplier<Boolean> isSelected) {
        super(x, y, width, height, Text.literal(getCategoryDisplayName(category)), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.category = category;
        this.isSelected = isSelected;
    }

    /**
     * Gets the display name for a category.
     *
     * @param category The category
     * @return Human-readable category name
     */
    private static String getCategoryDisplayName(ModuleCategory category) {
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
     * Gets the icon character for a category.
     *
     * @param category The category
     * @return Icon character or symbol
     */
    private static String getCategoryIcon(ModuleCategory category) {
        return switch (category) {
            case RENDERING -> "\u25A0";  // Square
            case ENTITY -> "\u263A";     // Smiley (entity)
            case LIGHTING -> "\u2600";   // Sun
            case MEMORY -> "\u25CF";     // Circle
            case FPS_CONTROL -> "\u25B6"; // Play
            case SYSTEM -> "\u2699";     // Gear
        };
    }

    /**
     * Gets the associated category.
     *
     * @return The module category
     */
    public ModuleCategory getCategory() {
        return category;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean selected = isSelected.get();
        boolean hovered = this.isHovered();

        // Determine background color
        int bgColor;
        if (selected) {
            bgColor = TitanTheme.BG_HIGHLIGHT;
        } else if (hovered) {
            bgColor = TitanTheme.SIDEBAR_HOVER;
        } else {
            bgColor = 0x00000000; // Transparent
        }

        // Draw background
        if (bgColor != 0) {
            context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
        }

        // Draw selection indicator (left border)
        if (selected) {
            context.fill(
                this.getX(), this.getY(),
                this.getX() + INDICATOR_WIDTH, this.getY() + this.height,
                TitanTheme.SIDEBAR_SELECTED
            );
        }

        // Calculate text position
        int textX = this.getX() + INDICATOR_WIDTH + 8;
        int textY = this.getY() + (this.height - 8) / 2;

        // Draw icon
        String icon = getCategoryIcon(category);
        int iconColor = selected ? TitanTheme.PRIMARY_RED : TitanTheme.TEXT_SECONDARY;
        context.drawTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            icon,
            textX,
            textY,
            iconColor
        );

        // Draw label
        int labelX = textX + 12;
        int textColor = selected ? TitanTheme.TEXT_PRIMARY : (hovered ? TitanTheme.TEXT_PRIMARY : TitanTheme.TEXT_SECONDARY);
        context.drawTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            this.getMessage(),
            labelX,
            textY,
            textColor
        );

        // Draw focus border
        if (this.isFocused()) {
            context.drawBorder(
                this.getX() + 1, this.getY() + 1,
                this.width - 2, this.height - 2,
                TitanTheme.BORDER_FOCUSED
            );
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        // Play click sound
        MinecraftClient.getInstance().getSoundManager().play(
            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f)
        );
        super.onClick(mouseX, mouseY);
    }
}
