package com.lestora.dynamiclighting;

import com.lestora.common.DebugOverlay;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandRegistrationHandler {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        var root = Commands.literal("lestora");

        registerDynamicLighting(root);
        registerWhatAmIHolding(root);

        event.getDispatcher().register(root);
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
                            DynamicLighting.setEnabled(value);
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
