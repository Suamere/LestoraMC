package com.lestora.util;

import net.minecraft.core.BlockPos;

public class Coordinates {
    private final int x;
    private final int y;
    private final int z;

    private Coordinates(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Coordinates fromPos(BlockPos pos) {
        return new Coordinates(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public String toString() {
        return x + ", " + y + ", " + z;
    }
}