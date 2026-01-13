package com.titanperf.modules.particle;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.core.api.ModuleCategory;
import com.titanperf.core.config.TitanConfig;
import com.titanperf.core.module.AbstractPerformanceModule;
import net.minecraft.client.MinecraftClient;

/**
 * Particle optimization module for reducing particle-related lag.
 *
 * Particles can cause significant FPS drops, especially in areas with:
 * - Many torches/fire sources
 * - Rain/snow weather
 * - Explosions or combat
 * - Redstone contraptions
 *
 * This module provides intelligent particle culling based on:
 * - Distance from player
 * - Current FPS (reduce particles when FPS is low)
 * - Particle density in an area
 *
 * Configuration Options:
 * - particleCullDistance: Distance beyond which particles are hidden
 * - particleReductionPercent: Percentage of particles to skip when FPS is low
 * - fpsThreshold: FPS below which particle reduction kicks in
 */
public class ParticleOptimizerModule extends AbstractPerformanceModule {

    public static final String MODULE_ID = "particle_optimizer";

    private MinecraftClient client;
    private int particleCullDistance;
    private int particleReductionPercent;
    private int fpsThreshold;
    private int currentFps;
    private long particleCounter;
    private boolean shouldReduceParticles;

    public ParticleOptimizerModule() {
        super(MODULE_ID, "Particle Optimizer", ModuleCategory.RENDERING, 450);
    }

    @Override
    protected void initializeModule() {
        client = MinecraftClient.getInstance();

        TitanConfig config = TitanPerformanceMod.getConfig();
        particleCullDistance = config.getModuleSettingInt(MODULE_ID, "cullDistance", 32);
        particleReductionPercent = config.getModuleSettingInt(MODULE_ID, "reductionPercent", 50);
        fpsThreshold = config.getModuleSettingInt(MODULE_ID, "fpsThreshold", 40);

        logger.info("Particle optimizer: cull={}blocks, reduction={}%, threshold={}fps",
                particleCullDistance, particleReductionPercent, fpsThreshold);
    }

    @Override
    protected void enableModule() {
        particleCounter = 0;
        shouldReduceParticles = false;
    }

    @Override
    protected void disableModule() {
        shouldReduceParticles = false;
    }

    @Override
    protected void tickModule() {
        if (client == null) return;

        // Update current FPS using Minecraft's built-in FPS counter
        currentFps = client.getCurrentFps();

        // Determine if we should reduce particles
        shouldReduceParticles = currentFps < fpsThreshold;

        metrics.setGauge("current_fps", currentFps);
        metrics.setGauge("reducing_particles", shouldReduceParticles ? 1 : 0);
        metrics.setGauge("particles_skipped", particleCounter);
    }

    @Override
    protected void shutdownModule() {
        shouldReduceParticles = false;
    }

    /**
     * Determines if a particle should be rendered.
     * Called from particle rendering code.
     *
     * @param distanceSq Squared distance from player
     * @return true if particle should be rendered
     */
    public boolean shouldRenderParticle(double distanceSq) {
        if (!isEnabled()) return true;

        // Always cull particles beyond distance
        double cullDistSq = particleCullDistance * particleCullDistance;
        if (distanceSq > cullDistSq) {
            particleCounter++;
            metrics.incrementCounter("particles_culled_distance");
            return false;
        }

        // When FPS is low, randomly skip particles
        if (shouldReduceParticles) {
            particleCounter++;
            // Use simple counter-based reduction
            if ((particleCounter % 100) < particleReductionPercent) {
                metrics.incrementCounter("particles_culled_fps");
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the particle cull distance.
     */
    public int getParticleCullDistance() {
        return particleCullDistance;
    }

    /**
     * Returns whether particle reduction is currently active.
     */
    public boolean isReducingParticles() {
        return shouldReduceParticles;
    }
}
