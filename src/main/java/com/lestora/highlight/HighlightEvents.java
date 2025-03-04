package com.lestora.highlight;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber
public class HighlightEvents {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        handleBlockChange((level, highlightConfig) -> {
            highlightConfig.remove(event.getPos(), level);
        });
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        handleBlockChange((level, highlightConfig) -> {
            // Check if the BlockPos is occlusive, If not, return;
            highlightConfig.add(event.getPos(), level);
        });
    }

    private static void handleBlockChange(BiConsumer<Level, HighlightSphere> action) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        HighlightSphere config = HighlightSphere.getUserHighlightConfig(player.getUUID());
        if (config == null || !config.hasHighlights()) return;

        scheduler.schedule(() -> {
            Minecraft.getInstance().execute(() -> action.accept(level, config));
        }, 200, TimeUnit.MILLISECONDS);
    }
}