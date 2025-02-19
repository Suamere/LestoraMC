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

    private static boolean showDebug = false;

    public static void setShowDebug(boolean value) {
        showDebug = value;
    }

    @SubscribeEvent
    public static void onCustomizeGuiOverlay(CustomizeGuiOverlayEvent event) {
        if (!showDebug) return;  // Hide debug overlay if disabled.

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        var supportPos = StandingBlockUtil.getSupportingBlock(mc.player);
        String supportType = StandingBlockUtil.getSupportingBlockType(supportPos);

        String text = "Wetness: " + WetnessUtil.getPlayerWetness(mc.player);
        Component comp = Component.literal(text);
        String text2 = "Supporting Block: " + supportType + " " + supportPos.getSupportingPos();
        Component comp2 = Component.literal(text2);

        // Calculate the center for each line.
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int textWidth1 = mc.font.width(comp.getString());
        int textWidth2 = mc.font.width(comp2.getString());
        int x1 = (screenWidth - textWidth1) / 2;
        int x2 = (screenWidth - textWidth2) / 2;

        guiGraphics.drawString(mc.font, comp.getString(), x1, 10, 0xFFFFFF, false);
        guiGraphics.drawString(mc.font, comp2.getString(), x2, 25, 0xFFFFFF, false);
    }
}