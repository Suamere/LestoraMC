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
import net.minecraftforge.registries.ForgeRegistries;

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
                .then(Commands.literal("whatAmIHolding")
                        .executes(ctx -> {
                            // Get the client player directly.
                            var player = Minecraft.getInstance().player;
                            if (player == null) {
                                ctx.getSource().sendFailure(Component.literal("This command can only be run by a player."));
                                return 0;
                            }

                            // Get the main and off hand item stacks.
                            var mainStack = player.getMainHandItem();
                            var offStack = player.getOffhandItem();

                            // Get the ResourceLocation (official item name) for each item.
                            var mainRL = ForgeRegistries.ITEMS.getKey(mainStack.getItem());
                            var offRL = ForgeRegistries.ITEMS.getKey(offStack.getItem());

                            // Format the messages. If the hand is empty, say so.
                            String mainMsg = "Main Hand: " + (mainRL != null ? mainRL.toString() : "Empty");
                            String offMsg = "Off Hand: " + (offRL != null ? offRL.toString() : "Empty");

                            // Send the messages privately to the player.
                            ctx.getSource().sendSuccess(() -> Component.literal(mainMsg), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(offMsg), false);
                            return 1;
                        })
                )
//                .then(Commands.literal("reloadConfig")
//                        .requires(source -> source.hasPermission(2))
//                        .executes(ctx -> {
//                            File configFile = new File("config/lestora-common.toml");
//                            CommentedFileConfig configData = CommentedFileConfig.builder(configFile).build();
//                            configData.load();
//                            return 1;
//                        })
//                )
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
