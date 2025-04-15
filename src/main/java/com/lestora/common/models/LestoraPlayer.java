package com.lestora.common.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.lestora.blocksupport.models.SupportBlock;
import com.lestora.common.data.PlayerRepo;
import com.lestora.debug.DebugOverlay;
import com.lestora.debug.models.DebugObject;
import com.lestora.debug.models.DebugSupplier;
import com.lestora.vanillatemp.VanillaTemp;
import com.lestora.vanillatemp.dependencies.BiomeConfigHandler;
import com.lestora.wetness.models.Wetness;
import com.lestora.wetness.models.WetnessUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

import java.util.Collections;

public class LestoraPlayer {
    //public static final Logger LOGGER = LogManager.getLogger("lestora");

    private Player mcPlayer;
    private UUID uuid;
    private float bodyTemp = 98f;

    // Timers in seconds
    private long lastSaveTime = 0;

    private static final Map<UUID, LestoraPlayer> players = new HashMap<>();

    static {
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

    private void calcNew(Player player) {
        this.swimLevel = PlayerRepo.getSwimLevel(this.uuid);
        calc(player);
    }

    public void calc(Player player) {
        this.level = Minecraft.getInstance().level;
        if (this.level == null || this.mcPlayer == null) return;
        this.mcPlayer = player;
        this.uuid = player.getUUID();

        SupportBlock.calculate(this.mcPlayer);
        WetnessUtil.calculate(this.mcPlayer);

        this.bodyTemp = VanillaTemp.CalculateBodyTemp(this.mcPlayer);

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

        var biome = level.getBiome(player.blockPosition()).value();
        float baseTemp = BiomeConfigHandler.getBiomeTemp(biome);

        Wetness playerWetness = WetnessUtil.getWetness(player);
        if (playerWetness == Wetness.FULLY_SUBMERGED || playerWetness == Wetness.NEARLY_SUBMERGED)
            drownDmg = switch (this.swimLevel) {
                case 0 -> (playerWetness == Wetness.FULLY_SUBMERGED ? 20 : 15);
                case 1 -> (playerWetness == Wetness.FULLY_SUBMERGED ? 15 : 10);
                case 2 -> (playerWetness == Wetness.FULLY_SUBMERGED ? 10 : 5);
                default -> 0;
            };

        var wetDiff = playerWetness.ordinal() * 15;
        if (this.bodyTemp <= 45){
            freezeDmg = ((45 - this.bodyTemp) + wetDiff) / 25;
            if (isOverworld) {
                freezeDmg *= 1 + VanillaTemp.coldTempOffset(this.level.getDayTime(), baseTemp);
            }
        }

        var heatStart = 125 + wetDiff;
        if (this.bodyTemp >= heatStart){
            heatDmg = (this.bodyTemp - heatStart) / 25;
            if (isOverworld) {
                heatDmg *= 1 + VanillaTemp.hotTempOffset(this.level.getDayTime(), baseTemp);
            }
        }

        if (freezeDmg > 0){
            Holder<DamageType> dmgTypeHolder = dmgRegistry.getOrThrow(DamageTypes.FREEZE);
            DamageSource coldDmgType = new DamageSource(dmgTypeHolder);
            player.hurt(coldDmgType, freezeDmg);
        }

        if (heatDmg > 0){
            Holder<DamageType> dmgTypeHolder = dmgRegistry.getOrThrow(DamageTypes.ON_FIRE);
            DamageSource hotDmgType = new DamageSource(dmgTypeHolder);
            player.hurt(hotDmgType, heatDmg);
            System.err.println("heatDmg: " + heatDmg);
        }

        if (drownDmg > 0) {
            Holder<DamageType> dmgTypeHolder = dmgRegistry.getOrThrow(DamageTypes.DROWN);
            DamageSource drowningSource = new DamageSource(dmgTypeHolder);
            player.hurt(drowningSource, drownDmg);
        }
    }

    private static int getWetnessOrdinal(Player player) {
        var playerWetness = WetnessUtil.getWetness(player);
        var thisWetness = playerWetness == Wetness.FULLY_SUBMERGED ? Wetness.NEARLY_SUBMERGED : playerWetness;
        return thisWetness.ordinal();
    }
}