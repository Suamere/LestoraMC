package com.lestora.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.lestora.event.ConfigBiomeTempEventHandler;
import com.lestora.util.EntityBlockInfo;
import com.lestora.util.StandingBlockUtil;
import com.lestora.util.Wetness;
import com.lestora.util.WetnessUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class LestoraPlayer {
    private Player mcPlayer;
    private final UUID uuid;
    private EntityBlockInfo supportingBlock;
    private Wetness wetness;
    private Biome nonRiverBiome;
    private Biome biome;
    private boolean lastBlockSolid;
    private float bodyTemp = 0f;

    // Timers in seconds
    private float submergedTimer = 0f;
    private float soakedTimer = 0f;
    private float dampTimer = 0f;
    // Timestamp in milliseconds
    private long lastUpdateTime = 0;

    // Cache of nearby block states (cube from -5 to 5 in x,y,z)
    private final Map<BlockPos, BlockState> cachedBlockStates = new HashMap<>();

    // In-memory static collection of players
    private static final Map<UUID, LestoraPlayer> players = new HashMap<>();
    private ClientLevel level;

    public LestoraPlayer(Player player) {
        this.mcPlayer = player;
        this.uuid = player.getUUID();
    }

    public UUID getUuid() {
        return uuid;
    }

    public float getBodyTemp() {
        return this.bodyTemp;
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
            lestoraPlayer.calc(player);
            return lestoraPlayer;
        });
    }

    public void calc(Player player) {
        this.level = Minecraft.getInstance().level;
        if (this.level == null || this.mcPlayer == null) return;
        this.mcPlayer = player;

        // Cache the nearby blocks at the start.
        cacheNearbyBlocks();
        // Temporal Coupling FTW.  Keep these in order:
        CalculateSupportBlock();
        CalculateWetness();
        CalculateBodyTemp();
    }

    // Cache a cube of block states around the player (from -5 to +5 in x, y, and z).
    private void cacheNearbyBlocks() {
        BlockPos playerPos = mcPlayer.blockPosition();
        cachedBlockStates.clear();
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    cachedBlockStates.put(pos, this.level.getBlockState(pos));
                }
            }
        }
    }

    private void CalculateBodyTemp() {
        BlockPos playerPos = mcPlayer.blockPosition();
        Holder<Biome> biomeHolder = this.level.getBiome(playerPos);
        this.biome = biomeHolder.value();
        float baseTemp = ConfigBiomeTempEventHandler.getBiomeTemp(this.biome);

        // If player's altitude is below 60 and the biome is not one of the underground biomes,
        // then override baseTemp to 0.5.
        if (mcPlayer.getBlockY() < 60) {
            ResourceLocation biomeRL = getBiomeResourceLocation(this.level, this.biome);
            if (biomeRL == null ||
                    (!biomeRL.equals(ResourceLocation.parse("minecraft:dripstone_caves"))
                            && !biomeRL.equals(ResourceLocation.parse("minecraft:lush_caves"))
                            && !biomeRL.equals(ResourceLocation.parse("minecraft:deep_dark")))) {
                baseTemp = 0.5f;
            }
        }

        // Calculate lava proximity offset.
        int lavaOffset = 0;
        // First, check if the player is in lava.
        BlockState currentState = cachedBlockStates.get(playerPos);
        if (currentState != null && currentState.getBlock() == Blocks.LAVA) {
            bodyTemp = 300;
            return;
        } else {
            // Check for nearby lava within 5 blocks using the cached data.
            for (int d = 1; d <= 5; d++) {
                boolean foundLava = false;
                // Check in a cube of radius 'd' around the player.
                outer:
                for (int x = -d; x <= d; x++) {
                    for (int y = -d; y <= d; y++) {
                        for (int z = -d; z <= d; z++) {
                            BlockPos checkPos = playerPos.offset(x, y, z);
                            BlockState state = cachedBlockStates.get(checkPos);
                            if (state != null && state.getBlock() == Blocks.LAVA) {
                                foundLava = true;
                                break outer;
                            }
                        }
                    }
                }
                if (foundLava) {
                    switch (d) {
                        case 5:
                            lavaOffset = 10;
                            break;
                        case 4:
                            lavaOffset = 20;
                            break;
                        case 3:
                            lavaOffset = 30;
                            break;
                        case 2:
                            lavaOffset = 50;
                            break;
                        case 1:
                            lavaOffset = 75;
                            break;
                    }
                    break; // Use the first (closest) found lava distance.
                }
            }
        }

        var thisWetness = this.wetness == Wetness.FULLY_SUBMERGED ? Wetness.NEARLY_SUBMERGED : this.wetness;
        var wetnessOffset = Math.min(17 * thisWetness.ordinal(), 50);
        Integer altitudeOffset = (mcPlayer.getBlockY() - 60) / 5;
        // Incorporate lavaOffset into the final body temperature calculation.
        bodyTemp = (baseTemp * 25 - wetnessOffset - altitudeOffset) + 60 + lavaOffset;
    }

    private void CalculateWetness() {
        long now = System.currentTimeMillis();
        if (lastUpdateTime == 0) {
            lastUpdateTime = now;
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

    private void CalculateSupportBlock() {
        if (this.supportingBlock != null) {
            var thisBlock = this.supportingBlock.getSupportingBlock().getBlock();
            if (thisBlock == Blocks.WATER) {
                lastBlockSolid = false;
            } else if (thisBlock != Blocks.AIR) {
                lastBlockSolid = true;
            }
        }
        this.supportingBlock = StandingBlockUtil.getSupportingBlock(this.mcPlayer, lastBlockSolid);
    }

    // Helper method for getting the ResourceLocation for a given biome.
    private static ResourceLocation getBiomeResourceLocation(Level world, Biome biome) {
        Optional<Registry<Biome>> maybeBiomeRegistry = world.registryAccess().registries()
                .filter(entry -> entry.key().equals(Registries.BIOME))
                .map(entry -> (Registry<Biome>) entry.value())
                .findFirst();
        return maybeBiomeRegistry.map(registry -> registry.getKey(biome)).orElse(null);
    }
}