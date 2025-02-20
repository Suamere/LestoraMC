package com.lestora;

import com.lestora.util.TestLightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlayerPositionUpdater {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            BlockPos currentPos = mc.player.blockPosition();
            // Only run the lighting update if the position has changed.
            if (!currentPos.equals(TestLightConfig.getTestPos())) {
                TestLightConfig.setTestPos(currentPos);
                ClientChunkCache chunkSource = mc.level.getChunkSource();
                LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                lightingEngine.checkBlock(currentPos);
                lightingEngine.runLightUpdates();
            }
        }
    }
}
