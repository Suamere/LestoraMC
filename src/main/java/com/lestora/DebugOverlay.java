package com.lestora;

import com.lestora.data.LestoraPlayer;
import com.lestora.util.StandingBlockUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
        var lestoraPlayer = LestoraPlayer.get(mc.player);
        var supportPos = lestoraPlayer.getSupportingBlock();

        var text1 = "Wetness: " + lestoraPlayer.getWetness();
        var text2 = "Supporting Block: " + StandingBlockUtil.getSupportingBlockType(supportPos) + " " + supportPos.getSupportingPos();

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int textWidth1 = mc.font.width(text1);
        int textWidth2 = mc.font.width(text2);
        int x1 = (screenWidth - textWidth1) / 2;
        int x2 = (screenWidth - textWidth2) / 2;

        guiGraphics.drawString(mc.font, text1, x1, 10, 0xFFFFFF, false);
        guiGraphics.drawString(mc.font, text2, x2, 25, 0xFFFFFF, false);
    }
}