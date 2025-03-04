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

    private void updateHighlightedPositions(Level level) {
        highlightedPositions.clear();
        int centerBlockX = getBlockCoord(centerX);
        int centerBlockY = getBlockCoord(centerY);
        int centerBlockZ = getBlockCoord(centerZ);
        int intRadius = (int) Math.ceil(highlightRadius);
        double radiusSq = highlightRadius * highlightRadius;

        for (int dx = -intRadius; dx <= intRadius; dx++) {
            double dxSq = dx * dx;
            for (int dy = -intRadius; dy <= intRadius; dy++) {
                double dySq = dy * dy;
                double dxySq = dxSq + dySq;
                if (dxySq > radiusSq) continue;
                int maxDz = (int) Math.floor(Math.sqrt(radiusSq - dxySq));
                for (int dz = -maxDz; dz <= maxDz; dz++) {
                    BlockPos pos = new BlockPos(centerBlockX + dx, centerBlockY + dy, centerBlockZ + dz);
                    if (isBlockExposed(pos, level) && !highlightedPositions.contains(pos)) {
                        highlightedPositions.add(pos);
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
        if (level.getBlockState(brokenPos).canOcclude()) return;
        CheckSurroundings(brokenPos, level);
    }

    public void add(BlockPos placedPos, Level level) {
        if (!isWithinSphere(placedPos)) return;
        if (!level.getBlockState(placedPos).canOcclude()) return;
        // This seems obvious... the block was just placed, so it's exposed
        // But depending on the timing, or falling "sand", etc... it might not be true.
        if (isBlockExposed(placedPos, level)) {
            if (!highlightedPositions.contains(placedPos)) {
                highlightedPositions.add(placedPos);
            }
        } else {
            highlightedPositions.remove(placedPos);
        }
        CheckSurroundings(placedPos, level);
    }

    private void CheckSurroundings(BlockPos blockPos, Level level) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = blockPos.relative(dir);
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