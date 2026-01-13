package com.titanperf.mixin.client;

import com.titanperf.TitanPerformanceMod;
import com.titanperf.modules.lighting.LightingOptimizerModule;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

/**
 * Mixin for ClientChunkManager to track chunk loading on the client.
 *
 * When chunks load on the client, their lighting needs to be calculated.
 * By tracking chunk loads, we can prepare our lighting optimization
 * caches and prioritize visible chunks for faster initial display.
 *
 * The ClientChunkManager is responsible for managing all loaded chunks
 * on the client side. It handles chunk receipt from the server and
 * manages the client's view of the world.
 */
@Mixin(ClientChunkManager.class)
public abstract class ClientChunkManagerMixin {

    /**
     * Hook when a chunk is loaded on the client.
     *
     * @param x Chunk X coordinate
     * @param z Chunk Z coordinate
     * @param chunkData The chunk data
     * @param consumer The consumer
     * @param cir Callback info
     */
    @Inject(method = "loadChunkFromPacket", at = @At("RETURN"), require = 0)
    private void titanperf$onChunkLoaded(int x, int z, ChunkData chunkData,
                                          Consumer<ChunkData.BlockEntityVisitor> consumer,
                                          CallbackInfoReturnable<WorldChunk> cir) {
        var controller = TitanPerformanceMod.getController();
        if (controller == null || !controller.isReady()) {
            return;
        }

        LightingOptimizerModule lightingModule = controller.getModule(
                LightingOptimizerModule.MODULE_ID, LightingOptimizerModule.class);

        if (lightingModule != null && lightingModule.isEnabled()) {
            ChunkPos chunkPos = new ChunkPos(x, z);
            for (int section = -4; section < 20; section++) {
                lightingModule.scheduleChunkSectionUpdate(
                        ChunkSectionPos.from(chunkPos, section));
            }
            lightingModule.getMetrics().incrementCounter("chunks_loaded");
        }
    }

    /**
     * Hook when a chunk is unloaded on the client.
     *
     * @param chunkPos The chunk position
     * @param ci Callback info
     */
    @Inject(method = "unload", at = @At("HEAD"), require = 0)
    private void titanperf$onChunkUnloaded(ChunkPos chunkPos, CallbackInfo ci) {
        var controller = TitanPerformanceMod.getController();
        if (controller == null || !controller.isReady()) {
            return;
        }

        LightingOptimizerModule lightingModule = controller.getModule(
                LightingOptimizerModule.MODULE_ID, LightingOptimizerModule.class);

        if (lightingModule != null && lightingModule.isEnabled()) {
            lightingModule.invalidateSkyLightCache(chunkPos);
        }
    }
}
