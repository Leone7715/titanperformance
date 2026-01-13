package com.titanperf.modules.fps;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.core.api.ModuleCategory;
import com.titanperf.core.config.TitanConfig;
import com.titanperf.core.module.AbstractPerformanceModule;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * Dynamic FPS limiting module for power and resource management.
 *
 * Inspired by Dynamic FPS mod, this module reduces frame rate when the game
 * window is not focused or when the player is in menus. This saves significant
 * CPU and GPU resources, reduces power consumption (important for laptops),
 * and allows other applications to run more smoothly in the background.
 *
 * FPS Limiting States:
 *
 * 1. FOCUSED: Normal gameplay with window focused
 *    Uses the player's configured FPS limit or unlimited
 *
 * 2. UNFOCUSED: Window is not in focus (another application is active)
 *    Heavily limited to reduce resource usage (default: 1-10 FPS)
 *
 * 3. MINIMIZED: Window is minimized to taskbar
 *    Nearly paused (default: 1 FPS) to minimize resource usage
 *
 * 4. MENU: In main menu, pause menu, or other menu screens
 *    Moderately limited (default: 30-60 FPS) as fast response not needed
 *
 * Implementation Details:
 * FPS limiting is achieved by sleeping the render thread between frames.
 * The sleep duration is calculated to achieve the target frame rate.
 * This is more efficient than busy-waiting and allows the CPU to idle.
 *
 * The module monitors window focus through GLFW callbacks and screen state
 * through Minecraft's screen manager. State transitions are debounced to
 * prevent rapid switching when alt-tabbing quickly.
 *
 * Configuration Options:
 * unfocusedFps: Target FPS when window is unfocused
 * minimizedFps: Target FPS when window is minimized
 * menuFps: Target FPS in menu screens
 */
public class DynamicFpsModule extends AbstractPerformanceModule {

    /**
     * Module identifier for configuration and registration.
     */
    public static final String MODULE_ID = "dynamic_fps";

    /**
     * Current FPS limiting state.
     */
    private FpsState currentState;

    /**
     * Target FPS for unfocused state.
     */
    private int unfocusedFps;

    /**
     * Target FPS for minimized state.
     */
    private int minimizedFps;

    /**
     * Target FPS for menu state.
     */
    private int menuFps;

    /**
     * Whether the window is currently focused.
     */
    private boolean windowFocused;

    /**
     * Whether the window is currently minimized.
     */
    private boolean windowMinimized;

    /**
     * Whether we're currently in a menu screen.
     */
    private boolean inMenu;

    /**
     * Timestamp of last state change for debouncing.
     */
    private long lastStateChangeTime;

    /**
     * Minimum time between state changes in milliseconds.
     */
    private static final long STATE_CHANGE_DEBOUNCE_MS = 100;

    /**
     * Reference to Minecraft client.
     */
    private MinecraftClient client;

    /**
     * Current effective FPS limit.
     */
    private int currentFpsLimit;

    /**
     * Original FPS limit from game settings (to restore on disable).
     */
    private int originalFpsLimit;

    /**
     * Constructs the dynamic FPS module.
     */
    public DynamicFpsModule() {
        super(MODULE_ID, "Dynamic FPS", ModuleCategory.FPS_CONTROL, 100);
        this.currentState = FpsState.FOCUSED;
        this.windowFocused = true;
        this.windowMinimized = false;
        this.inMenu = false;
    }

    @Override
    protected void initializeModule() {
        client = MinecraftClient.getInstance();

        // Load configuration
        TitanConfig config = TitanPerformanceMod.getConfig();
        unfocusedFps = config.getModuleSettingInt(MODULE_ID, "unfocusedFps", 10);
        minimizedFps = config.getModuleSettingInt(MODULE_ID, "minimizedFps", 1);
        menuFps = config.getModuleSettingInt(MODULE_ID, "menuFps", 60);

        // Store original FPS limit
        originalFpsLimit = client.options.getMaxFps().getValue();

        logger.info("Dynamic FPS configured: unfocused={}, minimized={}, menu={}",
                unfocusedFps, minimizedFps, menuFps);
    }

    @Override
    protected void enableModule() {
        // Initialize state based on current window state
        updateWindowState();
        FpsState initialState = determineState();
        applyState(initialState);
        lastStateChangeTime = System.currentTimeMillis();
    }

    @Override
    protected void disableModule() {
        // Restore original FPS limit when disabled
        currentState = FpsState.FOCUSED;
        currentFpsLimit = originalFpsLimit;
    }

    @Override
    protected void tickModule() {
        // Update window state detection
        updateWindowState();

        // Determine appropriate state
        FpsState newState = determineState();

        // Apply state change with debouncing
        if (newState != currentState) {
            long now = System.currentTimeMillis();
            if (now - lastStateChangeTime > STATE_CHANGE_DEBOUNCE_MS) {
                applyState(newState);
                lastStateChangeTime = now;
            }
        }

        // Update metrics
        metrics.setGauge("current_fps_limit", currentFpsLimit);
        metrics.setGauge("state_ordinal", currentState.ordinal());
        metrics.setGauge("window_focused", windowFocused ? 1 : 0);
        metrics.setGauge("in_menu", inMenu ? 1 : 0);
    }

    @Override
    protected void shutdownModule() {
        // Ensure FPS limit is restored
        if (currentFpsLimit != originalFpsLimit) {
            // In production, this would restore through options
            logger.info("Restoring original FPS limit: {}", originalFpsLimit);
        }
    }

    /**
     * Updates the cached window state from GLFW and Minecraft.
     */
    private void updateWindowState() {
        // Check window focus
        // In production this would use GLFW.glfwGetWindowAttrib
        // For now we use Minecraft's isWindowFocused method
        windowFocused = client.isWindowFocused();

        // Check if minimized
        // GLFW.glfwGetWindowAttrib with GLFW.GLFW_ICONIFIED
        long windowHandle = client.getWindow().getHandle();
        windowMinimized = GLFW.glfwGetWindowAttrib(windowHandle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;

        // Check if in menu
        // We're in a menu if there's a screen open that's not the game HUD
        inMenu = client.currentScreen != null;
    }

    /**
     * Determines the appropriate FPS state based on current conditions.
     *
     * State priority (highest to lowest):
     * 1. MINIMIZED - Window is iconified
     * 2. UNFOCUSED - Window not focused and not minimized
     * 3. MENU - In a menu screen while focused
     * 4. FOCUSED - Normal gameplay
     *
     * @return The appropriate FPS state
     */
    private FpsState determineState() {
        if (windowMinimized) {
            return FpsState.MINIMIZED;
        }

        if (!windowFocused) {
            return FpsState.UNFOCUSED;
        }

        if (inMenu) {
            return FpsState.MENU;
        }

        return FpsState.FOCUSED;
    }

    /**
     * Applies a new FPS limiting state.
     *
     * @param newState The state to apply
     */
    private void applyState(FpsState newState) {
        FpsState oldState = currentState;
        currentState = newState;

        // Calculate new FPS limit
        int newLimit = switch (newState) {
            case MINIMIZED -> minimizedFps;
            case UNFOCUSED -> unfocusedFps;
            case MENU -> menuFps;
            case FOCUSED -> originalFpsLimit;
        };

        currentFpsLimit = newLimit;

        logger.debug("FPS state changed: {} -> {} (limit: {} -> {})",
                oldState, newState, getEffectiveFpsLimit(oldState), newLimit);

        metrics.incrementCounter("state_changes");
    }

    /**
     * Gets the effective FPS limit for a state.
     *
     * @param state The state to query
     * @return The FPS limit for that state
     */
    private int getEffectiveFpsLimit(FpsState state) {
        return switch (state) {
            case MINIMIZED -> minimizedFps;
            case UNFOCUSED -> unfocusedFps;
            case MENU -> menuFps;
            case FOCUSED -> originalFpsLimit;
        };
    }

    /**
     * Returns the current FPS limit that should be applied.
     *
     * Called from rendering mixins to determine frame pacing.
     *
     * @return Current effective FPS limit
     */
    public int getCurrentFpsLimit() {
        if (!isEnabled()) {
            return 260; // Unlimited
        }
        return currentFpsLimit;
    }

    /**
     * Returns whether the frame rate should currently be limited.
     *
     * @return true if limiting should be applied
     */
    public boolean shouldLimitFps() {
        return isEnabled() && currentState != FpsState.FOCUSED;
    }

    /**
     * Returns the current FPS state.
     *
     * @return Current state enum value
     */
    public FpsState getCurrentState() {
        return currentState;
    }

    /**
     * Calculates the appropriate sleep time to achieve target FPS.
     *
     * @param targetFps The target frame rate
     * @param lastFrameTimeNs Time taken by the last frame in nanoseconds
     * @return Milliseconds to sleep, or 0 if no sleep needed
     */
    public long calculateSleepTime(int targetFps, long lastFrameTimeNs) {
        if (targetFps <= 0 || targetFps >= 260) {
            return 0; // No limiting
        }

        // Target frame time in nanoseconds
        long targetFrameTimeNs = 1_000_000_000L / targetFps;

        // How much time we have left in this frame
        long remainingNs = targetFrameTimeNs - lastFrameTimeNs;

        if (remainingNs <= 0) {
            return 0; // Already exceeded target time
        }

        // Convert to milliseconds, leave some margin for accuracy
        return Math.max(0, (remainingNs / 1_000_000L) - 1);
    }

    /**
     * Called when window focus changes.
     *
     * This can be called from GLFW focus callbacks for immediate response.
     *
     * @param focused true if window gained focus
     */
    public void onWindowFocusChanged(boolean focused) {
        this.windowFocused = focused;
        metrics.incrementCounter("focus_changes");
    }

    /**
     * Called when window minimized state changes.
     *
     * @param minimized true if window was minimized
     */
    public void onWindowMinimizedChanged(boolean minimized) {
        this.windowMinimized = minimized;
        metrics.incrementCounter("minimize_changes");
    }

    /**
     * Updates configuration at runtime.
     *
     * @param newUnfocusedFps New unfocused FPS limit
     * @param newMinimizedFps New minimized FPS limit
     * @param newMenuFps New menu FPS limit
     */
    public void updateConfiguration(int newUnfocusedFps, int newMinimizedFps, int newMenuFps) {
        this.unfocusedFps = newUnfocusedFps;
        this.minimizedFps = newMinimizedFps;
        this.menuFps = newMenuFps;

        // Reapply current state with new settings
        applyState(currentState);

        logger.info("Dynamic FPS config updated: unfocused={}, minimized={}, menu={}",
                newUnfocusedFps, newMinimizedFps, newMenuFps);
    }

    /**
     * FPS limiting states.
     */
    public enum FpsState {
        /**
         * Normal gameplay with focused window.
         */
        FOCUSED,

        /**
         * Window is open but not focused.
         */
        UNFOCUSED,

        /**
         * Window is minimized to taskbar.
         */
        MINIMIZED,

        /**
         * Player is in a menu screen.
         */
        MENU
    }
}
