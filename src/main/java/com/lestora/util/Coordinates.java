package com.lestora.util;

import net.minecraft.core.BlockPos;

// A simple coordinates model.
public class Coordinates {
    private final int x;
    private final int y;
    private final int z;

    public Coordinates(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Coordinates fromPos(BlockPos pos) {
        return new Coordinates(pos.getX(), pos.getY(), pos.getZ());
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    @Override
    public String toString() {
        return x + ", " + y + ", " + z;
    }
}