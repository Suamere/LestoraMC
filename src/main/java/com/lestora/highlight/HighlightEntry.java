package com.lestora.highlight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.openjdk.nashorn.internal.ir.LexicalContext;


public class HighlightEntry {
    public BlockPos pos;
    public HighlightColor color;
    public HighlightFace face;
    public HighlightCorner corner;

    public HighlightEntry(BlockPos pos, HighlightColor color, HighlightFace face, HighlightCorner corner) {
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

    public static HighlightEntry Whole(BlockPos blockPos, HighlightColor color) {
        return new HighlightEntry(blockPos, color);
    }

    public static HighlightFace fromOppositeDirection(Direction dir) {
        return switch (dir) {
            case UP -> HighlightFace.DOWN;
            case DOWN -> HighlightFace.UP;
            case SOUTH -> HighlightFace.NORTH;
            case NORTH -> HighlightFace.SOUTH;
            case EAST -> HighlightFace.WEST;
            case WEST -> HighlightFace.EAST;
        };
    }

    public static HighlightFace fromDirection(Direction dir) {
        return switch (dir) {
            case UP -> HighlightFace.UP;
            case DOWN -> HighlightFace.DOWN;
            case SOUTH -> HighlightFace.SOUTH;
            case NORTH -> HighlightFace.NORTH;
            case EAST -> HighlightFace.EAST;
            case WEST -> HighlightFace.WEST;
        };
    }

    public static HighlightCorner corner(Direction facingDirection, Direction sideDir) {
        return switch (facingDirection){
            case UP -> switch (sideDir) {
                case NORTH -> HighlightCorner.UP;
                case EAST -> HighlightCorner.LEFT;
                case SOUTH -> HighlightCorner.DOWN;
                case WEST -> HighlightCorner.RIGHT;
                default -> null;
            };
            case DOWN -> switch (sideDir) {
                case NORTH -> HighlightCorner.DOWN;
                case EAST -> HighlightCorner.LEFT;
                case SOUTH -> HighlightCorner.UP;
                case WEST -> HighlightCorner.RIGHT;
                default -> null;
            };
            case NORTH -> switch (sideDir) {
                case EAST -> HighlightCorner.LEFT;
                case WEST -> HighlightCorner.RIGHT;
                case UP -> HighlightCorner.DOWN;
                case DOWN -> HighlightCorner.UP;
                default -> null;
            };
            case EAST -> switch (sideDir) {
                case NORTH -> HighlightCorner.RIGHT;
                case SOUTH -> HighlightCorner.LEFT;
                case UP -> HighlightCorner.DOWN;
                case DOWN -> HighlightCorner.UP;
                default -> null;
            };
            case SOUTH -> switch (sideDir) {
                case EAST -> HighlightCorner.RIGHT;
                case WEST -> HighlightCorner.LEFT;
                case UP -> HighlightCorner.DOWN;
                case DOWN -> HighlightCorner.UP;
                default -> null;
            };
            case WEST -> switch (sideDir) {
                case NORTH -> HighlightCorner.LEFT;
                case SOUTH -> HighlightCorner.RIGHT;
                case UP -> HighlightCorner.DOWN;
                case DOWN -> HighlightCorner.UP;
                default -> null;
            };
        };
    }

    public static HighlightFace shiftFace(HighlightFace face, HighlightCorner sideDir) {
        return switch (face) {
            case UP -> switch (sideDir) {
                case UP -> HighlightFace.NORTH;
                case DOWN -> HighlightFace.SOUTH;
                case LEFT -> HighlightFace.WEST;
                case RIGHT -> HighlightFace.EAST;
                default -> null;
            };
            case DOWN -> switch (sideDir) {
                case UP -> HighlightFace.NORTH;
                case DOWN -> HighlightFace.SOUTH;
                case LEFT -> HighlightFace.EAST;
                case RIGHT -> HighlightFace.WEST;
                default -> null;
            };
            case EAST -> switch (sideDir) {
                case UP -> HighlightFace.UP;
                case DOWN -> HighlightFace.DOWN;
                case LEFT -> HighlightFace.NORTH;
                case RIGHT -> HighlightFace.SOUTH;
                default -> null;
            };
            case WEST -> switch (sideDir) {
                case UP -> HighlightFace.UP;
                case DOWN -> HighlightFace.DOWN;
                case LEFT -> HighlightFace.SOUTH;
                case RIGHT -> HighlightFace.NORTH;
                default -> null;
            };
            case NORTH -> switch (sideDir) {
                case UP -> HighlightFace.UP;
                case DOWN -> HighlightFace.DOWN;
                case LEFT -> HighlightFace.EAST;
                case RIGHT -> HighlightFace.WEST;
                default -> null;
            };
            case SOUTH -> switch (sideDir) {
                case UP -> HighlightFace.UP;
                case DOWN -> HighlightFace.DOWN;
                case LEFT -> HighlightFace.WEST;
                case RIGHT -> HighlightFace.EAST;
                default -> null;
            };
        };
    }
}