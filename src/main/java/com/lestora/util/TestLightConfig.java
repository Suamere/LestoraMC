package com.lestora.util;

import net.minecraft.core.BlockPos;

public final class TestLightConfig {
    private static int testLightLevel = 14;
    private static BlockPos testPos = BlockPos.ZERO;

    private TestLightConfig() {}

    public static void setTestLightLevel(int level) {
        if (level < 0 || level > 14) {
            throw new IllegalArgumentException("Light level must be between 0 and 14");
        }
        testLightLevel = level;
    }

    public static int getTestLightLevel() {
        return testLightLevel;
    }

    public static void setTestPos(BlockPos pos) {
        testPos = pos.immutable();
    }

    public static BlockPos getTestPos() {
        return testPos;
    }
}
