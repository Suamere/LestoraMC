package com.lestora;

import com.lestora.util.TestLightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class EventSubscribor {
    public static final Logger LOGGER = LogManager.getLogger("lestora");

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

    private static final Map<UUID, Boolean> previousTorchState = new ConcurrentHashMap<>();
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (Player player : level.players()) {
            boolean currentlyHoldingTorch =
                    player.getMainHandItem().getItem() == Items.TORCH ||
                            player.getOffhandItem().getItem() == Items.TORCH;
            UUID uuid = player.getUUID();
            Boolean previous = previousTorchState.get(uuid);
            if (previous == null || previous != currentlyHoldingTorch) {
                previousTorchState.put(uuid, currentlyHoldingTorch);
                if (currentlyHoldingTorch) {
                    TestLightConfig.tryAddEntity(player);
                } else {
                    TestLightConfig.tryRemoveEntity(player);
                }
            }
        }
        TestLightConfig.tryUpdateEntityPositions();
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            // Delay the check by one tick
            Minecraft.getInstance().execute(() -> {
                var item = itemEntity.getItem().getItem();
                LOGGER.info("Delayed check item: {}", item);
                if (item == Items.TORCH) {
                    LOGGER.info("Delayed check found Torch");
                    TestLightConfig.tryAddEntity(itemEntity);
                }
            });
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
