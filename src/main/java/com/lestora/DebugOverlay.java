package com.lestora;

import com.lestora.data.LestoraPlayer;
import com.lestora.event.ConfigBiomeTempEventHandler;
import com.lestora.util.StandingBlockUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

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

//        Optional<Registry<Biome>> maybeBiomeRegistry = mc.level.registryAccess().registries()
//                .filter(entry -> entry.key().equals(Registries.BIOME))
//                .map(entry -> (Registry<Biome>) entry.value())
//                .findFirst();
//
//        var biomeName = "Empty";
//        if (maybeBiomeRegistry.isPresent()) {
//            ResourceLocation biomeResource = maybeBiomeRegistry.get().getKey(lestoraPlayer.getBiome());
//            if (biomeResource != null) {
//                biomeName = biomeResource.toString();
//            }
//        }

        // Construct your debug strings.
        String text1 = "Wetness: " + lestoraPlayer.getWetness();
        String text2 = "Supporting Block: " + StandingBlockUtil.getSupportingBlockType(supportPos) + " " + supportPos.getSupportingPos();
        String text3 = "Body Temp: " + lestoraPlayer.getBodyTemp();
        String text4 = "Swim Level: " + lestoraPlayer.getSwimLevel();

        // Draw them with a starting y of 10 and 15px spacing.
        drawCenteredLines(guiGraphics, mc, 10, 15, text1, text2, text3, text4);
    }

    private static void drawCenteredLines(GuiGraphics guiGraphics, Minecraft mc, int startY, int lineSpacing, String... lines) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int y = startY;
        for (String line : lines) {
            int textWidth = mc.font.width(line);
            int x = (screenWidth - textWidth) / 2;
            guiGraphics.drawString(mc.font, line, x, y, 0xFFFFFF, false);
            y += lineSpacing;
        }
    }
}