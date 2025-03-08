package com.lestora.highlight.mixin;

import com.lestora.highlight.HighlightMemory;
import com.lestora.highlight.HighlightSphere;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Inject(method = "setBlocksDirty", at = @At("HEAD"))
    private void onSetBlocksDirty(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) return;
        UUID userId = Minecraft.getInstance().player.getUUID();
        HighlightSphere config = HighlightSphere.getUserHighlightConfig(userId);
        if (config == null || !HighlightMemory.hasHighlights()) return;

        Level level = (Level)(Object)this;
        if (newState.is(Blocks.AIR)) {
            config.remove(pos, level);
        } else {
            config.add(pos, level);
        }
    }
}
