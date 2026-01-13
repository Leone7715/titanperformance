package com.titanperf;

import com.titanperf.client.TitanKeybinds;
import com.titanperf.client.TitanScreenHandler;
import com.titanperf.core.controller.PerformanceController;
import com.titanperf.modules.rendering.RenderingOptimizerModule;
import com.titanperf.modules.entity.EntityCullerModule;
import com.titanperf.modules.entity.EntityThrottlerModule;
import com.titanperf.modules.lighting.LightingOptimizerModule;
import com.titanperf.modules.memory.MemoryOptimizerModule;
import com.titanperf.modules.fps.DynamicFpsModule;
import com.titanperf.modules.particle.ParticleOptimizerModule;
import com.titanperf.modules.smooth.SmoothFpsModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side entry point for Titan Performance.
 *
 * This class handles client-specific initialization including registration
 * of rendering, entity culling, and other client-side optimization modules.
 * Server-side and common initialization is handled by TitanPerformanceMod.
 *
 * Client Modules Registered:
 * RenderingOptimizer: Chunk rendering, frustum culling, draw call optimization
 * EntityCuller: Skip rendering entities outside view or occluded
 * EntityThrottler: Reduce update frequency for distant/inactive entities
 * LightingOptimizer: Batch and defer lighting calculations
 * MemoryOptimizer: Pool objects and reduce allocations
 * DynamicFps: Limit frame rate when unfocused or in menus
 *
 * Event Hooks:
 * ClientTick: Drives module tick processing on the client thread
 * ClientStopping: Triggers cleanup and configuration save
 */
@Environment(EnvType.CLIENT)
public class TitanPerformanceClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("TitanPerf/Client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Titan Performance client initializing");
        long startTime = System.nanoTime();

        // Register keybinds
        TitanKeybinds.register();
        LOGGER.info("Keybinds registered");

        // Register screen handlers for Video Settings button
        TitanScreenHandler.register();
        LOGGER.info("Screen handlers registered");

        PerformanceController controller = TitanPerformanceMod.getController();
        if (controller == null) {
            LOGGER.error("Controller not initialized, client modules cannot be registered");
            return;
        }

        // Register client-side optimization modules
        // Modules are registered in priority order (higher priority = processed first)
        registerClientModules(controller);

        // Register client tick event for module processing
        // We use END_CLIENT_TICK to process after game logic has run
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (controller.isReady()) {
                controller.onTick();
            }
        });

        // Register client shutdown event for cleanup
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            LOGGER.info("Client stopping, performing cleanup");
            controller.shutdown();
            TitanPerformanceMod.saveConfig();
        });

        long elapsed = (System.nanoTime() - startTime) / 1_000_000L;
        LOGGER.info("Titan Performance client initialized in {}ms", elapsed);
    }

    /**
     * Registers all client-side optimization modules with the controller.
     *
     * Module registration order determines initialization order, but tick
     * processing order is determined by priority values. Higher priority
     * modules are ticked first, which is important when modules have
     * dependencies on each other's output.
     *
     * Priority Guidelines:
     * 1000+: System modules (hardware detection, metrics)
     * 500-999: Pre-rendering modules (culling, throttling)
     * 100-499: Rendering modules (chunk building, draw calls)
     * 1-99: Post-rendering modules (FPS control, cleanup)
     *
     * @param controller The performance controller to register with
     */
    private void registerClientModules(PerformanceController controller) {
        // Rendering Optimizer: Core rendering pipeline optimizations
        // Priority 500: Runs before most other modules to establish rendering state
        controller.registerModule(new RenderingOptimizerModule());

        // Entity Culler: Skip rendering non-visible entities
        // Priority 600: Runs early to prevent wasted entity rendering work
        controller.registerModule(new EntityCullerModule());

        // Entity Throttler: Reduce tick frequency for distant entities
        // Priority 550: Works with culler to manage entity processing
        controller.registerModule(new EntityThrottlerModule());

        // Lighting Optimizer: Batch and optimize light updates
        // Priority 400: Runs after entity processing, before final rendering
        controller.registerModule(new LightingOptimizerModule());

        // Memory Optimizer: Reduce allocations and manage object pools
        // Priority 300: Runs throughout to manage memory pressure
        controller.registerModule(new MemoryOptimizerModule());

        // Dynamic FPS: Limit frame rate when appropriate
        // Priority 100: Runs last to apply final frame rate decisions
        controller.registerModule(new DynamicFpsModule());

        // Particle Optimizer: Reduce particle lag
        // Priority 450: Works with rendering for particle culling
        controller.registerModule(new ParticleOptimizerModule());

        // Smooth FPS: Reduce stutters and frame time spikes
        // Priority 150: Works alongside dynamic FPS for smoother gameplay
        controller.registerModule(new SmoothFpsModule());

        LOGGER.info("Registered {} client modules", controller.getAllModules().size());
    }
}
