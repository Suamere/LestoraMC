package com.lestora;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class HighlightConfig {
    private static double centerX;
    private static double centerY;
    private static double centerZ;
    private static double highlightRadius;
    private static final List<BlockPos> highlightedPositions = new CopyOnWriteArrayList<>();

    // Call this when the command is executed
    public static void setHighlightCenterAndRadius(double x, double y, double z, double radius, Level level) {
        centerX = x;
        centerY = y;
        centerZ = z;
        highlightRadius = radius;
        updateHighlightedPositions(level);
    }

    public static double getCenterX() {
        return centerX;
    }

    public static double getCenterY() {
        return centerY;
    }

    public static double getCenterZ() {
        return centerZ;
    }

    public static double getHighlightRadius() {
        return highlightRadius;
    }

    public static List<BlockPos> getHighlightedPositions() {
        return highlightedPositions;
    }

    private static void updateHighlightedPositions(Level level) {
        highlightedPositions.clear();
        BlockPos centerPos = new BlockPos((int) centerX, (int) centerY, (int) centerZ);
        int intRadius = (int) Math.ceil(highlightRadius);
        for (int x = -intRadius; x <= intRadius; x++) {
            for (int y = -intRadius; y <= intRadius; y++) {
                for (int z = -intRadius; z <= intRadius; z++) {
                    BlockPos pos = centerPos.offset(x, y, z);
                    if (pos.distSqr(centerPos) <= highlightRadius * highlightRadius) {
                        BlockState state = level.getBlockState(pos);
                        if (state.canOcclude()) {
                            boolean exposed = false;
                            for (Direction dir : Direction.values()) {
                                BlockPos neighborPos = pos.relative(dir);
                                BlockState neighborState = level.getBlockState(neighborPos);
                                if (!neighborState.canOcclude() || neighborState.getFluidState().is(FluidTags.LAVA)) {
                                    exposed = true;
                                    break;
                                }
                            }
                            if (exposed) {
                                highlightedPositions.add(pos);
                            }
                        }
                    }
                }
            }
        }
    }
}