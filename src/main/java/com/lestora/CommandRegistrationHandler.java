package com.lestora;

import com.lestora.util.TestLightConfig;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.UUID;

@Mod.EventBusSubscriber(value = net.minecraftforge.api.distmarker.Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandRegistrationHandler {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("lestora")
                .then(Commands.literal("highlightRadius")
                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0))
                                .executes(CommandRegistrationHandler::setHighlight)
                        )
                )
                .then(Commands.literal("test")
                        .then(Commands.argument("lightLevel", IntegerArgumentType.integer(0, 14))
                                .executes(CommandRegistrationHandler::testNeighborChanged)
                        )
                )
                .then(Commands.literal("showDebug")
                        // If no argument is provided, default to true.
                        .executes(ctx -> {
                            DebugOverlay.setShowDebug(true);
                            return 1;
                        })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean value = BoolArgumentType.getBool(ctx, "value");
                                    DebugOverlay.setShowDebug(value);
                                    return 1;
                                })
                        )
                )
        );
    }

    private static int testNeighborChanged(CommandContext<CommandSourceStack> context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            context.getSource().sendFailure(Component.literal("No client world loaded."));
            return 0;
        }
        int level = IntegerArgumentType.getInteger(context, "lightLevel");
        TestLightConfig.setTestLightLevel(level);

        BlockPos pos = TestLightConfig.getTestPos();
        // ClientChunkCache is what getChunkSource() returns.
        ClientChunkCache chunkSource = mc.level.getChunkSource();
        // getLightEngine() returns a LevelLightingEngine.
        LevelLightEngine lightingEngine = chunkSource.getLightEngine();

        // This will trigger both the block and skylight engines to recheck the block.
        lightingEngine.checkBlock(pos);
        // Optionally, run any queued light updates if needed:
        lightingEngine.runLightUpdates();

        return 1;
    }

    private static int setHighlight(CommandContext<CommandSourceStack> context) {
        double radius = DoubleArgumentType.getDouble(context, "radius");
        double x = context.getSource().getPosition().x;
        double y = context.getSource().getPosition().y;
        double z = context.getSource().getPosition().z;

        Level world = Minecraft.getInstance().level;
        if(world == null) {
            context.getSource().sendFailure(Component.literal("No client world available."));
            return 0;
        }

        UUID userId = Minecraft.getInstance().player.getUUID();
        HighlightConfig.setHighlightCenterAndRadius(userId, x, y, z, radius, world);
        return 1;
    }
}
