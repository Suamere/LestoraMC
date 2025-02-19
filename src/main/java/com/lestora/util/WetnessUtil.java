package com.lestora.util;

import net.minecraft.client.Minecraft;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class WetnessUtil {

    public static Wetness getPlayerWetness(Player player) {
        Level world = Minecraft.getInstance().level;
        if (player == null || world == null) {
            return Wetness.DRY;
        }

        // Get the "standing space" (the block the player occupies).
        var standingSpace = StandingBlockUtil.getSupportingBlock(player);
        BlockState stateStanding = standingSpace.getSupportingBlock();
        BlockState headState = standingSpace.getHeadBlock();
        BlockState feetState = standingSpace.getFeetBlock();

        // Check if the supporting block is actual water.
        if (stateStanding.getBlock() == Blocks.WATER) {
            if (headState.getBlock() == Blocks.WATER) {
                if (headState.getFluidState().getAmount() > 3)
                    return Wetness.FULLY_SUBMERGED;
                else
                    return Wetness.NEARLY_SUBMERGED;
            }

            BlockState belowState = standingSpace.getBelowSupport();
            if (belowState.getBlock() == Blocks.WATER) {
                return Wetness.NEARLY_SUBMERGED;
            }

            return Wetness.SOAKED;
        }
        // When standing on a non-air block that isn’t water.
        else if (stateStanding.getBlock() != Blocks.AIR) {
            if (headState.getBlock() == Blocks.WATER) {
                if (headState.getFluidState().getAmount() > 3)
                    return Wetness.FULLY_SUBMERGED;
                else
                    return Wetness.NEARLY_SUBMERGED;
            }

            if (feetState.getBlock() == Blocks.WATER) {
                if (feetState.getFluidState().getAmount() > 3) {
                    return Wetness.SOAKED;
                } else {
                    return Wetness.SPLASHED;
                }
            }
        }

        // Fallback: if the player is in water or rain.
        if (player.isInWaterOrRain())
            return Wetness.SPLASHED;

        return Wetness.DRY;
    }
}