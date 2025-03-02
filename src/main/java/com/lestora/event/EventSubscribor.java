package com.lestora.event;

import com.lestora.highlight.HighlightSphere;
import com.lestora.common.models.LestoraPlayer;
import com.lestora.common.models.LestoraVillager;
import com.lestora.dynamiclighting.DynamicLighting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

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

        if (DynamicLighting.getEnabled()) {
            for (Player player : level.players()) {
                var mainStack = player.getMainHandItem();
                var offStack = player.getOffhandItem();
                ResourceLocation mhi = mainStack != null ? ForgeRegistries.ITEMS.getKey(mainStack.getItem()) : null;
                ResourceLocation ohi = offStack != null ? ForgeRegistries.ITEMS.getKey(offStack.getItem()) : null;
                Integer mhr = (mhi != null) ? ConfigEventHandler.getLightLevel(mhi) : null;
                Integer ohr = (ohi != null) ? ConfigEventHandler.getLightLevel(ohi) : null;
                // Pick main hand if available, else offhand
                ResourceLocation resourceLocation = null;
                if (mhr != null && ohr != null) { resourceLocation = (mhr > ohr) ? mhi : ohi; }
                else if (mhr != null) resourceLocation = mhi;
                else if (ohr != null) resourceLocation = ohi;

                UUID uuid = player.getUUID();
                ResourceLocation previous = previousTorchState.get(uuid);
                boolean changed = (previous == null && resourceLocation != null)
                        || (previous != null && !previous.equals(resourceLocation));

                if (changed) {
                    if (resourceLocation != null) {
                        previousTorchState.put(uuid, resourceLocation);
                        DynamicLighting.tryAddEntity(player, resourceLocation);
                    } else {
                        previousTorchState.remove(uuid);
                        DynamicLighting.tryRemoveEntity(player);
                    }
                }
            }
            DynamicLighting.tryUpdateEntityPositions();
        }

        var clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer != null) {
            LestoraPlayer.get(clientPlayer).calc(clientPlayer);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            Minecraft.getInstance().execute(() -> {
                var resourceLocation = ForgeRegistries.ITEMS.getKey(itemEntity.getItem().getItem());
                var lightLevel = ConfigEventHandler.getLightLevel(resourceLocation);
                if (lightLevel != null) {
                    DynamicLighting.tryAddEntity(itemEntity, resourceLocation);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        DynamicLighting.tryRemoveEntity(event.getEntity());
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
