package com.lestora.event;

import com.lestora.temperature.ConfigBiomeTemp;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = "lestora", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigBiomeTempEventHandler {
    public static Map<ResourceLocation, Float> biomeTempsMap = new HashMap<>();
    // Cache to avoid repeated lookups for the same biome instance.
    private static final Map<Biome, ResourceLocation> biomeRLCache = new HashMap<>();

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ConfigBiomeTemp.BIOME_CONFIG) {
            biomeTempsMap = ConfigBiomeTemp.getBiomeTempsMap();
        }
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ConfigBiomeTemp.BIOME_CONFIG) {
            biomeTempsMap = ConfigBiomeTemp.getBiomeTempsMap();
        }
    }

    public static float getBiomeTemp(Biome biome) {
        ResourceLocation rl = biomeRLCache.get(biome);
        if (rl == null) {
            // Attempt to lookup via the dynamic registry from the current client level.
            var mc = Minecraft.getInstance();
            if (mc.level != null) {
                Optional<Registry<Biome>> maybeBiomeRegistry = mc.level.registryAccess().registries()
                        .filter(entry -> entry.key().equals(Registries.BIOME))
                        .map(entry -> (Registry<Biome>) entry.value())
                        .findFirst();
                if (maybeBiomeRegistry.isPresent()) {
                    rl = maybeBiomeRegistry.get().getKey(biome);
                    if (rl != null) {
                        biomeRLCache.put(biome, rl);
                    }
                }
            }
        }
        // Look up the temperature from our config map, falling back to the biome's base temperature.
        Float temp = (rl != null) ? biomeTempsMap.get(rl) : null;
        if (temp == null) {
            temp = biome.getBaseTemperature();
        }
        return temp;
    }
}
