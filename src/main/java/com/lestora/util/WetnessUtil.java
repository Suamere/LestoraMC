package com.lestora.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class WetnessUtil {

    /**
     * Determines the wetness of a player.
     *
     * Prioritized from most to least wet:
     *
     * 1. FULLY_SUBMERGED: The head block (2 above the standing space) is a full water source.
     * 2. NEARLY_SUBMERGED: Either the block above the standing space shows deep water conditions
     *    (player in a water source with water below, or flowing water above with amount > 3)
     *    OR if no proper supporting block is detected and a downward scan shows two or more water blocks,
     *    and the head is not in water.
     * 3. SOAKED: The player's standing space is a full water source, or a downward scan shows one water block.
     * 4. SPLASHED: The player's standing space is water but not a full source.
     * 5. DRY: Otherwise.
     *
     * This version also scans downward when there's no supporting block (e.g. when jumping or falling in deep water).
     *
     * @param player the player entity (client-side)
     * @return the player's wetness state.
     */
    public static Wetness getPlayerWetness(Player player) {
        Level world = Minecraft.getInstance().level;
        if (player == null || world == null) {
            return Wetness.DRY;
        }

        // Get the "standing space" (the block the player occupies).
        BlockPos standingSpace = StandingBlockUtil.getPlayerStandingSpace(player);
        if (standingSpace == null) {
            // Fallback if no support was detected.
            standingSpace = new BlockPos(
                    (int)Math.floor(player.getX()),
                    (int)Math.floor(player.getY()) - 1,
                    (int)Math.floor(player.getZ())
            ).above();
        }

        BlockState stateStanding = world.getBlockState(standingSpace);

        // 1. FULLY_SUBMERGED: Check the head block (2 above standingSpace).
        BlockPos head = standingSpace.above(2);
        BlockState stateHead = world.getBlockState(head);
        if (stateHead.getFluidState().is(FluidTags.WATER) && stateHead.getFluidState().isSource()) {
            return Wetness.FULLY_SUBMERGED;
        }

        // 2. Normal case: If the standing space itself is water...
        if (stateStanding.getFluidState().is(FluidTags.WATER)) {
            // 2a. NEARLY_SUBMERGED: Check the block immediately above.
            BlockPos above = standingSpace.above();
            BlockState stateAbove = world.getBlockState(above);
            if (stateAbove.getFluidState().is(FluidTags.WATER)) {
                // If the player is in a water source and the block below the standing space is water,
                // then it's deep enough.
                if (stateStanding.getFluidState().isSource()) {
                    BlockPos below = standingSpace.below();
                    BlockState stateBelow = world.getBlockState(below);
                    if (stateBelow.getFluidState().is(FluidTags.WATER)) {
                        return Wetness.NEARLY_SUBMERGED;
                    }
                }
                // Or if the water above is flowing with a level > 3, consider it nearly submerged.
                int waterLevelAbove = stateAbove.getFluidState().getAmount();
                if (waterLevelAbove > 3) {
                    return Wetness.NEARLY_SUBMERGED;
                }
            }
            // 2b. SOAKED: If the standing space is a water source.
            if (stateStanding.getFluidState().isSource()) {
                return Wetness.SOAKED;
            }
            // 2c. SPLASHED: Otherwise, if it's water but not a source.
            return Wetness.SPLASHED;
        }

        // 3. Fallback: No proper supporting block (i.e. standing space isn't water).
        // This might happen when the player is falling or jumping through water.
        // We'll scan downward from the player's feet.
        BlockPos scanStart = new BlockPos(
                (int)Math.floor(player.getX()),
                (int)Math.floor(player.getY()),
                (int)Math.floor(player.getZ())
        );
        int waterDepth = scanDownWaterDepth(world, scanStart);
        if (waterDepth == 1) {
            return Wetness.SOAKED;
        } else if (waterDepth >= 2) {
            // If the head isn't in water, then assume partially submerged (nearly submerged).
            BlockPos headPos = scanStart.above(2);
            BlockState headState = world.getBlockState(headPos);
            if (!headState.getFluidState().is(FluidTags.WATER)) {
                return Wetness.NEARLY_SUBMERGED;
            }
            // Otherwise, if the head is in water, treat as fully submerged.
            return Wetness.FULLY_SUBMERGED;
        }

        // 4. Default: DRY.
        return Wetness.DRY;
    }

    // Utility method to scan downward from a given BlockPos and count consecutive water blocks.
    private static int scanDownWaterDepth(Level world, BlockPos startPos) {
        int depth = 0;
        BlockPos pos = startPos;
        while (world.getBlockState(pos).getFluidState().is(FluidTags.WATER)) {
            depth++;
            pos = pos.below();
        }
        return depth;
    }
}
