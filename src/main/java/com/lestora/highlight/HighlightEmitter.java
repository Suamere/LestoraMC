package com.lestora.highlight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HighlightEmitter {
    private static List<BlockPos> findTorchesAroundPlayer(Player player) {
        Level level = player.level();
        if (level == null) return Collections.emptyList();
        BlockPos center = player.blockPosition();
        int radius = 40;
        int radiusSq = radius * radius;
        List<BlockPos> torchPositions = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (level.getBlockState(pos).getBlock() == Blocks.TORCH) {
                            torchPositions.add(pos);
                        }
                    }
                }
            }
        }
        return torchPositions;
    }

    private static final Set<UUID> torchUUIDs = ConcurrentHashMap.newKeySet();
    public static void processTorches(Player player, Level level) {
        if (level == null) return;
        removeTorches();
        int outerRadius = 14;
        var torchPositions = findTorchesAroundPlayer(player);
        for (BlockPos torchPos : torchPositions) {
            UUID torchUUID = UUID.nameUUIDFromBytes(
                    ("torch:" + torchPos.getX() + ":" + torchPos.getY() + ":" + torchPos.getZ())
                            .getBytes(StandardCharsets.UTF_8)
            );
//            var torchEntry1 = new HighlightEntry(torchPos.below(), generateColorFromPos(torchPos, 1.0f), HighlightFace.UP, HighlightCorner.NORTH_EAST);
//            var torchEntry2 = new HighlightEntry(torchPos.below(), generateColorFromPos(torchPos, 1.0f), HighlightFace.UP, HighlightCorner.SOUTH_WEST);
//            HighlightMemory.add(torchUUID, torchEntry1);
//            HighlightMemory.add(torchUUID, torchEntry2);
            torchUUIDs.add(torchUUID);
            var innerRadius = outerRadius - 1;
            for (int dx = -outerRadius; dx <= outerRadius; dx++) {
                for (int dy = -outerRadius; dy <= outerRadius; dy++) {
                    for (int dz = -outerRadius; dz <= outerRadius; dz++) {
                        int manhattanDistance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                        if (manhattanDistance == outerRadius || manhattanDistance == innerRadius) {
                            BlockPos lightBlockPos = torchPos.offset(dx, dy, dz);
                            var lightBlockState = level.getBlockState(lightBlockPos);
                            if (HighlightMemory.isTransparent(lightBlockState, lightBlockPos)) {
                                int blockLight = level.getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(lightBlockPos);
                                var belowLightPos = lightBlockPos.below();
                                var northLightPos = lightBlockPos.north();
                                if (manhattanDistance == innerRadius && blockLight == 1) {
                                    if (HighlightMemory.isBlockSturdy(level, belowLightPos, Direction.UP) && HighlightMemory.isBlockExposed(belowLightPos, level)) {
                                        if (dx == 0 && Math.abs(dz) == innerRadius) {
                                            var c1 = dz < 0 ? HighlightCorner.NORTH_WEST : HighlightCorner.SOUTH_EAST;
                                            var c2 = dz < 0 ? HighlightCorner.WEST : HighlightCorner.EAST;
                                            HighlightMemory.add(torchUUID, new HighlightEntry(belowLightPos, HighlightColor.yellow(), HighlightFace.UP, c1));
                                            HighlightMemory.add(torchUUID, new HighlightEntry(belowLightPos, HighlightColor.yellow(), HighlightFace.UP, c2));
                                        } else if (dz == 0 && Math.abs(dx) == innerRadius) {
                                            var c1 = dx < 0 ? HighlightCorner.NORTH_WEST : HighlightCorner.SOUTH_EAST;
                                            var c2 = dx < 0 ? HighlightCorner.NORTH : HighlightCorner.SOUTH;
                                            HighlightMemory.add(torchUUID, new HighlightEntry(belowLightPos, HighlightColor.yellow(), HighlightFace.UP, c1));
                                            HighlightMemory.add(torchUUID, new HighlightEntry(belowLightPos, HighlightColor.yellow(), HighlightFace.UP, c2));
                                        } else {
                                            HighlightMemory.add(torchUUID, new HighlightEntry(belowLightPos, HighlightColor.yellow(), HighlightFace.UP, getCornerOut(torchPos, lightBlockPos, Direction.UP)));
                                        }
                                    }
                                    if (HighlightMemory.isBlockSturdy(level, northLightPos, Direction.SOUTH) && HighlightMemory.isBlockExposed(northLightPos, level)) {
                                        HighlightMemory.add(torchUUID, new HighlightEntry(northLightPos, HighlightColor.yellow(), HighlightFace.SOUTH, getCornerOut(torchPos, lightBlockPos, Direction.SOUTH)));
                                    }
                                }
                                else if (manhattanDistance == outerRadius && blockLight == 0) {
                                    if (HighlightMemory.isBlockSturdy(level, belowLightPos, Direction.UP) && HighlightMemory.isBlockExposed(belowLightPos, level)) {
                                        if (dx == 0 && Math.abs(dz) == outerRadius) {
                                            var c1 = dz < 0 ? HighlightCorner.NORTH : HighlightCorner.SOUTH;
                                            HighlightMemory.add(torchUUID, new HighlightEntry(belowLightPos, HighlightColor.black(1.0f), HighlightFace.UP, c1));
                                        } else if (dz == 0 && Math.abs(dx) == outerRadius) {
                                            var c1 = dx < 0 ? HighlightCorner.WEST : HighlightCorner.EAST;
                                            HighlightMemory.add(torchUUID, new HighlightEntry(belowLightPos, HighlightColor.black(1.0f), HighlightFace.UP, c1));
                                        } else {
                                            HighlightMemory.add(torchUUID, new HighlightEntry(belowLightPos, HighlightColor.black(1.0f), HighlightFace.UP, getCornerIn(torchPos, lightBlockPos)));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static @NotNull HighlightCorner getCornerIn(BlockPos torchPos, BlockPos pos) {
        int relX = pos.getX() - torchPos.getX();
        int relZ = pos.getZ() - torchPos.getZ();

        HighlightCorner corner;
        if (relX >= 0 && relZ < 0) { corner = HighlightCorner.SOUTH_WEST; }
        else if (relX < 0 && relZ < 0) { corner = HighlightCorner.SOUTH_EAST; }
        else if (relX >= 0) { corner = HighlightCorner.NORTH_WEST; }
        else { corner = HighlightCorner.NORTH_EAST; }
        return corner;
    }

    private static @NotNull HighlightCorner getCornerOut(BlockPos torchPos, BlockPos pos, Direction facing) {
        int relX = pos.getX() - torchPos.getX();
        int relZ = pos.getZ() - torchPos.getZ();

        HighlightCorner corner;
        if (relX >= 0 && relZ < 0) { corner = HighlightCorner.NORTH_EAST; }
        else if (relX < 0 && relZ < 0) { corner = HighlightCorner.NORTH_WEST; }
        else if (relX >= 0) { corner = HighlightCorner.SOUTH_EAST; }
        else { corner = HighlightCorner.SOUTH_WEST; }
        if (facing == Direction.UP) return corner;

        if (facing == Direction.SOUTH) {
            if (corner == HighlightCorner.NORTH_EAST) return HighlightCorner.BOTTOM_WEST;
            if (corner == HighlightCorner.NORTH_WEST) return HighlightCorner.BOTTOM_EAST;
        }

        return HighlightCorner.TOP_EAST;
    }

    public static void removeTorches() {
        for (UUID torchUUID : torchUUIDs)
            HighlightMemory.clear(torchUUID);

        torchUUIDs.clear();
    }

    private static HighlightColor generateColorFromUUID(UUID uuid, float alpha) {
        // Use bits from the most and least significant parts of the UUID
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        // Extract 8 bits for each channel
        int r = (int)((msb >> 16) & 0xFF);
        int b = (int)((lsb >> 48) & 0xFF);

        // Scale to [0,1] range for floats.
        float rf = r / 255.0f;
        float bf = b / 255.0f;

        return new HighlightColor(rf, 0, bf, alpha);
    }

    public static HighlightColor generateColorFromPos(BlockPos pos, float alpha) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        // Determine chunk coordinates and local coordinates.
        int chunkX = Math.floorDiv(x, 16);
        int chunkZ = Math.floorDiv(z, 16);
        int localX = Math.floorMod(x, 16);
        int localZ = Math.floorMod(z, 16);

        // Map the block's coordinates into a "super-chunk" area (3x3 chunks, range 0-47).
        int superX = (((chunkX % 3) + 3) % 3) * 16 + localX;
        int superZ = (((chunkZ % 3) + 3) % 3) * 16 + localZ;

        // Adjust X by shifting it by the Z-ordinal (superZ) and wrap within 0-47.
        int newX = (superX + superZ) % 48;
        // For every odd superZ, reverse the X ordering.
        if (superZ % 2 == 1) {
            newX = 47 - newX;
        }

        // Adjust Z by shifting it by the X-ordinal (superX) and wrap.
        int newZ = (superZ + superX) % 48;
        // For every odd superX, reverse the Z ordering.
        if (superX % 2 == 1) {
            newZ = 47 - newZ;
        }

        // Determine the green value from Y mod 3.
        int modY = Math.floorMod(y, 3);
        float green;
        switch (modY) {
            case 0: green = 0.2f; break;
            case 1: green = 0.5f; break;
            case 2: green = 0.8f; break;
            default: green = 0.0f; break;
        }

        // Scale newX and newZ from [0,47] to [0.0f,1.0f]
        float red = newX / 47.0f;
        float blue = newZ / 47.0f;

        return new HighlightColor(red, green, blue, alpha);
    }
}