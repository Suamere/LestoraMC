package com.lestora.event;

import com.lestora.common.models.LestoraPlayer;
import com.lestora.common.models.LestoraVillager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class EventSubscribor {
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