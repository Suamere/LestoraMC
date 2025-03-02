package com.lestora.mixin;

import com.lestora.highlight.HighlightSphere;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import java.util.UUID;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderLevel(
            com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator,
            net.minecraft.client.DeltaTracker deltaTracker,
            boolean someFlag,
            Camera camera,
            GameRenderer gameRenderer,
            Matrix4f matrix1,
            Matrix4f matrix2,
            CallbackInfo ci
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.player == null) return;

        // If no highlight radius is set, do nothing.
        UUID userId = mc.player.getUUID();
        HighlightSphere config = HighlightSphere.getUserHighlightConfig(userId);
        if (config == null || !config.hasHighlights()) return;

        // Get precomputed positions
        List<BlockPos> positions = config.getHighlightedPositions();
        if (positions.isEmpty()) return;

        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(matrix1);

        // Translate relative to the camera
        poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);

        for (BlockPos pos : positions) {
            AABB box = new AABB(pos).inflate(0.002);
            DebugRenderer.renderFilledBox(poseStack, mc.renderBuffers().bufferSource(), box, 1.0F, 0.0F, 0.0F, 0.5F);
        }
        mc.renderBuffers().bufferSource().endBatch(net.minecraft.client.renderer.RenderType.debugFilledBox());
    }
}
