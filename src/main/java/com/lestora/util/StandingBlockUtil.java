package com.lestora.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StandingBlockUtil {

    /**
     * Returns the block space that the player would occupy if their bounding box were offset downward by {@code offset} blocks.
     */
    public static BlockPos getPlayerStandingSpaceOffset(Player player, int offset) {
        Level world = Minecraft.getInstance().level;
        if (world == null) {
            return null;
        }
        // Offset the player's bounding box downward.
        AABB bb = player.getBoundingBox().move(0, -offset, 0);
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
                if (shape.isEmpty()) continue;
                AABB shapeAABB = shape.bounds().move(pos);
                AABB intersection = shapeAABB.intersect(sampleArea);
                double area = (intersection != null) ? intersection.getXsize() * intersection.getZsize() : 0.0;
                if (area > bestArea) {
                    bestArea = area;
                    bestSupport = pos;
                }
            }
        }
        // Return the block space above the candidate support.
        return (bestSupport != null) ? bestSupport.above() : null;
    }

    // Existing helper methods remain, for example:
    public static boolean isIgnoredFluid(BlockState state) {
        return state.getFluidState().is(net.minecraft.tags.FluidTags.WATER) && !state.getFluidState().isSource();
    }

    public static boolean isFullWater(BlockState state) {
        return state.getFluidState().is(net.minecraft.tags.FluidTags.WATER) && state.getFluidState().isSource();
    }

    // The original getPlayerStandingSpace (using the current bounding box) can remain unchanged:
    public static BlockPos getPlayerStandingSpace(Player player) {
        return getPlayerStandingSpaceOffset(player, 0);
    }

    // Updated getSupportingBlock that uses getPlayerStandingSpaceOffset for each scan:
    public static BlockPos getSupportingBlock(Player player) {
        Level world = Minecraft.getInstance().level;
        if (world == null || player == null) {
            return null;
        }

        // Scan downward from offset 0 to 50 blocks.
        BlockPos support = null;
        for (int offset = 0; offset < 50; offset++) {
            BlockPos standingSpace = getPlayerStandingSpaceOffset(player, offset);
            if (standingSpace == null) continue;
            BlockPos candidate = standingSpace.below();
            BlockState state = world.getBlockState(candidate);
            // We consider a candidate valid if:
            // • It's not air, and not an ignored fluid (flowing water)
            // • AND either it's not full water OR (if full water) the vertical gap is small (i.e. offset <= 1)
            if (!state.isAir() && !isIgnoredFluid(state)) {
                if (!isFullWater(state)) {
                    // Also, ignore non-occlusive blocks (like grass).
                    if (!state.getCollisionShape(world, candidate).isEmpty()) {
                        support = candidate;
                        break;
                    }
                } else {
                    // If it's full water, only accept it if we're very near (offset 0 or 1)
                    if (offset <= 1) {
                        support = candidate;
                        break;
                    }
                }
            }
        }
        // Fallback: if nothing was found, use the block directly below the player's current standing space.
        if (support == null) {
            BlockPos currentStanding = getPlayerStandingSpace(player);
            support = (currentStanding != null) ? currentStanding.below() :
                    new BlockPos(Mth.floor(player.getX()), Mth.floor(player.getY()) - 1, Mth.floor(player.getZ()));
        }
        return support;
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
        if (state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
            return "Water";
        }
        if (state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) {
            return "Lava";
        }
        if (state.getBlock() == net.minecraft.world.level.block.Blocks.POWDER_SNOW) {
            return "Powdered Snow";
        }
        return state.getBlock().getName().getString();
    }
}
