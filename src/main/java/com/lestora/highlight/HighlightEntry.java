package com.lestora.highlight;

import net.minecraft.core.BlockPos;


public class HighlightEntry {
    public final BlockPos pos;
    public final HighlightColor color;
    public final HighlightFace face;
    public final HighlightCorner corner;

    public HighlightEntry(BlockPos pos, HighlightColor color, HighlightFace face, HighlightCorner corner) {
        validate(face, corner);
        this.pos = pos;
        this.color = color;
        this.face = face;
        this.corner = corner;
    }

    private HighlightEntry(BlockPos pos, HighlightColor color) {
        this.pos = pos;
        this.color = color;
        this.face = null;
        this.corner = null;
    }

    private static void validate(HighlightFace face, HighlightCorner corner) {
        switch (face) {
            case UP:
            case DOWN:
                // For UP or DOWN, only allow the corner options: NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST.
                if (!(corner == HighlightCorner.NORTH_EAST ||
                        corner == HighlightCorner.NORTH_WEST ||
                        corner == HighlightCorner.SOUTH_EAST ||
                        corner == HighlightCorner.SOUTH_WEST ||
                        corner == HighlightCorner.NORTH ||
                        corner == HighlightCorner.SOUTH ||
                        corner == HighlightCorner.WEST ||
                        corner == HighlightCorner.EAST)) {
                    throw new IllegalArgumentException("For face " + face +
                            ", corner must be one of NORTH_EAST, NORTH_WEST, SOUTH_EAST, or SOUTH_WEST.");
                }
                break;
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST:
                // For side faces, allow only the corners with TOP or BOTTOM prefixes.
                boolean valid = (corner == HighlightCorner.TOP_NORTH ||
                        corner == HighlightCorner.TOP_SOUTH ||
                        corner == HighlightCorner.TOP_EAST  ||
                        corner == HighlightCorner.TOP_WEST  ||
                        corner == HighlightCorner.BOTTOM_NORTH ||
                        corner == HighlightCorner.BOTTOM_SOUTH ||
                        corner == HighlightCorner.BOTTOM_EAST  ||
                        corner == HighlightCorner.BOTTOM_WEST);
                if (!valid) {
                    throw new IllegalArgumentException("For face " + face +
                            ", corner must be one of TOP_NORTH, TOP_SOUTH, TOP_EAST, TOP_WEST, " +
                            "BOTTOM_NORTH, BOTTOM_SOUTH, BOTTOM_EAST, or BOTTOM_WEST.");
                }
                break;
        }
    }

    public static HighlightEntry Whole(BlockPos neighborPos, HighlightColor red) {
        return new HighlightEntry(neighborPos, red);
    }
}