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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import java.util.Collections;

public class LestoraPlayer {
    private Player mcPlayer;
    private UUID uuid;
    private EntityBlockInfo supportingBlock;
    private Wetness wetness = Wetness.DRY;
    private Biome biome;
    private boolean lastBlockSolid;
    private float bodyTemp = 98f;

    // Timers in seconds
    private long lastUpdateTime = 0;
    private float transitionTimer = 0f;
    private long lastSaveTime = 0;

    // Cache of nearby block states (cube from -5 to 5 in x,y,z)
    private final Map<BlockPos, BlockState> cachedBlockStates = new HashMap<>();

    // In-memory static collection of players
    private static final Map<UUID, LestoraPlayer> players = new HashMap<>();

    public static Map<UUID, LestoraPlayer> getPlayers() {
        return Collections.unmodifiableMap(players);
    }
    private ClientLevel level;
    private int swimLevel;

    private LestoraPlayer(Player player) {
        this.mcPlayer = player;
        this.uuid = player.getUUID();
    }

    public static LestoraPlayer get(Player player) {
        return players.computeIfAbsent(player.getUUID(), key -> {
            var lestoraPlayer = new LestoraPlayer(player);
            lestoraPlayer.calcNew(player);
            return lestoraPlayer;
        });
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

    public int getSwimLevel() {
        return this.swimLevel;
    }

    public void setSwimLevel(int level) {
        this.swimLevel = level;
    }

    private void saveDBValues() {
        SQLiteManager.setSwimLevel(this.uuid, this.swimLevel);
    }

    private void calcNew(Player player) {
        this.swimLevel = SQLiteManager.getSwimLevel(this.uuid);
        calc(player);
    }

    public void calc(Player player) {
        this.level = Minecraft.getInstance().level;
        if (this.level == null || this.mcPlayer == null) return;
        this.mcPlayer = player;
        this.uuid = player.getUUID();

        CacheNearbyBlocks();
        CalculateSupportBlock();
        CalculateWetness();
        RubberBand(this.bodyTemp, CalculateBodyTemp());

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSaveTime >= 5000) {
            saveDBValues();
            lastSaveTime = currentTime;
        }
    }

    public void CalculateDamage(Player player) {
        if (this.wetness == Wetness.FULLY_SUBMERGED || this.wetness == Wetness.NEARLY_SUBMERGED) {
            if (this.swimLevel < 3) {
                Holder<DamageType> dmgTypeHolder = player.level().registryAccess().registries()
                        .filter(entry -> entry.key().equals(Registries.DAMAGE_TYPE))
                        .map(entry -> (Registry<DamageType>) entry.value())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Damage type registry not found"))
                        .getOrThrow(DamageTypes.DROWN);

                DamageSource drowningSource = new DamageSource(dmgTypeHolder);
                player.hurt(drowningSource, 5.0F);
                System.out.println("Bro... l2swim");
            }
        }
    }

    private void RubberBand(float current, float target) {
        float diff = target - current;
        float factor = 0.05f + 0.05f * (float)Math.tanh(Math.abs(diff) / 50.0f);
        float step = diff * factor;
        if (Math.abs(step) > Math.abs(diff)) this.bodyTemp = target;
        else this.bodyTemp = current + step;
    }

    // Cache a cube of block states around the player (from -5 to +5 in x, y, and z).
    private void CacheNearbyBlocks() {
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

    private float CalculateBodyTemp() {
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

        // Immediate check: if the player is directly in lava or fire.
        BlockState currentState = cachedBlockStates.get(playerPos);
        if (currentState != null) {
            if (currentState.getBlock() == Blocks.LAVA) {
                return 300;
            }
            if (currentState.getBlock() == Blocks.FIRE) {
                return 150;
            }
        }

        // Check if the player has any Lava Buckets in inventory.
        boolean hasLavaBucket = false;
        for (ItemStack stack : mcPlayer.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == Items.LAVA_BUCKET) {
                hasLavaBucket = true;
                break;
            }
        }
        boolean hasSnowBucket = false;
        for (ItemStack stack : mcPlayer.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == Items.POWDER_SNOW_BUCKET) {
                hasSnowBucket = true;
                break;
            }
        }

        var thisWetness = this.wetness == Wetness.FULLY_SUBMERGED ? Wetness.NEARLY_SUBMERGED : this.wetness;
        var wetnessOffset = Math.min((baseTemp - 3) * -4 * thisWetness.ordinal(), 50) + (mcPlayer.isInPowderSnow ? 20 : 0);
        float offset = 0f;

        if (!mcPlayer.isInWater()) {
            if (hasLavaBucket || mcPlayer.isOnFire()) {
                offset = 75f;
            } else {
                offset = getFireLavaOffset(playerPos);
            }
        }

        if (hasSnowBucket) {
            offset -= 25f;
        }

        int altitudeOffset = (mcPlayer.getBlockY() - 60) / 5;
        if (this.level.dimension().equals(Level.NETHER))
            altitudeOffset = mcPlayer.getBlockY() / -5;
        else if (this.level.dimension().equals(Level.END))
            altitudeOffset = 0;

        return Math.min((baseTemp * 25 - wetnessOffset - altitudeOffset) + 60 + offset, 300);
    }


    private float getFireLavaOffset(BlockPos playerPos) {
        float offset = 0f;
        // Loop over distances 1 to 5
        for (int d = 1; d <= 5; d++) {
            // Check in a cube of radius 'd' around the player.
            for (int x = -d; x <= d; x++) {
                for (int y = -d; y <= d; y++) {
                    for (int z = -d; z <= d; z++) {
                        BlockPos checkPos = playerPos.offset(x, y, z);
                        BlockState state = cachedBlockStates.get(checkPos);
                        if (state != null) {
                            if (state.getBlock() == Blocks.LAVA) {
                                float candidate = getLavaOffset(d);
                                if (candidate > offset) {
                                    offset = candidate;
                                }
                            } else if (state.getBlock() == Blocks.FIRE) {
                                float candidate = getFireOffset(d);
                                if (candidate > offset) {
                                    offset = candidate;
                                }
                            }
                        }
                    }
                }
            }
        }
        return offset;
    }

    private float getLavaOffset(int d) {
        switch (d) {
            case 1: return 75f;
            case 2: return 50f;
            case 3: return 30f;
            case 4: return 20f;
            case 5: return 10f;
            default: return 0f;
        }
    }

    private float getFireOffset(int d) {
        switch (d) {
            case 1: return 37.5f;
            case 2: return 25f;
            case 3: return 15f;
            case 4: return 10f;
            case 5: return 5f;
            default: return 0f;
        }
    }

    private void CalculateWetness() {
        long now = System.currentTimeMillis();
        if (lastUpdateTime == 0) {
            lastUpdateTime = now;
        }
        // Compute elapsed time in seconds.
        float delta = (now - lastUpdateTime) / 1000f;
        lastUpdateTime = now;

        // Calculate drynessTime based on bodyTemp.
        // For bodyTemp <= 100: interpolate from 30 sec at -100 to 5 sec at 100.
        // For bodyTemp > 100: cap at 1 sec.
        float clampedTemp = Math.max(-100f, Math.min(bodyTemp, 100f));
        float drynessTime = (bodyTemp > 100) ? 1f : 30f - 0.125f * (clampedTemp + 100f);

        // Accumulate time since last state transition.
        transitionTimer += delta;

        // Get the current "raw" wetness from your utility.
        var rawWetness = WetnessUtil.getPlayerWetness(this.mcPlayer, this.supportingBlock);

        // Immediate upgrade: if the raw wetness is wetter than our current state.
        if (rawWetness.ordinal() > this.wetness.ordinal()) {
            this.wetness = rawWetness;
            transitionTimer = 0;
            return;
        }

        // If we were FULLY_SUBMERGED but now have less wet raw value,
        // immediately drop to NEARLY_SUBMERGED.
        if (this.wetness == Wetness.FULLY_SUBMERGED &&
                rawWetness.ordinal() < Wetness.FULLY_SUBMERGED.ordinal()) {
            this.wetness = Wetness.NEARLY_SUBMERGED;
            transitionTimer = 0;
            return;
        }

        // If accumulated time exceeds drynessTime, downgrade one stage.
        if (transitionTimer >= drynessTime) {
            switch (this.wetness) {
                case NEARLY_SUBMERGED:
                    this.wetness = Wetness.SOAKED;
                    break;
                case SOAKED:
                    this.wetness = Wetness.DAMP;
                    break;
                case DAMP:
                    this.wetness = Wetness.DRY;
                    break;
                default:
                    // Already DRY, do nothing.
                    break;
            }
            transitionTimer = 0;
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