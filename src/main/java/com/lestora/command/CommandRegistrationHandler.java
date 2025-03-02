package com.lestora.command;

import com.lestora.common.models.LestoraPlayer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandRegistrationHandler {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        var root = Commands.literal("lestora");

        registerSetLevels(root);

        event.getDispatcher().register(root);
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
}
