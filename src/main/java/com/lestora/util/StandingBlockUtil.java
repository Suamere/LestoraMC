package com.lestora.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StandingBlockUtil {

    public static EntityBlockInfo getSupportingBlock(Player player) {
        Level world = Minecraft.getInstance().level;
        if (world == null || player == null) return null;

        BlockPos supportingPos = player.getOnPos();
        BlockState state = world.getBlockState(supportingPos);

        if (state.getBlock() == Blocks.AIR) {
            // Move the player's bounding box downward by one block.
            AABB bb = player.getBoundingBox().move(0, -1.0, 0);
            // We'll search for support in the blocks intersecting the horizontal span of bb
            int minX = Mth.floor(bb.minX);
            int maxX = Mth.floor(bb.maxX);
            int posY = Mth.floor(bb.minY); // likely the candidate block's y
            int minZ = Mth.floor(bb.minZ);
            int maxZ = Mth.floor(bb.maxZ);

            BlockPos bestCandidate = null;
            double bestOverlap = 0.0;

            // Check all blocks in that horizontal area at the candidate y-level.
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, posY, z);
                    BlockState bs = world.getBlockState(pos);
                    VoxelShape shape = bs.getCollisionShape(world, pos);
                    if (shape.isEmpty())
                        continue;

                    // Convert the shape to an AABB in world coordinates.
                    AABB shapeBB = shape.bounds().move(pos);
                    // Calculate the horizontal intersection with our downward-offset bounding box.
                    AABB intersection = shapeBB.intersect(bb);
                    double overlap = 0.0;
                    if (intersection != null && intersection.getXsize() > 0 && intersection.getZsize() > 0) {
                        overlap = intersection.getXsize() * intersection.getZsize();
                    }
                    if (overlap > bestOverlap) {
                        bestOverlap = overlap;
                        bestCandidate = pos;
                    }
                }
            }

            if (bestCandidate != null && bestOverlap > 0.0) {
                supportingPos = bestCandidate;
            } else {
                // If no block collision is found, check if water exists below the original support.
                BlockPos waterPos = supportingPos.below();
                BlockState waterState = world.getBlockState(waterPos);
                if (waterState.getBlock() == Blocks.WATER) {
                    supportingPos = waterPos;
                }
            }
        }

        if (state.getBlock() == Blocks.WATER) {
            if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
                state = state.setValue(BlockStateProperties.WATERLOGGED, false);
            }
        }

        return EntityBlockInfo.fromSupport(state, supportingPos);
    }

    public static String getSupportingBlockType(EntityBlockInfo supportPos) {
        if (supportPos == null) {
            return "None";
        }
        BlockState state = supportPos.getSupportingBlock();

        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
            return state.getBlock().getName().getString();
        }

        return state.getBlock().getName().getString();
    }
}