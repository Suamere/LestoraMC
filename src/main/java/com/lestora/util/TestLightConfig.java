package com.lestora.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.lighting.LevelLightEngine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class TestLightConfig {
    private static final Lock lock = new ReentrantLock();
    private static final ConcurrentHashMap<UUID, BlockPos> currentPositions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Entity> registeredEntities = new ConcurrentHashMap<>();

    public static Collection<BlockPos> getCurrentPositions() {
        lock.lock();
        try {
            return new ArrayList<>(currentPositions.values());
        } finally {
            lock.unlock();
        }
    }

    public static void tryUpdateEntityPositions() {
        lock.lock();
        try {
            for (Entity e : registeredEntities.values()) {
                BlockPos newPos = e.blockPosition();
                BlockPos oldPos = currentPositions.getOrDefault(e.getUUID(), BlockPos.ZERO);
                if (!newPos.equals(oldPos)) {
                    currentPositions.put(e.getUUID(), newPos.immutable());

                    var level = Minecraft.getInstance().level;
                    if (level != null) {
                        ClientChunkCache chunkSource = level.getChunkSource();
                        LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                        lightingEngine.checkBlock(newPos);
                        lightingEngine.checkBlock(oldPos);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static void tryAddEntity(Entity e) {
        lock.lock();
        try {
            registeredEntities.putIfAbsent(e.getUUID(), e);
        } finally {
            lock.unlock();
        }
    }

    public static void tryRemoveEntity(Entity e) {
        lock.lock();
        try {
            registeredEntities.remove(e.getUUID());
            currentPositions.remove(e.getUUID());
        } finally {
            lock.unlock();
        }
    }
}