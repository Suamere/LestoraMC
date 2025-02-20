package com.lestora;

import com.lestora.util.TestLightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber
public class EventSubscribor {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockPos brokenPos = event.getPos();
        UUID userId = Minecraft.getInstance().player.getUUID();
        HighlightConfig config = HighlightConfig.getUserHighlightConfig(userId);
        if (config == null || config.getHighlightRadius() <= 0) return;

        config.remove(brokenPos, Minecraft.getInstance().level);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockPos placedPos = event.getPos();
        UUID userId = Minecraft.getInstance().player.getUUID();
        HighlightConfig config = HighlightConfig.getUserHighlightConfig(userId);
        if (config == null || config.getHighlightRadius() <= 0) return;

        config.add(placedPos, level);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (Player player : level.players()) {
            TestLightConfig.tryAddEntity(player);
        }
        TestLightConfig.tryUpdateEntityPositions();
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            if (itemEntity.getItem().getItem() == Items.TORCH) {
                TestLightConfig.tryAddEntity(itemEntity);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        TestLightConfig.tryRemoveEntity(event.getEntity());
    }

//    @SubscribeEvent
//    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
//        if (event.phase != TickEvent.Phase.END) return;
//
//        Player player = event.player;
//        if (!player.isInWater()) return;
//        Level level = Minecraft.getInstance().level;
//        if (!level.isClientSide) return;
//        if (player.isCreative() || player.isSpectator()) return;
//
//        var velocity = player.getDeltaMovement();
//        player.setDeltaMovement(velocity.x / 2, velocity.y > 0 ? -velocity.y : velocity.y, velocity.z / 2);
//    }
}
