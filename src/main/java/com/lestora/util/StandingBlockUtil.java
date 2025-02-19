package com.lestora.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StandingBlockUtil {

    /**
     * Returns the block space that the player occupies.
     * (Typically the block above the supporting block.)
     */
    public static BlockPos getPlayerStandingSpace(Player player) {
        Level world = Minecraft.getInstance().level;
        if (world == null) {
            return null;
        }
        AABB bb = player.getBoundingBox();
        // Use a slight offset below the feet to sample support.
        double sampleY = bb.minY - 0.1;

        int minX = Mth.floor(bb.minX);
        int maxX = Mth.floor(bb.maxX);
        int minZ = Mth.floor(bb.minZ);
        int maxZ = Mth.floor(bb.maxZ);

        BlockPos bestSupport = null;
        double bestArea = 0.0;
        AABB sampleArea = new AABB(bb.minX, sampleY, bb.minZ, bb.maxX, sampleY + 0.01, bb.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = new BlockPos(x, Mth.floor(sampleY), z);
                BlockState state = world.getBlockState(pos);
                VoxelShape shape = state.getCollisionShape(world, pos);
                if (shape.isEmpty()) {
                    continue;
                }
                AABB shapeAABB = shape.bounds().move(pos);
                AABB intersection = shapeAABB.intersect(sampleArea);
                double area = (intersection != null) ? intersection.getXsize() * intersection.getZsize() : 0.0;
                if (area > bestArea) {
                    bestArea = area;
                    bestSupport = pos;
                }
            }
        }
        // The "standing space" is typically one block above the support.
        return (bestSupport != null) ? bestSupport.above() : null;
    }

    /**
     * Returns the block position that is actually supporting the player.
     *
     * If the player's immediate support (the block below their standing space) is air,
     * this method scans downward up to 50 blocks. In that scan:
     *   - Flowing water (non-source water) is ignored (treated as air).
     *   - If a full water block is encountered, it is skipped over—unless the block below it is
     *     also a full water block, in which case the support is labeled as water;
     *     or if the block immediately below a full water block is solid/lava, then that solid block is support.
     * If no support is found within 50 blocks, it returns the original candidate.
     */
    public static BlockPos getSupportingBlock(Player player) {
        Level world = Minecraft.getInstance().level;
        if (world == null || player == null) {
            return null;
        }
        // Start with the block immediately below the player's standing space.
        BlockPos standingSpace = getPlayerStandingSpace(player);
        BlockPos candidate = (standingSpace != null)
                ? standingSpace.below()
                : new BlockPos(Mth.floor(player.getX()), Mth.floor(player.getY()) - 1, Mth.floor(player.getZ()));

        BlockState state = world.getBlockState(candidate);
        // If the candidate is not air, not an ignored fluid, and is not full water, and has a non-empty collision shape, return it.
        if (!state.isAir() && !isIgnoredFluid(state) && !isFullWater(state) &&
                !state.getCollisionShape(world, candidate).isEmpty()) {
            return candidate;
        }
        // Otherwise, scan downward up to 50 blocks.
        BlockPos scanPos = candidate;
        for (int i = 0; i < 50; i++) {
            BlockState scanState = world.getBlockState(scanPos);
            // Handle full water separately.
            if (isFullWater(scanState)) {
                BlockPos next = scanPos.below();
                BlockState nextState = world.getBlockState(next);
                if (isFullWater(nextState)) {
                    // Two consecutive full water blocks: support is water.
                    return scanPos;
                } else if (!nextState.isAir() && !isIgnoredFluid(nextState) &&
                        (!nextState.getCollisionShape(world, next).isEmpty() || isFullWater(nextState))) {
                    // If the block below a full water block is solid (or water), that's our support.
                    return next;
                } else {
                    // Skip over the water block.
                    scanPos = next;
                    continue;
                }
            } else {
                // For non-water blocks:
                // Skip if air or ignored fluid.
                if (scanState.isAir() || isIgnoredFluid(scanState)) {
                    scanPos = scanPos.below();
                    continue;
                }
                // If not full water and the collision shape is empty (e.g. grass), skip it.
                if (scanState.getCollisionShape(world, scanPos).isEmpty()) {
                    scanPos = scanPos.below();
                    continue;
                }
                // Otherwise, we've found a solid (or lava, etc.) block.
                return scanPos;
            }
        }
        // If nothing is found after 50 blocks, return the original candidate.
        return candidate;
    }



    /**
     * Returns a string representing the type of the supporting block.
     * It will return one of: "Water", "Air", "Lava", "Powdered Snow", or the block's name.
     */
    public static String getSupportingBlockType(Player player) {
        Level world = Minecraft.getInstance().level;
        if (player == null || world == null) {
            return "Unknown";
        }
        BlockPos supportPos = getSupportingBlock(player);
        if (supportPos == null) {
            return "None";
        }
        BlockState state = world.getBlockState(supportPos);
        if (state.isAir()) {
            return "Air";
        }
        if (state.getFluidState().is(FluidTags.WATER)) {
            return "Water";
        }
        if (state.getFluidState().is(FluidTags.LAVA)) {
            return "Lava";
        }
        if (state.getBlock() == Blocks.POWDER_SNOW) {
            return "Powdered Snow";
        }
        // Return the block's name.
        return state.getBlock().getName().getString();
    }

    private static boolean isIgnoredFluid(BlockState state) {
        // Flowing water (non-source water) should be ignored.
        return state.getFluidState().is(FluidTags.WATER) && !state.getFluidState().isSource();
    }

    private static boolean isFullWater(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER) && state.getFluidState().isSource();
    }
}
