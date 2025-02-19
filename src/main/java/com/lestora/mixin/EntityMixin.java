package com.lestora.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isInWater", at = @At("TAIL"), cancellable = true)
    private void simulateUnderwater(CallbackInfoReturnable<Boolean> cir) {
        if (should_Simulate_Drowning()) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Define your custom condition here.
     * For testing, you might just return true.
     */
    @Unique
    private boolean should_Simulate_Drowning() {
        // Replace with your condition logic (e.g., key press, config flag, etc.)
        return false; // For demonstration, always simulate underwater.
    }
}
