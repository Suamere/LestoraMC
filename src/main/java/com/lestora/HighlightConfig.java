package com.lestora;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class HighlightConfig {
    private double centerX;
    private double centerY;
    private double centerZ;
    private double highlightRadius;
    private final List<BlockPos> highlightedPositions = new CopyOnWriteArrayList<>();

    private static final ConcurrentHashMap<UUID, HighlightConfig> userConfigs = new ConcurrentHashMap<>();

    public static void setHighlightCenterAndRadius(UUID userId, double x, double y, double z, double radius, Level level) {
        userConfigs.compute(userId, (id, existingConfig) -> {
            if (existingConfig == null) {
                existingConfig = new HighlightConfig();
            }
            // Update the user's config values
            existingConfig.centerX = x;
            existingConfig.centerY = y;
            existingConfig.centerZ = z;
            existingConfig.highlightRadius = radius;
            // Recalculate highlighted positions for the whole area
            existingConfig.updateHighlightedPositions(level);
            return existingConfig;
        });
    }

    public static HighlightConfig getUserHighlightConfig(UUID userId) {
        return userConfigs.get(userId);
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public double getHighlightRadius() {
        return highlightRadius;
    }

    public List<BlockPos> getHighlightedPositions() {
        return highlightedPositions;
    }

    private void updateHighlightedPositions(Level level) {
        highlightedPositions.clear();
        BlockPos centerPos = new BlockPos((int) centerX, (int) centerY, (int) centerZ);
        int intRadius = (int) Math.ceil(highlightRadius);
        for (int x = -intRadius; x <= intRadius; x++) {
            for (int y = -intRadius; y <= intRadius; y++) {
                for (int z = -intRadius; z <= intRadius; z++) {
                    BlockPos pos = centerPos.offset(x, y, z);
                    if (pos.distSqr(centerPos) <= highlightRadius * highlightRadius) {
                        if (isBlockExposed(pos, level)) {
                            highlightedPositions.add(pos);
                        }
                    }
                }
            }
        }
    }

    private boolean isBlockExposed(BlockPos pos, Level level) {
        BlockState state = level.getBlockState(pos);
        if (!state.canOcclude()) return false;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (!neighborState.canOcclude() || neighborState.getFluidState().is(FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }

    public void remove(BlockPos brokenPos, Level level) {
        highlightedPositions.remove(brokenPos);

        BlockPos centerPos = new BlockPos((int) centerX, (int) centerY, (int) centerZ);

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = brokenPos.relative(dir);
            if (neighborPos.distSqr(centerPos) <= highlightRadius * highlightRadius) {
                if (isBlockExposed(neighborPos, level)) {
                    if (!highlightedPositions.contains(neighborPos)) {
                        highlightedPositions.add(neighborPos);
                    }
                } else {
                    highlightedPositions.remove(neighborPos);
                }
            }
        }
    }

    public void add(BlockPos placedPos, Level level) {
        BlockPos centerPos = new BlockPos((int) centerX, (int) centerY, (int) centerZ);
        // Only proceed if the placed block is within the highlight sphere.
        if (placedPos.distSqr(centerPos) > highlightRadius * highlightRadius) return;

        // Check the newly placed block.
        if (isBlockExposed(placedPos, level)) {
            if (!highlightedPositions.contains(placedPos)) {
                highlightedPositions.add(placedPos);
            }
        } else {
            highlightedPositions.remove(placedPos);
        }

        // Check neighbors: placing a block might cover adjacent blocks, so update them.
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = placedPos.relative(dir);
            if (neighborPos.distSqr(centerPos) <= highlightRadius * highlightRadius) {
                if (isBlockExposed(neighborPos, level)) {
                    if (!highlightedPositions.contains(neighborPos)) {
                        highlightedPositions.add(neighborPos);
                    }
                } else {
                    highlightedPositions.remove(neighborPos);
                }
            }
        }
    }
}