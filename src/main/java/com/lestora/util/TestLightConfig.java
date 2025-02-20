package com.lestora.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LevelLightEngine;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class TestLightConfig {
    private static final Lock lock = new ReentrantLock();
    private static BlockPos currentPos = BlockPos.ZERO;

    public static BlockPos getCurrentPos() {
        lock.lock();
        try {
            return currentPos;
        } finally {
            lock.unlock();
        }
    }

    public static void tryUpdateLightPos(BlockPos newPos) {
        lock.lock();
        try {
            if (!newPos.equals(currentPos)) {
                BlockPos oldSpace = currentPos;
                currentPos = newPos.immutable();

                var level = Minecraft.getInstance().level;
                if (level != null) {
                    ClientChunkCache chunkSource = level.getChunkSource();
                    LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                    lightingEngine.checkBlock(newPos);
                    lightingEngine.checkBlock(oldSpace);
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
