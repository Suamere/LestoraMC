package com.lestora.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.extensions.IForgeFluid;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;

public class WetnessUtil {

    public static String getPlayerWetness(Player player) {
        Level world = Minecraft.getInstance().level;
        if (player == null || world == null) {
            return Wetness.DRY.name() + "1";
        }

        var simpleWetness = simpleWetness(player, world);
        if (simpleWetness != null) return simpleWetness;

        return complexWetness(player);
    }

    private static @NotNull String complexWetness(Player player) {
        // Get the "standing space" (the block the player occupies).
        var standingSpace = StandingBlockUtil.getSupportingBlock(player);
        BlockState stateStanding = standingSpace.getSupportingBlock();
        BlockState headState = standingSpace.getHeadBlock();
        BlockState feetState = standingSpace.getFeetBlock();

        // Check if the supporting block is actual water.
        if (stateStanding.getBlock() == Blocks.WATER) {
            if (headState.getBlock() == Blocks.WATER) {
                if (headState.getFluidState().getAmount() > 3)
                    return Wetness.FULLY_SUBMERGED.name() + "2";
                else
                    return Wetness.NEARLY_SUBMERGED.name() + "3";
            }

            BlockState belowState = standingSpace.getBelowSupport();
            if (belowState.getBlock() == Blocks.WATER) {
                return Wetness.NEARLY_SUBMERGED.name() + "4";
            }

            return Wetness.SOAKED.name() + "5";
        }
        // When standing on a non-air block that isn’t water.
        else if (stateStanding.getBlock() != Blocks.AIR) {
            if (headState.getBlock() == Blocks.WATER) {
                if (headState.getFluidState().getAmount() > 3)
                    return Wetness.FULLY_SUBMERGED.name() + "6";
                else
                    return Wetness.NEARLY_SUBMERGED.name() + "7";
            }

            if (feetState.getBlock() == Blocks.WATER) {
                if (feetState.getFluidState().getAmount() > 3) {
                    return Wetness.SOAKED.name() + "8";
                } else {
                    return Wetness.DAMP.name() + "9";
                }
            }
        }

        // Fallback: if the player is in water or rain.
        if (player.isInWaterOrRain())
            return Wetness.DAMP.name() + "10";

        return Wetness.DRY.name() + "11";
    }

    private static String intermediateWetness(Player player, Level world) {
        AABB playerBB = player.getBoundingBox();
        // Define the horizontal range based on player's bounding box.
        int minX = Mth.floor(playerBB.minX);
        int maxX = Mth.floor(playerBB.maxX);
        int minZ = Mth.floor(playerBB.minZ);
        int maxZ = Mth.floor(playerBB.maxZ);

        BlockPos bestCandidate = null;
        double bestOverlap = 0;

        int footY = Mth.floor(player.getY());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = new BlockPos(x, footY, z);
                FluidState fluidState = world.getFluidState(pos);
                if (!fluidState.isEmpty() && (fluidState.getType() == Fluids.WATER || fluidState.getType() == Fluids.FLOWING_WATER)) {

                    AABB waterAABB = new AABB(x, footY, z, x + 1, footY, z + 1);
                    AABB intersection = playerBB.intersect(waterAABB);
                    if (intersection != null && intersection.getXsize() > 0 && intersection.getZsize() > 0) {
                        double overlap = intersection.getXsize() * intersection.getZsize();
                        if (overlap > bestOverlap) {
                            bestOverlap = overlap;
                            bestCandidate = pos;
                        }
                    }
                }
            }
        }

        if (bestCandidate == null) return null;

        FluidState fluidState = world.getFluidState(bestCandidate);
        if (fluidState.getAmount() > 4) {
            return Wetness.SOAKED.name() + "12";
        } else {
            return Wetness.DAMP.name() + "13";
        }
    }

    private static String simpleWetness(Player player, Level world) {
        FluidType waterType = ((IForgeFluid) Fluids.WATER).getFluidType();
        if (player.isEyeInFluidType(waterType)) {
            return Wetness.FULLY_SUBMERGED.name() + "14";
        }
        else if (player.isInWater()) {
            if (world.getBlockState(player.blockPosition().below()).canOcclude()) {
                FluidState fluidStateAbove = world.getFluidState(player.blockPosition().above());
                if (!fluidStateAbove.isEmpty() && fluidStateAbove.getType() == Fluids.WATER) {
                    if (fluidStateAbove.getAmount() > 4)
                        return Wetness.FULLY_SUBMERGED.name() + "15";
                    return Wetness.NEARLY_SUBMERGED.name() + "16";
                }

                FluidState fluidState = world.getFluidState(player.blockPosition());
                if (!fluidState.isEmpty() && (fluidState.getType() == Fluids.WATER || fluidState.getType() == Fluids.FLOWING_WATER)) {
                    if (fluidState.getAmount() > 4) {
                        return Wetness.SOAKED.name() + "17";
                    } else {
                        return Wetness.DAMP.name() + "18";
                    }
                }

                return intermediateWetness(player, world);
            }
        }
        else if (player.isInWaterOrRain() && world.getBlockState(player.blockPosition().below()).canOcclude()){
            return Wetness.DAMP.name() + "19";
        }

        return null;
    }
}