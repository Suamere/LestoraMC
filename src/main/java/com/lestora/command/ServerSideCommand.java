package com.lestora.command;

import com.lestora.data.LestoraPlayer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ServerSideCommand {

    private static int tickCount = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCount++;
        if (tickCount >= 100) { // roughly 5 seconds at 20 ticks per second
            tickCount = 0;
            var server = event.getServer();
            for (LestoraPlayer lp : LestoraPlayer.getPlayers().values()) {
                Player serverPlayer = server.getPlayerList().getPlayer(lp.getUuid());
                if (serverPlayer != null) {
                    lp.CalculateDamage(serverPlayer);
                }
            }
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("lestora")
                .then(Commands.literal("hurtPlayer")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    int damage = IntegerArgumentType.getInteger(context, "amount");
                                    CommandSourceStack source = context.getSource();
                                    try {
                                        Player player = source.getPlayerOrException();
                                        var level = source.getLevel();
                                        Holder<DamageType> dmgTypeHolder = level.registryAccess().registries()
                                                .filter(entry -> entry.key().equals(Registries.DAMAGE_TYPE))
                                                .map(entry -> (Registry<DamageType>) entry.value())
                                                .findFirst()
                                                .orElseThrow(() -> new IllegalStateException("Damage type registry not found"))
                                                .getOrThrow(DamageTypes.GENERIC);

                                        DamageSource drowningSource = new DamageSource(dmgTypeHolder);
                                        player.hurt(drowningSource, damage);
                                    } catch (Exception e) {
                                        source.sendFailure(net.minecraft.network.chat.Component.literal("This command can only be executed by a player."));
                                    }
                                    return 1;
                                }))));
    }
}
