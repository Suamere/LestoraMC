package com.lestora;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandRegistrationHandler {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("highlightRadius")
                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0))
                        .executes(CommandRegistrationHandler::setHighlight)
                )
        );
    }

    private static int setHighlight(CommandContext<CommandSourceStack> context) {
        double radius = DoubleArgumentType.getDouble(context, "radius");
        // Get the command source position
        double x = context.getSource().getPosition().x;
        double y = context.getSource().getPosition().y;
        double z = context.getSource().getPosition().z;

        HighlightConfig.setHighlightCenterAndRadius(x, y, z, radius, context.getSource().getLevel());
        context.getSource().sendSuccess(() -> Component.literal("Highlight radius set to " + radius
                + " at (" + x + ", " + y + ", " + z + ")"), true);
        return 1;
    }
}
