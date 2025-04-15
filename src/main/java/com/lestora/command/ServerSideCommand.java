package com.lestora.command;

import com.lestora.common.models.LestoraPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ServerSideCommand {

    private static int tickCount = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCount++;
        if (tickCount >= 20) { // roughly 20 ticks per second
            tickCount = 0;
            var server = event.getServer();
            for (LestoraPlayer lp : LestoraPlayer.getPlayers().values()) {
                Player serverPlayer = server.getPlayerList().getPlayer(lp.getUuid());
                if (serverPlayer != null) {
                    lp.CalculateDamage(serverPlayer);
                }
            }
        }
    }
}