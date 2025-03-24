package com.lestora.common.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.lestora.blocksupport.models.SupportBlock;
import com.lestora.common.data.PlayerRepo;
import com.lestora.debug.DebugOverlay;
import com.lestora.debug.models.DebugObject;
import com.lestora.debug.models.DebugSupplier;
import com.lestora.event.ConfigBiomeTempEventHandler;
import com.lestora.wetness.models.Wetness;
import com.lestora.wetness.models.WetnessUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

import java.util.Collections;

public class LestoraPlayer {
    //public static final Logger LOGGER = LogManager.getLogger("lestora");

    private Player mcPlayer;
    private UUID uuid;
    private Biome biome;
    private float bodyTemp = 98f;

    // Timers in seconds
    private long lastUpdateTime = 0;
    private float transitionTimer = 0f;
    private long lastSaveTime = 0;

    // Cache of nearby block states (cube from -5 to 5 in x,y,z)
    private final Map<BlockPos, BlockState> cachedBlockStates = new HashMap<>();

    private static final Map<UUID, LestoraPlayer> players = new HashMap<>();

    static {
        var bodyTempSupplier = new DebugSupplier("Lestora_BodyTemp", 9, () -> {
            var lp = LestoraPlayer.get(Minecraft.getInstance().player);
            var bodyTemp = lp.getBodyTemp();
            var temp = String.valueOf(lp.getBodyTemp());
            int color = bodyTemp > 120 ? 16711680 : (bodyTemp < 60 ? 255 : 16776960);
            return new DebugObject("Temp", color, false, temp, color, false,
                              "Body Temp", color, true, temp, color, true);
        });
        DebugOverlay.registerDebugLine(bodyTempSupplier.getKey(), bodyTempSupplier);

        var swimLevelSupplier = new DebugSupplier("Lestora_Level_Swim", 8, () -> {
            var lp = LestoraPlayer.get(Minecraft.getInstance().player);
            var swim = String.valueOf(lp.getSwimLevel());
            var color = 15792383;
            return new DebugObject("Swim", color, false, swim, color, false,
                    "Swim Level", color, true, swim, color, true);
        });
        DebugOverlay.registerDebugLine(swimLevelSupplier.getKey(), swimLevelSupplier);
    }

    public static Map<UUID, LestoraPlayer> getPlayers() {
        return Collections.unmodifiableMap(players);
    }
    private ClientLevel level;
    public ClientLevel getLevel() {
        return level;
    }

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

    private LestoraVillager focusedVillager;
    public boolean TryFocusOnVillager(LestoraVillager villager) {
        if (villager == null) {
            UnfocusCurrentVillager();
            return false;
        }

        if (focusedVillager != null && !focusedVillager.getUUID().equals(villager.getUUID())) {
            UnfocusCurrentVillager();
        }

        focusedVillager = villager;
        villager.setNoAi(true);
        return true;
    }

    public void UnfocusCurrentVillager() {
        if (focusedVillager != null)
            focusedVillager.setNoAi(false);
        focusedVillager = null;
    }

    public UUID getUuid() {
        return uuid;
    }

    public float getBodyTemp() {
        return this.bodyTemp;
    }

    public Biome getBiome() {
        return biome;
    }

    public String getName() {
        return mcPlayer.getName().getString();
    }

    public int getSwimLevel() {
        return this.swimLevel;
    }

    public void setSwimLevel(int level) {
        this.swimLevel = level;
    }

    private void saveDBValues() {
        PlayerRepo.setSwimLevel(this.uuid, this.swimLevel);
    }

    private void calcNew(Player player) {
        this.swimLevel = PlayerRepo.getSwimLevel(this.uuid);
        calc(player);
    }

    public void calc(Player player) {
        this.level = Minecraft.getInstance().level;
        if (this.level == null || this.mcPlayer == null) return;
        this.mcPlayer = player;
        this.uuid = player.getUUID();

        CacheNearbyBlocks();
        SupportBlock.calculate(this.mcPlayer);
        WetnessUtil.calculate(this.mcPlayer);
        RubberBand(this.bodyTemp, CalculateBodyTemp());

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSaveTime >= 5000) {
            saveDBValues();
            lastSaveTime = currentTime;
        }
    }

    public void CalculateDamage(Player player) {
        var dmgRegistry = player.level().registryAccess().registries()
                .filter(entry -> entry.key().equals(Registries.DAMAGE_TYPE))
                .map(entry -> (Registry<DamageType>) entry.value())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Damage type registry not found"));

        var isOverworld = this.level.dimension().equals(Level.OVERWORLD);
        float freezeDmg = 0;
        float heatDmg = 0;
        int drownDmg = 0;
        float baseTemp = ConfigBiomeTempEventHandler.getBiomeTemp(this.biome);

        Wetness playerWetness = WetnessUtil.getWetness(player);
        if (playerWetness == Wetness.FULLY_SUBMERGED || playerWetness == Wetness.NEARLY_SUBMERGED)
            drownDmg = switch (this.swimLevel) {
                case 0 -> (playerWetness == Wetness.FULLY_SUBMERGED ? 20 : 15);
                case 1 -> (playerWetness == Wetness.FULLY_SUBMERGED ? 15 : 10);
                case 2 -> (playerWetness == Wetness.FULLY_SUBMERGED ? 10 : 5);
                default -> 0;
            };

        var wetDiff = playerWetness.ordinal() * 15;
        var time = daytimeTempOffset(this.level.getDayTime(), baseTemp);
        if (this.bodyTemp <= 45){
            freezeDmg = ((45 - this.bodyTemp) + wetDiff) / 25;
            if (isOverworld) {
                if (time < 0) {
                    freezeDmg *= 1 + (-time / maxDayDiff);
                }
            }
        }

        var heatStart = 125 + wetDiff;
        if (this.bodyTemp >= heatStart){
            heatDmg = (this.bodyTemp - heatStart) / 25;
            if (isOverworld) {
                if (time > 0) {
                    heatDmg *= 1 + (time / maxDayDiff);
                }
            }
        }

        if (freezeDmg > 0){
            Holder<DamageType> dmgTypeHolder = dmgRegistry.getOrThrow(DamageTypes.FREEZE);
            DamageSource drowningSource = new DamageSource(dmgTypeHolder);
            player.hurt(drowningSource, freezeDmg);
        }

        if (heatDmg > 0){
            Holder<DamageType> dmgTypeHolder = dmgRegistry.getOrThrow(DamageTypes.ON_FIRE);
            DamageSource drowningSource = new DamageSource(dmgTypeHolder);
            player.hurt(drowningSource, heatDmg);
            System.err.println("heatDmg: " + heatDmg);
        }

        if (drownDmg > 0) {
            Holder<DamageType> dmgTypeHolder = dmgRegistry.getOrThrow(DamageTypes.DROWN);
            DamageSource drowningSource = new DamageSource(dmgTypeHolder);
            player.hurt(drowningSource, drownDmg);
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

        var isOverworld = this.level.dimension().equals(Level.OVERWORLD);
        var coolBeanz = 0.5f;
        if (mcPlayer.getBlockY() < 45) {
            ResourceLocation biomeRL = getBiomeResourceLocation(this.level, this.biome);
            if (biomeRL == null ||
                    (!biomeRL.equals(ResourceLocation.parse("minecraft:dripstone_caves"))
                            && !biomeRL.equals(ResourceLocation.parse("minecraft:lush_caves"))
                            && !biomeRL.equals(ResourceLocation.parse("minecraft:deep_dark")))) {
                baseTemp = coolBeanz;
            }
        }

        if (isOverworld && baseTemp > coolBeanz){
            var light = level.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(playerPos);
            float skyLightTemp = light / 15f;
            baseTemp = ((baseTemp - coolBeanz) * skyLightTemp) + coolBeanz;
        }

        // Immediate check: if the player is directly in lava or fire.
        BlockState currentState = cachedBlockStates.get(playerPos);
        if (currentState != null) {
            if (currentState.getBlock() == Blocks.LAVA) {
                return 300;
            }
            if (currentState.getBlock() == Blocks.FIRE || currentState.getBlock() == Blocks.SOUL_FIRE) {
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

        var playerWetness = WetnessUtil.getWetness(mcPlayer);
        var thisWetness = playerWetness == Wetness.FULLY_SUBMERGED ? Wetness.NEARLY_SUBMERGED : playerWetness;
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

        var timeOffset = 0;
        if (isOverworld) {
            timeOffset = daytimeTempOffset(this.level.getDayTime(), baseTemp);
        }

        return Math.min((baseTemp * 25) - wetnessOffset - altitudeOffset + 60 + timeOffset + offset, 300);
    }

    private static float maxDayDiff = 15f;
    public static int daytimeTempOffset(long time, float environmentOffset) {
        long tickTime = time % 24000;
        float baseValue;

        // Compute the base value:
        // - 0 at sunrise (tick 0) and sunset (tick 12000)
        // - 15 at noon (tick 6000)
        // - -15 at midnight (tick 18000)
        if (tickTime < 6000) {
            // Sunrise (0) to noon (6000): increase from 0 to 15.
            baseValue = (maxDayDiff / 6000) * tickTime;
        } else if (tickTime < 12000) {
            // Noon (6000) to sunset (12000): decrease from 15 to 0.
            baseValue = maxDayDiff - (maxDayDiff / 6000) * (tickTime - 6000);
        } else if (tickTime < 18000) {
            // Sunset (12000) to midnight (18000): decrease from 0 to -15.
            baseValue = 0f - (maxDayDiff / 6000) * (tickTime - 12000);
        } else {
            // Midnight (18000) to next sunrise (24000): increase from -15 to 0.
            baseValue = -maxDayDiff + (maxDayDiff / 6000) * (tickTime - 18000);
        }

        // Apply the environmentOffset multipliers:
        float result = baseValue;
        if (environmentOffset < 0) {
            var absOffset = Math.abs(environmentOffset) + 1;
            if (baseValue < 0) {
                result = baseValue * absOffset;
            } else if (baseValue > 0) {
                result = baseValue * (absOffset / 2.0f);
            }
        } else if (environmentOffset > 0) {
            var absOffset = environmentOffset + 1;
            if (baseValue < 0) {
                result = baseValue * (absOffset / 2.0f);
            } else if (baseValue > 0) {
                result = baseValue * absOffset;
            }
        }

        return (int) result;
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
                            } else if (state.getBlock() == Blocks.FIRE || state.getBlock() == Blocks.SOUL_FIRE) {
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

    // Helper method for getting the ResourceLocation for a given biome.
    private static ResourceLocation getBiomeResourceLocation(Level world, Biome biome) {
        Optional<Registry<Biome>> maybeBiomeRegistry = world.registryAccess().registries()
                .filter(entry -> entry.key().equals(Registries.BIOME))
                .map(entry -> (Registry<Biome>) entry.value())
                .findFirst();
        return maybeBiomeRegistry.map(registry -> registry.getKey(biome)).orElse(null);
    }

    public void sendMsg(String msg) {
        if (mcPlayer instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal(msg));
        }
        else {
            mcPlayer.displayClientMessage(Component.literal(msg), false);
        }
    }

    public LestoraVillager getFocus() {
        return focusedVillager;
    }
}