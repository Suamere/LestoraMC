package com.lestora;

import com.lestora.util.StandingBlockUtil;
import com.lestora.util.WetnessUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "lestora", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DebugOverlay {

    @SubscribeEvent
    public static void onCustomizeGuiOverlay(CustomizeGuiOverlayEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();

        String supportType = StandingBlockUtil.getSupportingBlockType(mc.player);
        String text = "Wetness: " + WetnessUtil.getPlayerWetness(mc.player).name();
        Component comp = Component.literal(text);
        Component comp2 = Component.literal("Supporting Block: " + supportType);

        guiGraphics.drawString(mc.font, comp.getString(), 10, 10, 0xFFFFFF, false);
        guiGraphics.drawString(mc.font, comp2.getString(), 10, 25, 0xFFFFFF, false);
    }
}