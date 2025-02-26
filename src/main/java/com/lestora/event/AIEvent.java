package com.lestora.event;

import com.lestora.data.LestoraVillager;
import com.lestora.data.VillagerConversations;
import net.minecraft.ChatFormatting;
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
            // Do this in LestoraVillager or LestoraPlayer so that it can bet the new name and then set up a callback event to continue the conversation?
            if (villager.getCustomName() == null) {
                VillagerConversations.villagerCount++;
                String newName = "Villager" + VillagerConversations.villagerCount;
                villager.setCustomName(Component.literal(newName).withStyle(ChatFormatting.GOLD));
            }

            UUID villagerUUID = villager.getUUID();
            if (!VillagerConversations.villagerConversations.containsKey(villagerUUID)) {
                String name = villager.getCustomName() != null ? villager.getCustomName().getString() : "Villager";
                LestoraVillager lv = new LestoraVillager(villagerUUID, name);
                VillagerConversations.villagerConversations.put(villagerUUID, lv);
            }
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

    // Tick event to check and display outgoing messages
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Iterate through our stored conversations
        for (LestoraVillager lv : VillagerConversations.villagerConversations.values()) {
            if (lv.outgoingMessage != null) {
                serverPlayer.sendSystemMessage(Component.literal(lv.name + " says, \"" + lv.outgoingMessage + "\""));
                lv.outgoingMessage = null;
            }
        }
    }
}