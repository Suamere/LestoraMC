package com.lestora.common;

import com.google.common.base.Function;
import com.lestora.common.models.LestoraPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "lestora", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DebugOverlay {

    private static boolean showDebug = false;
    private static final Map<String, Function<LestoraPlayer, String>> registeredLogs = new LinkedHashMap<>();

    public static void setShowDebug(boolean value) {
        showDebug = value;
    }

    public static void registerDebugLine(String key, Function<LestoraPlayer, String> func) {
        registeredLogs.put(key, func);
    }

    @SubscribeEvent
    public static void onCustomizeGuiOverlay(CustomizeGuiOverlayEvent event) {
        if (!showDebug) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var lestoraPlayer = LestoraPlayer.get(mc.player);
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Function<LestoraPlayer, String>> entry : registeredLogs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().apply(lestoraPlayer);
            lines.add(key + ": " + value);
        }

        drawCenteredLines(event.getGuiGraphics(), mc, lines.toArray(new String[0]));
    }

    private static void drawCenteredLines(GuiGraphics guiGraphics, Minecraft mc, String... lines) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int centerX = screenWidth / 2;
        int colonWidth = mc.font.width(":");
        // The colon is drawn centered, so its left coordinate is:
        int colonDrawX = centerX - colonWidth / 2;
        int y = 10;
        for (String line : lines) {
            // Split the line on the first colon
            String[] parts = line.split(":", 2);
            String leftText = parts[0].trim();
            String rightText = parts.length > 1 ? parts[1].trim() : "";

            // Draw the left text, right-aligned so it ends at the colon's start.
            int leftTextWidth = mc.font.width(leftText);
            int leftDrawX = colonDrawX - leftTextWidth;
            guiGraphics.drawString(mc.font, leftText, leftDrawX, y, 0xFFFFFF, false);

            // Draw the colon centered.
            guiGraphics.drawString(mc.font, " : ", colonDrawX, y, 0xFFFFFF, false);

            // Draw the right text immediately after the colon.
            int rightDrawX = colonDrawX + colonWidth;
            guiGraphics.drawString(mc.font, rightText, rightDrawX, y, 0xFFFFFF, false);

            y += 15;
        }
    }
}