package com.lestora.highlight;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import net.minecraftforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber
public class HighlightEvents {
    public static boolean isPlayerCrouchingKeyDown = false;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        handleBlockChange((level, highlightConfig) -> {
            highlightConfig.remove(event.getPos(), level);
        });
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        handleBlockChange((level, highlightConfig) -> {
            // Check if the BlockPos is occlusive, If not, return;
            highlightConfig.add(event.getPos(), level);
        });
    }

    private static void handleBlockChange(BiConsumer<Level, HighlightSphere> action) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        HighlightSphere config = HighlightSphere.getUserHighlightConfig(player.getUUID());
        if (config == null || !HighlightMemory.hasHighlights()) return;

        scheduler.schedule(() -> {
            Minecraft.getInstance().execute(() -> action.accept(level, config));
        }, 200, TimeUnit.MILLISECONDS);
    }


    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getKey() == Minecraft.getInstance().options.keyShift.getKey().getValue()) {
            var toggle = Minecraft.getInstance().options.toggleCrouch().get();
            var isDown = event.getAction() == GLFW.GLFW_PRESS;
            var isUp = event.getAction() == GLFW.GLFW_RELEASE;
            var isChanged = false;

            if (toggle && isDown) {
                isPlayerCrouchingKeyDown = !isPlayerCrouchingKeyDown;
                isChanged = true;
            }
            else if (!toggle) {
                if (isDown && !isPlayerCrouchingKeyDown) {
                    isPlayerCrouchingKeyDown = true;
                    isChanged = true;
                } else if (isUp && isPlayerCrouchingKeyDown) {
                    isPlayerCrouchingKeyDown = false;
                    isChanged = true;
                }
            }
            if (isChanged && isPlayerCrouchingKeyDown) {
                scheduler.schedule(() -> {
                    Minecraft.getInstance().execute(() -> {
                        if (Minecraft.getInstance().player.isCrouching()) {
                            HighlightEmitter.processTorches(Minecraft.getInstance().player, Minecraft.getInstance().level);
                        }
                    });
                }, 100, TimeUnit.MILLISECONDS);
            }
            else if (isChanged) {
                HighlightEmitter.removeTorches();
            }
        }
    }
}