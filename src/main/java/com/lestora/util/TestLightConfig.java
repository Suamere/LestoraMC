package com.lestora.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class TestLightConfig {
    public static final Logger LOGGER = LogManager.getLogger("lestora");

    public record EntityPair(Entity first, ResourceLocation second) {}
    public record PosAndName(BlockPos position, ResourceLocation resource) {}

    private static boolean enabled = true;
    private static final Lock lock = new ReentrantLock();
    private static final ConcurrentHashMap<UUID, PosAndName> currentPositions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, EntityPair> registeredEntities = new ConcurrentHashMap<>();

    public static Collection<PosAndName> getCurrentPositions() {
        lock.lock();
        try {
            // Instead, return the collection of two values... the BlockPos and the ItemName
            return new ArrayList<>(currentPositions.values());
        } finally {
            lock.unlock();
        }
    }

    public static void tryUpdateEntityPositions() {
        lock.lock();
        try {
            for (Map.Entry<UUID, EntityPair> entry : registeredEntities.entrySet()) {
                var e = entry.getValue();
                BlockPos newPos = e.first().blockPosition();
                PosAndName oldPos = currentPositions.getOrDefault(e.first().getUUID(), new PosAndName(BlockPos.ZERO, null));
                if (!newPos.equals(oldPos)) {
                    currentPositions.put(e.first().getUUID(), new PosAndName(newPos.immutable(), e.second()));

                    var level = Minecraft.getInstance().level;
                    if (level != null) {
                        ClientChunkCache chunkSource = level.getChunkSource();
                        LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                        lightingEngine.checkBlock(newPos);
                        lightingEngine.checkBlock(oldPos.position);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static void tryAddEntity(Entity e, ResourceLocation resource) {
        lock.lock();
        try {
            registeredEntities.putIfAbsent(e.getUUID(), new EntityPair(e, resource));
        } finally {
            lock.unlock();
        }
    }

    public static void tryRemoveEntity(Entity e) {
        lock.lock();
        try {
            registeredEntities.remove(e.getUUID());
            var oldPos = currentPositions.remove(e.getUUID());
            var level = Minecraft.getInstance().level;
            if (level != null && oldPos != null) {
                ClientChunkCache chunkSource = level.getChunkSource();
                LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                lightingEngine.checkBlock(oldPos.position);
            }
        } finally {
            lock.unlock();
        }
    }

    public static boolean getEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean newVal) {
        lock.lock();
        try {
            if (newVal == enabled) return;

            enabled = newVal;
            if (newVal == false) {
                for (EntityPair e : registeredEntities.values()) {
                    var oldPos = currentPositions.getOrDefault(e.first().getUUID(), new PosAndName(BlockPos.ZERO, null));

                    var level = Minecraft.getInstance().level;
                    if (level != null) {
                        ClientChunkCache chunkSource = level.getChunkSource();
                        LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                        lightingEngine.checkBlock(oldPos.position);
                    }
                }
            }
            else {
                for (EntityPair e : registeredEntities.values()) {
                    BlockPos newPos = e.first().blockPosition();

                    var level = Minecraft.getInstance().level;
                    if (level != null) {
                        ClientChunkCache chunkSource = level.getChunkSource();
                        LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                        lightingEngine.checkBlock(newPos);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }
}