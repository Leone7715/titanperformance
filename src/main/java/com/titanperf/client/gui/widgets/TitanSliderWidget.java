package com.titanperf.client.gui.widgets;

import com.titanperf.client.gui.TitanTheme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;

/**
 * Custom slider widget with Titan Performance red theme.
 *
 * Features:
 * - Red-themed track fill
 * - Clean modern design
 * - Value display on the left
 * - Smooth thumb with border
 */
public class TitanSliderWidget extends SliderWidget {

    private final int minValue;
    private final int maxValue;
    private final String suffix;
    private final Consumer<Integer> onChange;

    /**
     * Creates a new Titan-styled slider.
     *
     * @param x X position
     * @param y Y position
     * @param width Width of the slider
     * @param height Height of the slider
     * @param minValue Minimum value
     * @param maxValue Maximum value
     * @param currentValue Current value
     * @param suffix Suffix to display after value (e.g., "%", "px")
     * @param onChange Callback when value changes
     */
    public TitanSliderWidget(int x, int y, int width, int height,
                             int minValue, int maxValue, int currentValue,
                             String suffix, Consumer<Integer> onChange) {
        super(x, y, width, height, Text.literal(""), calculateValue(currentValue, minValue, maxValue));
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.suffix = suffix;
        this.onChange = onChange;
        updateMessage();
    }

    private static double calculateValue(int current, int min, int max) {
        return (double) (current - min) / (max - min);
    }

    @Override
    protected void updateMessage() {
        int currentValue = (int) Math.round(this.value * (maxValue - minValue) + minValue);
        setMessage(Text.literal(currentValue + suffix));
    }

    @Override
    protected void applyValue() {
        int currentValue = (int) Math.round(this.value * (maxValue - minValue) + minValue);
        if (onChange != null) {
            onChange.accept(currentValue);
        }
    }

    /**
     * Gets the current integer value.
     *
     * @return Current value
     */
    public int getIntValue() {
        return (int) Math.round(this.value * (maxValue - minValue) + minValue);
    }

    /**
     * Sets the value programmatically.
     *
     * @param value New value
     */
    public void setValue(int value) {
        this.value = calculateValue(value, minValue, maxValue);
        updateMessage();
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Calculate dimensions
        int trackHeight = 4;
        int trackY = this.getY() + (this.height - trackHeight) / 2;
        int valueWidth = 40; // Space for value text
        int trackX = this.getX() + valueWidth + 4;
        int trackWidth = this.width - valueWidth - 8;

        // Draw value text on the left
        String valueText = getIntValue() + suffix;
        int textColor = this.active ? TitanTheme.TEXT_PRIMARY : TitanTheme.TEXT_DISABLED;
        context.drawTextWithShadow(
            net.minecraft.client.MinecraftClient.getInstance().textRenderer,
            valueText,
            this.getX() + valueWidth - net.minecraft.client.MinecraftClient.getInstance().textRenderer.getWidth(valueText),
            this.getY() + (this.height - 8) / 2,
            textColor
        );

        // Draw track background
        int trackColor = this.active ? TitanTheme.SLIDER_TRACK : TitanTheme.BG_DARK;
        context.fill(trackX, trackY, trackX + trackWidth, trackY + trackHeight, trackColor);

        // Draw filled portion
        int fillWidth = (int) (trackWidth * this.value);
        int fillColor = this.active ? TitanTheme.SLIDER_FILL : TitanTheme.TEXT_DISABLED;
        if (fillWidth > 0) {
            context.fill(trackX, trackY, trackX + fillWidth, trackY + trackHeight, fillColor);
        }

        // Draw thumb
        int thumbWidth = 8;
        int thumbHeight = 14;
        int thumbX = trackX + fillWidth - thumbWidth / 2;
        int thumbY = this.getY() + (this.height - thumbHeight) / 2;

        // Thumb background
        int thumbBg = this.isHovered() || this.isFocused() ? TitanTheme.SLIDER_THUMB : 0xFFE0E0E0;
        context.fill(thumbX, thumbY, thumbX + thumbWidth, thumbY + thumbHeight, thumbBg);

        // Thumb border
        int borderColor = this.active ? TitanTheme.SLIDER_THUMB_BORDER : TitanTheme.TEXT_DISABLED;
        context.drawBorder(thumbX, thumbY, thumbWidth, thumbHeight, borderColor);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        int valueWidth = 40;
        int trackX = this.getX() + valueWidth + 4;
        int trackWidth = this.width - valueWidth - 8;

        // Only respond to clicks on the track area
        if (mouseX >= trackX && mouseX <= trackX + trackWidth) {
            this.value = MathHelper.clamp((mouseX - trackX) / trackWidth, 0.0, 1.0);
            updateMessage();
            applyValue();
        }
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        int valueWidth = 40;
        int trackX = this.getX() + valueWidth + 4;
        int trackWidth = this.width - valueWidth - 8;

        this.value = MathHelper.clamp((mouseX - trackX) / trackWidth, 0.0, 1.0);
        updateMessage();
        applyValue();
    }
}
