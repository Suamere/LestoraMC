package com.lestora.event;

import com.lestora.data.LestoraVillager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import com.lestora.data.LestoraPlayer;

import java.util.UUID;

@EventBusSubscriber
public class AIEvent {

    // Right-click (interact) with entity event
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager)) {
            return;
        }
        // Cancel default interaction
        event.setCanceled(true);

        var lestoraPlayer = LestoraPlayer.get(event.getEntity());
        var newFocus = lestoraPlayer.TryFocusOnVillager(villager);

        if (newFocus) {
            // Do this in LestoraVillager or LestoraPlayer so that it can get the new name and then set up a callback event to continue the conversation?
            var lestoraVillager = LestoraVillager.get(villager);
            lestoraVillager.newFocus(lestoraPlayer);
        }
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