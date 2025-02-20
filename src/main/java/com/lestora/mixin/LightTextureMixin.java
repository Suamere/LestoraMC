package com.lestora.mixin;

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
        // Compare the long representation of the block's position with the test position.
        if (pos == TestLightConfig.getCurrentPos().asLong()) {
            return 14;
        }
        return getEmission(pos, state);
    }
}