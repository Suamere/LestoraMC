package com.lestora.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "lestora", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeybindHandler {
    public static KeyMapping snapKey;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        snapKey = new KeyMapping("key.lestora.snap", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.lestora");
        event.register(snapKey);
    }
}