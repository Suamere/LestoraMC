package com.lestora.event;

import com.lestora.LestoraConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "lestora", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigEventHandler {
    public static Map<ResourceLocation, Integer> lightLevelsMap = new HashMap<>();

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == LestoraConfig.COMMON_CONFIG) {
            lightLevelsMap = LestoraConfig.getLightLevelsMap();
        }
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == LestoraConfig.COMMON_CONFIG) {
            lightLevelsMap = LestoraConfig.getLightLevelsMap();
        }
    }

    public static Integer getLightLevel(ResourceLocation rl) {
        return lightLevelsMap.get(rl);
    }
}