package com.lestora.dummydebug.mixin;

import com.lestora.common.DebugOverlay;
import com.lestora.dummydebug.DummyDebugScreenOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class DisableDebugMixin {
    @Inject(method = "getDebugOverlay", at = @At("HEAD"), cancellable = true)
    private void disableDebugOverlay(CallbackInfoReturnable<DebugScreenOverlay> cir) {
        // Returning null prevents the debug overlay from being rendered
        if (DebugOverlay.getF3Disabled())
            cir.setReturnValue(new DummyDebugScreenOverlay((Minecraft)(Object)this));
    }
}