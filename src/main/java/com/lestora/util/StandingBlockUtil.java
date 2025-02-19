package com.lestora.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StandingBlockUtil {

    public static BlockPos getPlayerStandingSpaceOffset(Player player, int offset) {
        Level world = Minecraft.getInstance().level;
        var defaultBlock = new BlockPos(Mth.floor(player.getX()), Mth.floor(player.getY()) - offset, Mth.floor(player.getZ()));
        if (world == null) return defaultBlock;

        AABB bb = player.getBoundingBox().move(0, -offset, 0);
        double sampleY = bb.minY - 0.1;
        int minX = Mth.floor(bb.minX);
        int maxX = Mth.floor(bb.maxX);
        int minZ = Mth.floor(bb.minZ);
        int maxZ = Mth.floor(bb.maxZ);
        BlockPos bestSupport = null;
        double bestArea = 0.0;
        double sampleThickness = 0.1;
        AABB sampleArea = new AABB(bb.minX, sampleY, bb.minZ, bb.maxX, sampleY + sampleThickness, bb.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = new BlockPos(x, Mth.floor(sampleY), z);
                BlockState state = world.getBlockState(pos);
                VoxelShape shape = state.getCollisionShape(world, pos);
                if (shape.isEmpty()) continue;
                AABB shapeAABB = shape.bounds().move(pos);
                AABB intersection = shapeAABB.intersect(sampleArea);
                double area = (intersection.getXsize() > 0 && intersection.getZsize() > 0)
                        ? intersection.getXsize() * intersection.getZsize() : 0.0;
                if (area > bestArea) {
                    bestArea = area;
                    bestSupport = pos;
                }
            }
        }

        if ((bestSupport == null || bestArea == 0.0)) {
            double footY = player.getBoundingBox().minY;
            double fraction = footY - Math.floor(footY);

            // If the foot's Y is roughly at .5 (with a tolerance of 0.05), assume fence/wall.
            if (Math.abs(fraction - 0.5) < 0.05) {
                BlockState state = world.getBlockState(defaultBlock.below());
                if (state.getBlock() instanceof FenceBlock || state.getBlock() instanceof WallBlock) {
                    return defaultBlock.below();
                }
            }
            // Fall back to default if nothing qualifies.
            return defaultBlock;
        }

        return bestSupport;
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

    public static boolean isFloating(Player player) {
        return player.isInWater() && !player.onGround();
    }

    public static BlockPos getSupportingBlock(Player player) {
        Level world = Minecraft.getInstance().level;
        if (world == null || player == null) return null;

        return player.getOnPos();

//        if (isFloating(player)) {
//            return new BlockPos(Mth.floor(player.getX()), Mth.floor(player.getY()), Mth.floor(player.getZ()));
//        }
//
//        BlockPos support = null;
//        BlockPos firstWater = null;
//
//        return getPlayerStandingSpaceOffset(player, 0);

//        var scanDepth = 2;
//        for (int offset = 0; offset <= scanDepth; offset++) {
//            BlockPos candidate = getPlayerStandingSpaceOffset(player, offset);
//            BlockState state = world.getBlockState(candidate);
//
//            if (state.isAir() || isIgnoredFluid(state)) continue;
//
//            if (isFullWater(state)) {
//                if (firstWater != null) {
//                    support = firstWater;
//                    break;
//                }
//                firstWater = candidate;
//                scanDepth++;
//            } else if (!state.getCollisionShape(world, candidate).isEmpty()) {
//                if (firstWater != null && isFloating(player))
//                    support = firstWater;
//                else
//                    support = candidate;
//                break;
//            } else if (firstWater != null) {
//                support = firstWater;
//                break;
//            }
//        }
//
//        return support != null ? support : new BlockPos(Mth.floor(player.getX()), Mth.floor(player.getY()) - 1, Mth.floor(player.getZ()));
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
