package com.lestora.mixin;

import com.lestora.HighlightConfig;
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
        System.out.println("[HighlightConfig] setBlocksDirty injection hit at " + pos);

        if (Minecraft.getInstance().player == null) return;
        UUID userId = Minecraft.getInstance().player.getUUID();
        HighlightConfig config = HighlightConfig.getUserHighlightConfig(userId);
        if (config == null || config.getHighlightRadius() <= 0) return;

        Level level = (Level)(Object)this;
        if (newState.is(Blocks.AIR)) {
            config.remove(pos, level);
        } else {
            config.add(pos, level);
        }
    }
}
