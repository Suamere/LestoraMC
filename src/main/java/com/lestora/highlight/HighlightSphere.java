package com.lestora.highlight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class HighlightSphere {
    private double centerX;
    private double centerY;
    private double centerZ;
    private double highlightRadius;
    private final List<BlockPos> highlightedPositions = new CopyOnWriteArrayList<>();

    private static final ConcurrentHashMap<UUID, HighlightSphere> userConfigs = new ConcurrentHashMap<>();

    public static void setHighlightCenterAndRadius(UUID userId, double x, double y, double z, double radius, Level level) {
        userConfigs.compute(userId, (id, existingConfig) -> {
            if (existingConfig == null) {
                existingConfig = new HighlightSphere();
            }
            existingConfig.centerX = x;
            existingConfig.centerY = y;
            existingConfig.centerZ = z;
            existingConfig.highlightRadius = radius;
            existingConfig.updateHighlightedPositions(level);
            return existingConfig;
        });
    }

    public static HighlightSphere getUserHighlightConfig(UUID userId) {
        return userConfigs.get(userId);
    }

    public Boolean hasHighlights() { return !highlightedPositions.isEmpty(); }
    public List<BlockPos> getHighlightedPositions() { return highlightedPositions; }

    private int getBlockCoord(double coord) {
        return (int) Math.floor(coord);
    }

    private boolean isWithinSphere(BlockPos pos) {
        int centerBlockX = getBlockCoord(centerX);
        int centerBlockY = getBlockCoord(centerY);
        int centerBlockZ = getBlockCoord(centerZ);

        double dx = (pos.getX() + 0.5) - (centerBlockX + 0.5);
        double dy = (pos.getY() + 0.5) - (centerBlockY + 0.5);
        double dz = (pos.getZ() + 0.5) - (centerBlockZ + 0.5);
        double distSqr = dx * dx + dy * dy + dz * dz;
        return distSqr <= highlightRadius * highlightRadius;
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

    private void updateHighlightedPositions(Level level) {
        highlightedPositions.clear();
        int centerBlockX = getBlockCoord(centerX);
        int centerBlockY = getBlockCoord(centerY);
        int centerBlockZ = getBlockCoord(centerZ);
        int intRadius = (int) Math.ceil(highlightRadius);
        // Iterate over a cube surrounding the center block
        for (int x = centerBlockX - intRadius; x <= centerBlockX + intRadius; x++) {
            for (int y = centerBlockY - intRadius; y <= centerBlockY + intRadius; y++) {
                for (int z = centerBlockZ - intRadius; z <= centerBlockZ + intRadius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isWithinSphere(pos) && isBlockExposed(pos, level)) {
                        highlightedPositions.add(pos);
                    }
                }
            }
        }
    }

    public void remove(BlockPos brokenPos, Level level) {
        highlightedPositions.remove(brokenPos);
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = brokenPos.relative(dir);
            if (isWithinSphere(neighborPos)) {
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
        if (!isWithinSphere(placedPos)) return;
        if (isBlockExposed(placedPos, level)) {
            if (!highlightedPositions.contains(placedPos)) {
                highlightedPositions.add(placedPos);
            }
        } else {
            highlightedPositions.remove(placedPos);
        }
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = placedPos.relative(dir);
            if (isWithinSphere(neighborPos)) {
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