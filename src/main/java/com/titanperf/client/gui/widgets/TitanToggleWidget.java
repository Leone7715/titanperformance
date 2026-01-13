package com.titanperf.client.gui.widgets;

import com.titanperf.client.gui.TitanTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Custom toggle widget with Titan Performance red theme.
 *
 * Features:
 * - Pill-shaped toggle design
 * - Red when ON, gray when OFF
 * - Smooth handle animation
 * - Label on the left
 */
public class TitanToggleWidget extends ClickableWidget {

    private static final int TOGGLE_WIDTH = 36;
    private static final int TOGGLE_HEIGHT = 18;
    private static final int HANDLE_SIZE = 14;
    private static final int HANDLE_MARGIN = 2;

    private boolean value;
    private final Consumer<Boolean> onChange;
    private float animationProgress;

    /**
     * Creates a new Titan-styled toggle.
     *
     * @param x X position
     * @param y Y position
     * @param width Total width including label space
     * @param height Height
     * @param label Label text
     * @param value Initial value
     * @param onChange Callback when value changes
     */
    public TitanToggleWidget(int x, int y, int width, int height,
                             Text label, boolean value, Consumer<Boolean> onChange) {
        super(x, y, width, height, label);
        this.value = value;
        this.onChange = onChange;
        this.animationProgress = value ? 1.0f : 0.0f;
    }

    /**
     * Gets the current value.
     *
     * @return true if ON, false if OFF
     */
    public boolean getValue() {
        return value;
    }

    /**
     * Sets the value programmatically.
     *
     * @param value New value
     */
    public void setValue(boolean value) {
        this.value = value;
        this.animationProgress = value ? 1.0f : 0.0f;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        toggle();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Space or Enter to toggle
        if (keyCode == 32 || keyCode == 257) {
            toggle();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void toggle() {
        if (!this.active) return;

        this.value = !this.value;
        this.animationProgress = value ? 1.0f : 0.0f;

        // Play click sound
        MinecraftClient.getInstance().getSoundManager().play(
            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f)
        );

        if (onChange != null) {
            onChange.accept(this.value);
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Calculate toggle position (right side of widget)
        int toggleX = this.getX() + this.width - TOGGLE_WIDTH;
        int toggleY = this.getY() + (this.height - TOGGLE_HEIGHT) / 2;

        // Draw label
        int labelColor = this.active ? TitanTheme.TEXT_PRIMARY : TitanTheme.TEXT_DISABLED;
        context.drawTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            this.getMessage(),
            this.getX(),
            this.getY() + (this.height - 8) / 2,
            labelColor
        );

        // Animate the toggle
        float targetProgress = value ? 1.0f : 0.0f;
        animationProgress += (targetProgress - animationProgress) * 0.3f;

        // Draw toggle background (pill shape using filled rectangles)
        int bgColor;
        if (!this.active) {
            bgColor = TitanTheme.BUTTON_DISABLED;
        } else if (animationProgress > 0.5f) {
            bgColor = TitanTheme.blend(TitanTheme.TOGGLE_OFF, TitanTheme.TOGGLE_ON, animationProgress);
        } else {
            bgColor = TitanTheme.TOGGLE_OFF;
        }

        // Draw rounded pill background
        drawPill(context, toggleX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT, bgColor);

        // Draw handle
        int handleTravel = TOGGLE_WIDTH - HANDLE_SIZE - HANDLE_MARGIN * 2;
        int handleX = toggleX + HANDLE_MARGIN + (int) (handleTravel * animationProgress);
        int handleY = toggleY + HANDLE_MARGIN;

        int handleColor = TitanTheme.TOGGLE_HANDLE;
        if (!this.active) {
            handleColor = TitanTheme.TEXT_MUTED;
        }

        // Draw handle (rounded rectangle)
        drawPill(context, handleX, handleY, HANDLE_SIZE, HANDLE_SIZE, handleColor);

        // Draw focus border
        if (this.isFocused()) {
            context.drawBorder(toggleX - 1, toggleY - 1, TOGGLE_WIDTH + 2, TOGGLE_HEIGHT + 2,
                TitanTheme.BORDER_FOCUSED);
        }
    }

    /**
     * Draws a pill-shaped (rounded rectangle) element.
     * Since DrawContext doesn't have built-in rounded rect support,
     * we approximate with a regular filled rect.
     */
    private void drawPill(DrawContext context, int x, int y, int width, int height, int color) {
        // Fill the main body
        context.fill(x + 2, y, x + width - 2, y + height, color);
        context.fill(x, y + 2, x + width, y + height - 2, color);

        // Fill corners (rough approximation of rounded corners)
        context.fill(x + 1, y + 1, x + 2, y + 2, color);
        context.fill(x + width - 2, y + 1, x + width - 1, y + 2, color);
        context.fill(x + 1, y + height - 2, x + 2, y + height - 1, color);
        context.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, color);
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
