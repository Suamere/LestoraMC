package com.lestora.highlight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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

    public static List<LightEdges> findLightEdges(BlockPos torchPos, Level level) {
        List<LightEdges> lightEdges = new ArrayList<>();
        Queue<Node> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(new Node(torchPos, 14));
        visited.add(torchPos);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            BlockPos currentPos = current.pos;
            int currentLight = current.lightLevel;

            if (currentLight > 1) {
                // Propagate to adjacent transparent positions.
                for (Direction direction : Direction.values()) {
                    BlockPos neighborPos = currentPos.relative(direction);
                    if (visited.contains(neighborPos)) continue;

                    BlockState neighborState = level.getBlockState(neighborPos);
                    if (!HighlightMemory.isTransparent(neighborState, neighborPos)) continue;

                    visited.add(neighborPos);
                    queue.add(new Node(neighborPos, currentLight - 1));
                }
            } else if (currentLight == 1) {
                var actualLight = level.getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(currentPos);
                if (actualLight == 0)
                    System.err.println("Light Level 0 where 1 was expected, is your light level configuration right?");
                else if (actualLight > 1)
                    continue;

                List<BlockPos> lightLevel0 = new ArrayList<>();
                for (Direction direction : Direction.values()) {
                    BlockPos darkNeighbor = currentPos.relative(direction);
                    var actualNeighborLight = level.getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(darkNeighbor);
                    if (actualNeighborLight == 0)
                        lightLevel0.add(darkNeighbor);
                }
                if (!lightLevel0.isEmpty())
                    lightEdges.add(new LightEdges(currentPos, lightLevel0));
            }
        }

        return lightEdges;
    }

    public static class LightEdges {
        public final BlockPos LightLevel1;
        public final List<BlockPos> LightLevel0;

        public LightEdges(BlockPos lightLevel1, List<BlockPos> lightLevel0) {
            this.LightLevel1 = lightLevel1;
            this.LightLevel0 = lightLevel0;
        }
    }

    // Helper class to hold a BlockPos with its current light level.
    private static class Node {
        BlockPos pos;
        int lightLevel;

        Node(BlockPos pos, int lightLevel) {
            this.pos = pos;
            this.lightLevel = lightLevel;
        }
    }

    public static class RelativePos {
        public final int x;
        public final int y;
        public final int z;

        public RelativePos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String toString() {
            return "RelativePos{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
        }
    }

    public static RelativePos getRelativePosition(BlockPos sourceBlockPos, BlockPos targetBlockPos) {
        int dx = targetBlockPos.getX() - sourceBlockPos.getX();
        int dy = targetBlockPos.getY() - sourceBlockPos.getY();
        int dz = targetBlockPos.getZ() - sourceBlockPos.getZ();
        return new RelativePos(dx, dy, dz);
    }

    public static void processTorches(Player player, Level level) {
        if (level == null) return;
        removeTorches();
        var torchPositions = findTorchesAroundPlayer(player);
        for (BlockPos torchPos : torchPositions) {
            UUID torchUUID = UUID.nameUUIDFromBytes(
                    ("torch:" + torchPos.getX() + ":" + torchPos.getY() + ":" + torchPos.getZ())
                            .getBytes(StandardCharsets.UTF_8)
            );
            torchUUIDs.add(torchUUID);
            var edges = findLightEdges(torchPos, level);
            for (var edgePos : edges) {
                var relativePos = getRelativePosition(torchPos, edgePos.LightLevel1);
                processInnerHighlight(torchPos, edgePos.LightLevel1, relativePos.x, relativePos.z, level, torchUUID);
                for (var darkPos : edgePos.LightLevel0) {
                    // ToDo: MAYBE Check the edges again to see if this spot has light level 1?
                    var relativeDarkPos = getRelativePosition(edgePos.LightLevel1, darkPos);
                    processOuterHighlight(edgePos.LightLevel1, darkPos, relativeDarkPos.x, relativeDarkPos.z, level, torchUUID);
                }
            }
        }
    }

    private static void processInnerHighlight(BlockPos torchPos, BlockPos lightBlockPos, int dx, int dz, Level level, UUID torchUUID) {
        // UP face (block below)
        BlockPos upPos = lightBlockPos.below();
        if (HighlightMemory.isBlockSturdy(level, upPos, Direction.UP))
            processInnerHighlightUp(torchPos, lightBlockPos, dx, dz, torchUUID, upPos);
        // DOWN face (block above)
        BlockPos downPos = lightBlockPos.above();
        if (HighlightMemory.isBlockSturdy(level, downPos, Direction.DOWN))
            processInnerHighlightDown(torchPos, lightBlockPos, dx, dz, torchUUID, downPos);
        // SOUTH face (block north of current pos)
        BlockPos southPos = lightBlockPos.north();
        if (HighlightMemory.isBlockSturdy(level, southPos, Direction.SOUTH))
            processInnerHighlightSouth(torchPos, lightBlockPos, dx, dz, torchUUID, southPos);
        // NORTH face (block south of current pos)
        BlockPos northPos = lightBlockPos.south();
        if (HighlightMemory.isBlockSturdy(level, northPos, Direction.NORTH))
            processInnerHighlightNorth(torchPos, lightBlockPos, dx, dz, torchUUID, northPos);
        // EAST face (block west of current pos)
        BlockPos eastPos = lightBlockPos.west();
        if (HighlightMemory.isBlockSturdy(level, eastPos, Direction.EAST))
            processInnerHighlightEast(torchPos, lightBlockPos, dx, dz, torchUUID, eastPos);
        // WEST face (block east of current pos)
        BlockPos westPos = lightBlockPos.east();
        if (HighlightMemory.isBlockSturdy(level, westPos, Direction.WEST))
            processInnerHighlightWest(torchPos, lightBlockPos, dx, dz, torchUUID, westPos);
    }

    private static void processOuterHighlight(BlockPos torchPos, BlockPos lightBlockPos, int dx, int dz, Level level, UUID torchUUID) {
        // Outer highlights use black color.
        // For simplicity, we add outer highlights only on faces where a sturdy block exists.
        BlockPos upPos = lightBlockPos.below();
        if (HighlightMemory.isBlockSturdy(level, upPos, Direction.UP))
            processOuterHighlightUp(torchPos, lightBlockPos, dx, dz, torchUUID, upPos);
        BlockPos downPos = lightBlockPos.above();
        if (HighlightMemory.isBlockSturdy(level, downPos, Direction.DOWN))
            processOuterHighlightDown(torchPos, lightBlockPos, dx, dz, torchUUID, downPos);
        BlockPos southPos = lightBlockPos.north();
        if (HighlightMemory.isBlockSturdy(level, southPos, Direction.SOUTH))
            processOuterHighlightSouth(torchPos, lightBlockPos, dx, dz, torchUUID, southPos);
        BlockPos northPos = lightBlockPos.south();
        if (HighlightMemory.isBlockSturdy(level, northPos, Direction.NORTH))
            processOuterHighlightNorth(torchPos, lightBlockPos, dx, dz, torchUUID, northPos);
        BlockPos eastPos = lightBlockPos.west();
        if (HighlightMemory.isBlockSturdy(level, eastPos, Direction.EAST))
            processOuterHighlightEast(torchPos, lightBlockPos, dx, dz, torchUUID, eastPos);
        BlockPos westPos = lightBlockPos.east();
        if (HighlightMemory.isBlockSturdy(level, westPos, Direction.WEST))
            processOuterHighlightWest(torchPos, lightBlockPos, dx, dz, torchUUID, westPos);
    }

//=== Inner Highlight Handlers (Yellow) ===

    private static void processInnerHighlightUp(BlockPos torchPos, BlockPos lightBlockPos, int dx, int dz, UUID torchUUID, BlockPos targetPos) {
        // On the UP face, free coords: dx and dz.
        if (dx == 0) {
            var c1 = dz < 0 ? HighlightCorner.TOP_LEFT : HighlightCorner.BOTTOM_RIGHT;
            var c2 = dz < 0 ? HighlightCorner.LEFT     : HighlightCorner.RIGHT;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.UP, c1));
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.UP, c2));
        } else if (dz == 0) {
            var c1 = dx < 0 ? HighlightCorner.TOP_LEFT : HighlightCorner.BOTTOM_RIGHT;
            var c2 = dx < 0 ? HighlightCorner.UP       : HighlightCorner.DOWN;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.UP, c1));
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.UP, c2));
        } else {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.UP,
                    getCornerOut(torchPos, lightBlockPos)));
        }
    }

    private static void processInnerHighlightDown(BlockPos torchPos, BlockPos lightBlockPos, int dx, int dz, UUID torchUUID, BlockPos targetPos) {
        // For the DOWN face, we mirror the UP logic.
        if (dx == 0) {
            var c1 = dz < 0 ? HighlightCorner.BOTTOM_LEFT : HighlightCorner.TOP_RIGHT;
            var c2 = dz < 0 ? HighlightCorner.LEFT       : HighlightCorner.RIGHT;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.DOWN, c1));
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.DOWN, c2));
        } else if (dz == 0) {
            var c1 = dx < 0 ? HighlightCorner.BOTTOM_LEFT : HighlightCorner.TOP_RIGHT;
            var c2 = dx < 0 ? HighlightCorner.DOWN       : HighlightCorner.UP;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.DOWN, c1));
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.DOWN, c2));
        } else {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.DOWN, getCornerOut(torchPos, lightBlockPos)));
        }
    }

    private static void processInnerHighlightSouth(BlockPos torchPos, BlockPos lightBlockPos, int dx, int dz, UUID torchUUID, BlockPos targetPos) {
        // For the SOUTH face, if aligned then use a fixed corner.
        if (dx == 0 || dz == 0) {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.SOUTH, HighlightCorner.UP));
        } else {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.SOUTH, getCornerOut(torchPos, lightBlockPos)));
        }
    }

    private static void processInnerHighlightNorth(BlockPos torchPos, BlockPos lightBlockPos, int dx, int dz, UUID torchUUID, BlockPos targetPos) {
        // For the NORTH face, mirror SOUTH (choose a complementary fixed corner)
        if (dx == 0 || dz == 0) {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.NORTH, HighlightCorner.DOWN));
        } else {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.NORTH, getCornerOut(torchPos, lightBlockPos)));
        }
    }

    private static void processInnerHighlightEast(BlockPos torchPos, BlockPos lightBlockPos, int dx, int dz, UUID torchUUID, BlockPos targetPos) {
        // For the EAST face, if aligned then use a fixed corner.
        if (dx == 0 || dz == 0) {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.EAST, HighlightCorner.UP));
        } else {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.EAST, getCornerOut(torchPos, lightBlockPos)));
        }
    }

    private static void processInnerHighlightWest(BlockPos torchPos, BlockPos lightBlockPos, int dx, int dz, UUID torchUUID, BlockPos targetPos) {
        // For the WEST face, if aligned then use a fixed corner.
        if (dx == 0 || dz == 0) {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.WEST, HighlightCorner.UP));
        } else {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.yellow(), HighlightFace.WEST, getCornerOut(torchPos, lightBlockPos)));
        }
    }

//=== Outer Highlight Handlers (Black) ===

    private static void processOuterHighlightUp(BlockPos torchPos, BlockPos lightBlockPos, int dx, int dz, UUID torchUUID, BlockPos targetPos) {
        if (dx == 0) {
            var c1 = dz < 0 ? HighlightCorner.UP : HighlightCorner.DOWN;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.UP, c1));
        } else if (dz == 0) {
            var c1 = dx < 0 ? HighlightCorner.LEFT : HighlightCorner.RIGHT;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.UP, c1));
        } else {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.UP,
                    getCornerIn(torchPos, lightBlockPos)));
        }
    }

    private static void processOuterHighlightDown(BlockPos torchPos, BlockPos lightBlockPos, int dx, int dz, UUID torchUUID, BlockPos targetPos) {
        if (dx == 0) {
            var c1 = dz < 0 ? HighlightCorner.DOWN : HighlightCorner.UP;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.DOWN, c1));
        } else if (dz == 0) {
            var c1 = dx < 0 ? HighlightCorner.RIGHT : HighlightCorner.LEFT;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.DOWN, c1));
        } else {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.DOWN,
                    getCornerIn(torchPos, lightBlockPos)));
        }
    }

    private static void processOuterHighlightSouth(BlockPos torchPos, BlockPos lightBlockPos, int dx, int dz, UUID torchUUID, BlockPos targetPos) {
        if (dx == 0) {
            var c1 = dz < 0 ? HighlightCorner.UP : HighlightCorner.DOWN;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.SOUTH, c1));
        } else if (dz == 0) {
            var c1 = dx < 0 ? HighlightCorner.LEFT : HighlightCorner.RIGHT;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.SOUTH, c1));
        } else {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.SOUTH,
                    getCornerIn(torchPos, lightBlockPos)));
        }
    }

    private static void processOuterHighlightNorth(BlockPos torchPos, BlockPos lightBlockPos, int dx, int dz, UUID torchUUID, BlockPos targetPos) {
        if (dx == 0) {
            var c1 = dz < 0 ? HighlightCorner.DOWN : HighlightCorner.UP;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.NORTH, c1));
        } else if (dz == 0) {
            var c1 = dx < 0 ? HighlightCorner.RIGHT : HighlightCorner.LEFT;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.NORTH, c1));
        } else {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.NORTH,
                    getCornerIn(torchPos, lightBlockPos)));
        }
    }

    private static void processOuterHighlightEast(BlockPos torchPos, BlockPos lightBlockPos, int dx, int dz, UUID torchUUID, BlockPos targetPos) {
        if (dx == 0) {
            var c1 = dz < 0 ? HighlightCorner.UP : HighlightCorner.DOWN;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.EAST, c1));
        } else if (dz == 0) {
            var c1 = dx < 0 ? HighlightCorner.LEFT : HighlightCorner.RIGHT;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.EAST, c1));
        } else {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.EAST,
                    getCornerIn(torchPos, lightBlockPos)));
        }
    }

    private static void processOuterHighlightWest(BlockPos torchPos, BlockPos lightBlockPos, int dx, int dz, UUID torchUUID, BlockPos targetPos) {
        if (dx == 0) {
            var c1 = dz < 0 ? HighlightCorner.UP : HighlightCorner.DOWN;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.WEST, c1));
        } else if (dz == 0) {
            var c1 = dx < 0 ? HighlightCorner.LEFT : HighlightCorner.RIGHT;
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.WEST, c1));
        } else {
            HighlightMemory.add(torchUUID, new HighlightEntry(targetPos, HighlightColor.black(1.0f), HighlightFace.WEST,
                    getCornerIn(torchPos, lightBlockPos)));
        }
    }

//=== Helper Corner Methods ===

    private static @NotNull HighlightCorner getCornerIn(BlockPos torchPos, BlockPos pos) {
        int relX = pos.getX() - torchPos.getX();
        int relZ = pos.getZ() - torchPos.getZ();
        if (relX >= 0 && relZ < 0) return HighlightCorner.BOTTOM_LEFT;
        else if (relX < 0 && relZ < 0) return HighlightCorner.BOTTOM_RIGHT;
        else if (relX >= 0) return HighlightCorner.TOP_LEFT;
        else return HighlightCorner.TOP_RIGHT;
    }

    private static @NotNull HighlightCorner getCornerOut(BlockPos torchPos, BlockPos pos) {
        int relX = pos.getX() - torchPos.getX();
        int relZ = pos.getZ() - torchPos.getZ();
        if (relX >= 0 && relZ < 0) return HighlightCorner.TOP_RIGHT;
        else if (relX < 0 && relZ < 0) return HighlightCorner.TOP_LEFT;
        else if (relX >= 0) return HighlightCorner.BOTTOM_RIGHT;
        else return HighlightCorner.BOTTOM_LEFT;
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