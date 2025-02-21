package com.lestora;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.lestora.util.TestLightConfig;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
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
                .then(Commands.literal("dynamicLighting")
                        // If no argument is provided, default to true.
                        .executes(ctx -> {
                            DebugOverlay.setShowDebug(true);
                            return 1;
                        })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean value = BoolArgumentType.getBool(ctx, "value");
                                    TestLightConfig.setEnabled(value);
                                    return 1;
                                })
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
                .then(Commands.literal("reloadConfig")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            File configFile = new File("config/lestora-common.toml");
                            CommentedFileConfig configData = CommentedFileConfig.builder(configFile).build();
                            configData.load();
                            return 1;
                        })
                )
        );
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
