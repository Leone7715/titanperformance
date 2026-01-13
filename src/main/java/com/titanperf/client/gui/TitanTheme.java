package com.titanperf.client.gui;

/**
 * Color theme constants for Titan Performance GUI.
 *
 * Uses a bold RED color scheme to differentiate from other mods.
 * Colors are designed for readability on dark backgrounds while
 * providing clear visual hierarchy and state feedback.
 *
 * Color Format: 0xAARRGGBB (ARGB with alpha)
 */
public final class TitanTheme {

    private TitanTheme() {
        // Utility class
    }

    // ==================== PRIMARY RED PALETTE ====================

    /**
     * Primary red accent color - used for highlights, buttons, toggles.
     */
    public static final int PRIMARY_RED = 0xFFE53935;

    /**
     * Darker red variant - used for pressed states, borders.
     */
    public static final int PRIMARY_RED_DARK = 0xFFB71C1C;

    /**
     * Lighter red variant - used for hover states, glows.
     */
    public static final int PRIMARY_RED_LIGHT = 0xFFEF5350;

    /**
     * Very light red - used for subtle highlights.
     */
    public static final int PRIMARY_RED_SUBTLE = 0x40E53935;

    // ==================== BACKGROUND COLORS ====================

    /**
     * Darkest background - main panel background.
     */
    public static final int BG_DARK = 0xFF1A1A1A;

    /**
     * Medium dark background - card/section background.
     */
    public static final int BG_MEDIUM = 0xFF2D2D2D;

    /**
     * Lighter background - hover states, elevated elements.
     */
    public static final int BG_LIGHT = 0xFF404040;

    /**
     * Lightest background - active/selected states.
     */
    public static final int BG_HIGHLIGHT = 0xFF505050;

    /**
     * Semi-transparent overlay background.
     */
    public static final int BG_OVERLAY = 0xE0000000;

    /**
     * Semi-transparent dark background.
     */
    public static final int BG_SEMI_DARK = 0xB0000000;

    /**
     * Very transparent background for subtle effects.
     */
    public static final int BG_SUBTLE = 0x40000000;

    // ==================== TEXT COLORS ====================

    /**
     * Primary text color - white for main content.
     */
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;

    /**
     * Secondary text color - gray for descriptions.
     */
    public static final int TEXT_SECONDARY = 0xFFB0B0B0;

    /**
     * Disabled text color - darker gray.
     */
    public static final int TEXT_DISABLED = 0xFF666666;

    /**
     * Muted text color - subtle information.
     */
    public static final int TEXT_MUTED = 0xFF888888;

    /**
     * Title text color - slightly warm white.
     */
    public static final int TEXT_TITLE = 0xFFF5F5F5;

    // ==================== STATUS COLORS ====================

    /**
     * Success/enabled color - green.
     */
    public static final int SUCCESS_GREEN = 0xFF4CAF50;

    /**
     * Warning color - yellow/orange.
     */
    public static final int WARNING_YELLOW = 0xFFFFC107;

    /**
     * Error/danger color - bright red.
     */
    public static final int ERROR_RED = 0xFFF44336;

    /**
     * Info color - blue.
     */
    public static final int INFO_BLUE = 0xFF2196F3;

    // ==================== BORDER COLORS ====================

    /**
     * Default border color - subtle dark.
     */
    public static final int BORDER_DEFAULT = 0xFF3A3A3A;

    /**
     * Focused border color - red accent.
     */
    public static final int BORDER_FOCUSED = 0xFFE53935;

    /**
     * Hover border color - lighter.
     */
    public static final int BORDER_HOVER = 0xFF555555;

    // ==================== BUTTON COLORS ====================

    /**
     * Default button background.
     */
    public static final int BUTTON_DEFAULT = 0xFF3A3A3A;

    /**
     * Hovered button background.
     */
    public static final int BUTTON_HOVER = 0xFF4A4A4A;

    /**
     * Pressed button background.
     */
    public static final int BUTTON_PRESSED = 0xFF2A2A2A;

    /**
     * Disabled button background.
     */
    public static final int BUTTON_DISABLED = 0xFF2D2D2D;

    /**
     * Primary action button (red).
     */
    public static final int BUTTON_PRIMARY = PRIMARY_RED;

    /**
     * Primary button hovered.
     */
    public static final int BUTTON_PRIMARY_HOVER = PRIMARY_RED_LIGHT;

    /**
     * Primary button pressed.
     */
    public static final int BUTTON_PRIMARY_PRESSED = PRIMARY_RED_DARK;

    // ==================== SLIDER COLORS ====================

    /**
     * Slider track background.
     */
    public static final int SLIDER_TRACK = 0xFF3A3A3A;

    /**
     * Slider filled track.
     */
    public static final int SLIDER_FILL = PRIMARY_RED;

    /**
     * Slider thumb/handle.
     */
    public static final int SLIDER_THUMB = 0xFFFFFFFF;

    /**
     * Slider thumb border.
     */
    public static final int SLIDER_THUMB_BORDER = PRIMARY_RED_DARK;

    // ==================== TOGGLE COLORS ====================

    /**
     * Toggle off background.
     */
    public static final int TOGGLE_OFF = 0xFF3A3A3A;

    /**
     * Toggle on background.
     */
    public static final int TOGGLE_ON = PRIMARY_RED;

    /**
     * Toggle handle color.
     */
    public static final int TOGGLE_HANDLE = 0xFFFFFFFF;

    // ==================== SIDEBAR COLORS ====================

    /**
     * Sidebar background gradient start.
     */
    public static final int SIDEBAR_BG_START = 0xFF1F1F1F;

    /**
     * Sidebar background gradient end.
     */
    public static final int SIDEBAR_BG_END = 0xFF151515;

    /**
     * Selected category indicator.
     */
    public static final int SIDEBAR_SELECTED = PRIMARY_RED;

    /**
     * Category hover background.
     */
    public static final int SIDEBAR_HOVER = 0xFF2A2A2A;

    // ==================== SCROLLBAR COLORS ====================

    /**
     * Scrollbar track background.
     */
    public static final int SCROLLBAR_TRACK = 0x40000000;

    /**
     * Scrollbar thumb default.
     */
    public static final int SCROLLBAR_THUMB = 0x80FFFFFF;

    /**
     * Scrollbar thumb hovered.
     */
    public static final int SCROLLBAR_THUMB_HOVER = 0xA0FFFFFF;

    // ==================== HELPER METHODS ====================

    /**
     * Applies alpha to a color.
     *
     * @param color Original color (0xAARRGGBB)
     * @param alpha New alpha value (0-255)
     * @return Color with new alpha
     */
    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    /**
     * Blends two colors.
     *
     * @param color1 First color
     * @param color2 Second color
     * @param factor Blend factor (0.0 = color1, 1.0 = color2)
     * @return Blended color
     */
    public static int blend(int color1, int color2, float factor) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * factor);
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Lightens a color by a factor.
     *
     * @param color Original color
     * @param factor Lighten factor (0.0 = unchanged, 1.0 = white)
     * @return Lightened color
     */
    public static int lighten(int color, float factor) {
        return blend(color, 0xFFFFFFFF, factor);
    }

    /**
     * Darkens a color by a factor.
     *
     * @param color Original color
     * @param factor Darken factor (0.0 = unchanged, 1.0 = black)
     * @return Darkened color
     */
    public static int darken(int color, float factor) {
        return blend(color, 0xFF000000, factor);
    }
}
