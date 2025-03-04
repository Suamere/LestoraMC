package com.lestora.event;

import com.lestora.highlight.HighlightSphere;
import com.lestora.common.models.LestoraPlayer;
import com.lestora.common.models.LestoraVillager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class EventSubscribor {
    //public static final Logger LOGGER = LogManager.getLogger("lestora");

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockPos brokenPos = event.getPos();
        UUID userId = Minecraft.getInstance().player.getUUID();
        HighlightSphere config = HighlightSphere.getUserHighlightConfig(userId);
        if (config == null || !config.hasHighlights()) return;

        config.remove(brokenPos, Minecraft.getInstance().level);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockPos placedPos = event.getPos();
        UUID userId = Minecraft.getInstance().player.getUUID();
        HighlightSphere config = HighlightSphere.getUserHighlightConfig(userId);
        if (config == null || !config.hasHighlights()) return;

        config.add(placedPos, level);
    }

    private static final Map<UUID, ResourceLocation> previousTorchState = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        LestoraVillager.processNewVillagers();
        LestoraVillager.processChatMessages();
        LestoraVillager.giveFreedom();

        var clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer != null) {
            LestoraPlayer.get(clientPlayer).calc(clientPlayer);
        }
    }
}