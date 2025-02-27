package com.lestora.event;

import com.lestora.data.LestoraVillager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import com.lestora.data.LestoraPlayer;
import com.lestora.data.VillagerRepo;

import java.util.UUID;

@EventBusSubscriber
public class AIEvent {

    // Right-click (interact) with entity event
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager)) {
            return;
        }
        event.setCanceled(true);

        var lestoraPlayer = LestoraPlayer.get(event.getEntity());
        var lestoraVillager = LestoraVillager.get(villager);
        lestoraPlayer.TryFocusOnVillager(lestoraVillager);
    }

    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        LestoraPlayer lestoraPlayer = LestoraPlayer.get(player);
        LestoraVillager lestoraVillager = lestoraPlayer.getFocus();
        if (lestoraVillager != null) {
            lestoraVillager.tell(lestoraPlayer, event.getMessage().getString());
        }
    }

    @SubscribeEvent
    public static void onVillagerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        UUID villagerUUID = villager.getUUID();
        System.out.println("Villager died: " + villagerUUID + ". Removing from database.");
        VillagerRepo.deleteVillager(villagerUUID);
    }

    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        LestoraPlayer.get(event.getEntity()).UnfocusCurrentVillager();
    }

    @SubscribeEvent
    public static void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        LestoraPlayer.get(event.getEntity()).UnfocusCurrentVillager();
    }
}