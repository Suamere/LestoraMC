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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class LestoraPlayer {
    private final Player mcPlayer;
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

    // In-memory static collection of players
    private static final Map<UUID, LestoraPlayer> players = new HashMap<>();

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

        Level world = Minecraft.getInstance().level;
        if (world != null && Minecraft.getInstance().player != null) {
            BlockPos pos = Minecraft.getInstance().player.blockPosition();
            Holder<Biome> biomeHolder = world.getBiome(pos);
            this.biome = biomeHolder.value();
            float baseTemp = ConfigBiomeTempEventHandler.getBiomeTemp(this.biome);

            // If player's altitude is below 60 and the biome is not one of the underground biomes,
            // then override baseTemp to 0.5.
            if (mcPlayer.getBlockY() < 60) {
                ResourceLocation biomeRL = getBiomeResourceLocation(world, this.biome);
                // Use ResourceLocation.parse to get our underground biome keys.
                if (biomeRL == null ||
                        (!biomeRL.equals(ResourceLocation.parse("minecraft:dripstone_caves"))
                                && !biomeRL.equals(ResourceLocation.parse("minecraft:lush_caves"))
                                && !biomeRL.equals(ResourceLocation.parse("minecraft:deep_dark")))) {
                    baseTemp = 0.5f;
                }
            }

            var thisWetness = this.wetness == Wetness.FULLY_SUBMERGED ? Wetness.NEARLY_SUBMERGED : this.wetness;
            var wetnessOffset = Math.min(17 * thisWetness.ordinal(), 50);
            Integer altitudeOffset = (mcPlayer.getBlockY() - 60) / 5;
            bodyTemp = (baseTemp * 25 - wetnessOffset - altitudeOffset) + 60;
        }
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