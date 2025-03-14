package com.lestora.common.commands;

import com.lestora.common.DebugOverlay;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonClientCommands {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        var root = Commands.literal("lestora");

        registerShowDebug(root);
        event.getDispatcher().register(root);
    }

    private static void registerShowDebug(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("enableF3")
                .executes(ctx -> {
                    DebugOverlay.setF3Disabled(false);
                    return 1;
                })
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean value = BoolArgumentType.getBool(ctx, "value");
                            DebugOverlay.setF3Disabled(!value);
                            return 1;
                        })
                )
        );
    }
}
