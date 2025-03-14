package com.lestora.common;

import com.google.common.base.Function;
import com.lestora.common.models.LestoraPlayer;
import com.lestora.highlight.HighlightEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = "lestora", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DebugOverlay {

    private static boolean showDebug = false;
    private static boolean f3Disabled = true;
    private static final Map<String, Function<LestoraPlayer, String>> registeredLogs = new LinkedHashMap<>();

    static {
        registerDebugLine("Location", lestoraPlayer -> {
            Player player = lestoraPlayer.getMcPlayer();
            BlockPos playerPos = player.blockPosition();
            return playerPos.getX() + ", " + playerPos.getY() + ", " + playerPos.getZ();
        });
        registerDebugLine("Light Level", lestoraPlayer -> {
            Player player = lestoraPlayer.getMcPlayer();
            BlockPos playerPos = player.blockPosition();
            return String.valueOf(player.level().getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(playerPos));
        });
    }

    public static boolean getF3Disabled() {
        return f3Disabled;
    }

    public static void setF3Disabled(boolean value) {
        f3Disabled = value;
        showDebug = false;
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

        if (f3Disabled)
            drawLeftAlignedLines(event.getGuiGraphics(), mc, lines.toArray(new String[0]));
        else
            drawCenteredLines(event.getGuiGraphics(), mc, lines.toArray(new String[0]));
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getKey() == GLFW.GLFW_KEY_F3 && event.getAction() == GLFW.GLFW_PRESS) {
            showDebug = !showDebug;
        }
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
            String rightText = parts.length > 1 ? "  " + parts[1].trim() : "";

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

    private static void drawLeftAlignedLines(GuiGraphics guiGraphics, Minecraft mc, String... lines) {
        int leftMargin = 15; // left margin for the overlay
        int colonWidth = mc.font.width(":");
        int maxLeftWidth = 0;

        // First pass: determine the maximum width of the left side
        for (String line : lines) {
            String[] parts = line.split(":", 2);
            String leftText = parts[0].trim();
            int width = mc.font.width(leftText);
            if (width > maxLeftWidth) {
                maxLeftWidth = width;
            }
        }

        // The x coordinate where the colon should be centered:
        int colonX = leftMargin + maxLeftWidth;

        int y = 10;
        for (String line : lines) {
            // Split the line on the first colon
            String[] parts = line.split(":", 2);
            String leftText = parts[0].trim();
            String rightText = parts.length > 1 ? " " + parts[1].trim() : "";

            // Draw the left text right-aligned so that its end is at leftMargin + maxLeftWidth
            int leftTextWidth = mc.font.width(leftText);
            int leftDrawX = colonX - leftTextWidth - colonWidth;
            guiGraphics.drawString(mc.font, leftText, leftDrawX, y, 0xFFFFFF, false);

            // Draw the colon centered at colonX
            int colonDrawX = colonX - colonWidth;
            guiGraphics.drawString(mc.font, ":", colonDrawX, y, 0xFFFFFF, false);

            // Draw the right text immediately after the colon
            int rightDrawX = colonX + colonWidth * 2;
            guiGraphics.drawString(mc.font, rightText, rightDrawX, y, 0xFFFFFF, false);

            y += 15;
        }
    }
}