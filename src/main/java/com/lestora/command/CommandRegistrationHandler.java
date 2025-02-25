package com.lestora.command;

import com.lestora.DebugOverlay;
import com.lestora.HighlightConfig;
import com.lestora.data.LestoraPlayer;
import com.lestora.util.TestLightConfig;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandRegistrationHandler {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        var root = Commands.literal("lestora");

        registerHighlightRadius(root);
        registerDynamicLighting(root);
        registerShowDebug(root);
        registerWhatAmIHolding(root);
        registerSetLevels(root);

        event.getDispatcher().register(root);
    }

    private static void registerHighlightRadius(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("highlightRadius")
                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0))
                        .executes(context -> {
                            double radius = DoubleArgumentType.getDouble(context, "radius");
                            double x = context.getSource().getPosition().x;
                            double y = context.getSource().getPosition().y;
                            double z = context.getSource().getPosition().z;

                            Level world = Minecraft.getInstance().level;
                            if (world == null) {
                                context.getSource().sendFailure(Component.literal("No client world available."));
                                return 0;
                            }

                            UUID userId = Minecraft.getInstance().player.getUUID();
                            HighlightConfig.setHighlightCenterAndRadius(userId, x, y, z, radius, world);
                            return 1;
                        })
                )
        );
    }

    private static void registerSetLevels(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("setLevels")
                .then(Commands.literal("swimLevel")
                        .then(Commands.argument("swimLevel", IntegerArgumentType.integer(0, 3))
                                .executes(context -> {
                                    int swimLevel = IntegerArgumentType.getInteger(context, "swimLevel");
                                    LestoraPlayer.get(Minecraft.getInstance().player).setSwimLevel(swimLevel);
                                    return 1;
                                })
                        )
                )
        );
    }

    private static void registerDynamicLighting(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("dynamicLighting")
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
        );
    }

    private static void registerShowDebug(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("showDebug")
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
        );
    }

    private static void registerWhatAmIHolding(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("whatAmIHolding")
                .executes(ctx -> {
                    var player = Minecraft.getInstance().player;
                    if (player == null) {
                        ctx.getSource().sendFailure(Component.literal("This command can only be run by a player."));
                        return 0;
                    }

                    var mainStack = player.getMainHandItem();
                    var offStack = player.getOffhandItem();

                    var mainRL = ForgeRegistries.ITEMS.getKey(mainStack.getItem());
                    var offRL = ForgeRegistries.ITEMS.getKey(offStack.getItem());

                    String mainMsg = "Main Hand: " + (mainRL != null ? mainRL.toString() : "Empty");
                    String offMsg = "Off Hand: " + (offRL != null ? offRL.toString() : "Empty");

                    ctx.getSource().sendSuccess(() -> Component.literal(mainMsg), false);
                    ctx.getSource().sendSuccess(() -> Component.literal(offMsg), false);
                    return 1;
                })
        );
    }
}
