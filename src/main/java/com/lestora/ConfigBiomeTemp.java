package com.lestora;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigBiomeTemp {
    public static final ForgeConfigSpec BIOME_CONFIG;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BIOME_TEMPS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("biome_temps");
        BIOME_TEMPS = builder.comment("List of biome=temperature pairs. Temperature must be in range -0.7 to 2.0")
                .defineList("biomes",
                        Arrays.asList(
                                "minecraft:end_barrens=-1.0",
                                "minecraft:end_midlands=-1.0",
                                "minecraft:the_end=-1.0",
                                "minecraft:small_end_islands=-1.0",
                                "minecraft:end_highlands=0.0",
                                "minecraft:the_void=-1.0",
                                "minecraft:ice_spikes=-1.0",
                                "minecraft:frozen_peaks=-1.0",
                                "minecraft:deep_frozen_ocean=-1.0",
                                "minecraft:frozen_ocean=-0.8",
                                "minecraft:jagged_peaks=-0.7",
                                "minecraft:snowy_taiga=-0.7",
                                "minecraft:frozen_river=-0.5",
                                "minecraft:snowy_slopes=-0.7",
                                "minecraft:snowy_plains=-0.8",
                                "minecraft:deep_cold_ocean=-0.8",
                                "minecraft:snowy_beach=-0.25",
                                "minecraft:grove=-0.2",
                                "minecraft:cold_ocean=-0.2",
                                "minecraft:old_growth_spruce_taiga=0.25",
                                "minecraft:taiga=0.25",
                                "minecraft:old_growth_pine_taiga=0.3",
                                "minecraft:lush_caves=0.4",
                                "minecraft:dark_forest=0.4",
                                "minecraft:pale_garden=0.4",
                                "minecraft:cherry_grove=0.5",
                                "minecraft:deep_ocean=0.3",
                                "minecraft:meadow=0.5",
                                "minecraft:ocean=0.5",
                                "minecraft:river=0.5",
                                "minecraft:warm_ocean=0.7",
                                "minecraft:birch_forest=0.6",
                                "minecraft:old_growth_birch_forest=0.6",
                                "minecraft:deep_lukewarm_ocean=0.7",
                                "minecraft:lukewarm_ocean=0.6",
                                "minecraft:flower_forest=0.7",
                                "minecraft:forest=0.7",
                                "minecraft:beach=0.8",
                                "minecraft:dripstone_caves=0.4",
                                "minecraft:mangrove_swamp=0.9",
                                "minecraft:plains=0.8",
                                "minecraft:sunflower_plains=0.8",
                                "minecraft:mushroom_fields=0.9",
                                "minecraft:stony_peaks=1.0",
                                "minecraft:deep_dark=0.8",
                                "minecraft:bamboo_jungle=1.1",
                                "minecraft:sparse_jungle=1.1",
                                "minecraft:swamp=1.0",
                                "minecraft:jungle=1.2",
                                "minecraft:eroded_badlands=1.5",
                                "minecraft:savanna=1.5",
                                "minecraft:savanna_plateau=1.5",
                                "minecraft:windswept_savanna=1.5",
                                "minecraft:wooded_badlands=1.5",
                                "minecraft:badlands=1.8",
                                "minecraft:desert=1.8",
                                "minecraft:basalt_deltas=3.0",
                                "minecraft:crimson_forest=1.8",
                                "minecraft:nether_wastes=2.0",
                                "minecraft:soul_sand_valley=-1.0", // Odd choice, but let's make it cold in a Soul Sand Valley?  Lol
                                "minecraft:warped_forest=1.8"
                        ),
                        o -> o instanceof String && ((String) o).contains("="));
        builder.pop();
        BIOME_CONFIG = builder.build();
    }

    public static Map<ResourceLocation, Float> getBiomeTempsMap() {
        Map<ResourceLocation, Float> map = new HashMap<>();
        for (String entry : BIOME_TEMPS.get()) {
            String[] split = entry.split("=");
            if (split.length == 2) {
                String biomeId = split[0].trim();
                try {
                    float temp = Float.parseFloat(split[1].trim());
                    if (temp < -1.0f || temp > 3.0f) {
                        System.err.println("Temperature out of range for biome " + biomeId + ": " + temp);
                        continue;
                    }
                    ResourceLocation loc = ResourceLocation.tryParse(biomeId);
                    if (loc != null) {
                        map.put(loc, temp);
                    } else {
                        System.err.println("Invalid biome id in config: " + biomeId);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid temperature number in config for " + biomeId + ": " + split[1]);
                }
            }
        }
        return map;
    }
}
