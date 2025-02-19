package com.lestora;

import com.lestora.util.StandingBlockUtil;
import com.lestora.util.Wetness;
import com.lestora.util.WetnessUtil;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

@Mod.EventBusSubscriber(value = net.minecraftforge.api.distmarker.Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandRegistrationHandler {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("highlightRadius")
                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0))
                        .executes(CommandRegistrationHandler::setHighlight)
                )
        );

        event.getDispatcher().register(Commands.literal("sb")
                .executes(CommandRegistrationHandler::reportStandingBlock)
        );

        event.getDispatcher().register(Commands.literal("wetness")
                .executes(CommandRegistrationHandler::reportWetness)
        );
    }

    private static int reportWetness(CommandContext<CommandSourceStack> context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            context.getSource().sendFailure(Component.literal("No player found."));
            return 0;
        }
        Wetness wetness = WetnessUtil.getPlayerWetness(mc.player);
        context.getSource().sendSuccess(() -> Component.literal("Player wetness: " + wetness.name()), false);
        return 1;
    }

    private static int reportStandingBlock(CommandContext<CommandSourceStack> context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            context.getSource().sendFailure(Component.literal("No player instance found."));
            return 0;
        }

        BlockPos supportBlock = StandingBlockUtil.getPlayerStandingSpace(mc.player);
        if (supportBlock != null) {
            context.getSource().sendSuccess(() -> Component.literal("Standing Block: " + supportBlock.toShortString()), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("No supporting block found."), false);
        }
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
