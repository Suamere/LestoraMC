package com.lestora.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.lestora.util.EntityBlockInfo;
import com.lestora.util.StandingBlockUtil;
import com.lestora.util.Wetness;
import com.lestora.util.WetnessUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class LestoraPlayer {
    private final Player mcPlayer;
    private final UUID uuid;
    private EntityBlockInfo supportingBlock;
    private Wetness wetness;
    private Biome biome;
    private boolean lastBlockSolid;

    // Timers in seconds
    private float submergedTimer = 0f;
    private float soakedTimer = 0f;
    private float dampTimer = 0f;
    // Timestamp in milliseconds
    private long lastUpdateTime = 0;

    // In-memory static collection of players
    private static final Map<UUID, LestoraPlayer> players = new HashMap<>();

    public LestoraPlayer(Player player) {
        this.mcPlayer = player;
        this.uuid = player.getUUID();
    }

    public UUID getUuid() {
        return uuid;
    }

    public EntityBlockInfo getSupportingBlock() {
        return supportingBlock;
    }

    public BlockState getStateStanding() {
        return this.supportingBlock.getSupportingBlock();
    }

    public Wetness getWetness() {
        return wetness;
    }

    public Biome getBiome() {
        return biome;
    }

    public static LestoraPlayer get(Player player) {
        return players.computeIfAbsent(player.getUUID(), key -> {
            var lestoraPlayer = new LestoraPlayer(player);
            lestoraPlayer.calc();
            return lestoraPlayer;
        });
    }

    public void calc() {
        if (this.supportingBlock != null) {
            var thisBlock = this.supportingBlock.getSupportingBlock().getBlock();
            if (thisBlock == Blocks.WATER) {
                lastBlockSolid = false;
            }
            else if (thisBlock != Blocks.AIR) {
                lastBlockSolid = true;
            }
        }
        this.supportingBlock = StandingBlockUtil.getSupportingBlock(this.mcPlayer, lastBlockSolid);

        long now = System.currentTimeMillis();
        if (lastUpdateTime == 0) {
            lastUpdateTime = now;
            return;
        }
        float delta = (now - lastUpdateTime) / 1000f; // seconds elapsed
        lastUpdateTime = now;

        // Decrement any active timers
        if (submergedTimer > 0) submergedTimer = Math.max(0, submergedTimer - delta);
        if (soakedTimer > 0) soakedTimer = Math.max(0, soakedTimer - delta);
        if (dampTimer > 0) dampTimer = Math.max(0, dampTimer - delta);

        // Get current "raw" wetness from your utility method.
        var newWetness = WetnessUtil.getPlayerWetness(this.mcPlayer, this.supportingBlock);

        // If the player is (re)submerged, reset the SubmergedTimer
        if (newWetness == Wetness.FULLY_SUBMERGED || newWetness == Wetness.NEARLY_SUBMERGED) {
            submergedTimer = 5f;
            soakedTimer = 0f;
            dampTimer = 0f;
            this.wetness = newWetness;
        } else {
            // If the player was previously fully/near-submerged but no longer,
            // wait until the submerged timer runs out then downgrade to SOAKED.
            if (this.wetness == Wetness.FULLY_SUBMERGED || this.wetness == Wetness.NEARLY_SUBMERGED) {
                this.wetness = Wetness.NEARLY_SUBMERGED;
                if (submergedTimer <= 0) {
                    this.wetness = Wetness.SOAKED;
                    soakedTimer = 5f;
                    submergedTimer = 0f;
                }
            }
            // If the "raw" wetness indicates SOAKED, reset the soaked timer.
            else if (newWetness == Wetness.SOAKED) {
                soakedTimer = 5f;
                submergedTimer = 0f;
                dampTimer = 0f;
                this.wetness = Wetness.SOAKED;
            } else {
                // If the player was previously SOAKED, wait until the soaked timer expires before downgrading to DAMP.
                if (this.wetness == Wetness.SOAKED) {
                    if (soakedTimer <= 0) {
                        this.wetness = Wetness.DAMP;
                        dampTimer = 5f;
                        soakedTimer = 0f;
                    }
                }
                // If the raw state is DAMP, reset the damp timer.
                else if (newWetness == Wetness.DAMP) {
                    dampTimer = 5f;
                    submergedTimer = 0f;
                    soakedTimer = 0f;
                    this.wetness = Wetness.DAMP;
                } else {
                    // Otherwise, when damp timer expires, set state to DRY.
                    if (dampTimer <= 0) {
                        this.wetness = Wetness.DRY;
                        dampTimer = 0f;
                        submergedTimer = 0f;
                        soakedTimer = 0f;
                    }
                }
            }
        }
    }
}