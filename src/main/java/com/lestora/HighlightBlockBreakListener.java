package com.lestora;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber
public class HighlightBlockBreakListener {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockPos brokenPos = event.getPos();
        UUID userId = Minecraft.getInstance().player.getUUID();
        HighlightConfig config = HighlightConfig.getUserHighlightConfig(userId);
        if (config == null || config.getHighlightRadius() <= 0) return;

        // Get precomputed positions
        List<BlockPos> positions = config.getHighlightedPositions();
        config.remove(brokenPos, Minecraft.getInstance().level);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        // Use the client-side level from Minecraft instance.
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockPos placedPos = event.getPos();
        UUID userId = Minecraft.getInstance().player.getUUID();
        HighlightConfig config = HighlightConfig.getUserHighlightConfig(userId);
        if (config == null || config.getHighlightRadius() <= 0) return;

        config.add(placedPos, level);
    }
}
