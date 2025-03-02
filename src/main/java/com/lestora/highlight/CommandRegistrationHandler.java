package com.lestora.highlight;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandRegistrationHandler {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        var root = Commands.literal("lestora");

        registerHighlightRadius(root);

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
                            HighlightSphere.setHighlightCenterAndRadius(userId, x, y, z, radius, world);
                            return 1;
                        })
                )
        );
    }
}
