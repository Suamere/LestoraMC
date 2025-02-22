package com.lestora.mixin;

import com.lestora.event.ConfigEventHandler;
import com.lestora.util.TestLightConfig;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockLightEngine.class)
public abstract class LightTextureMixin {
    @Shadow
    private int getEmission(long pos, BlockState state) {
        return 0;
    }

    @Redirect(
            method = "checkNode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/lighting/BlockLightEngine;getEmission(JLnet/minecraft/world/level/block/state/BlockState;)I"
            )
    )
    private int overrideGetEmission(BlockLightEngine instance, long pos, BlockState state) {
        if (TestLightConfig.getEnabled()) {
            for (var currentPos : TestLightConfig.getCurrentPositions()) {
                if (pos == currentPos.position().asLong()) {
                    var lightLevel = ConfigEventHandler.getLightLevel(currentPos.resource());
                    if (lightLevel != null) { return lightLevel; }
                }
            }
        }
        return getEmission(pos, state);
    }
}