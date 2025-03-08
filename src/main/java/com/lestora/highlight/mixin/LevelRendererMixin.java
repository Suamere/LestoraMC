package com.lestora.highlight.mixin;

import com.lestora.highlight.HighlightEntry;
import com.lestora.highlight.HighlightMemory;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

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

        // Get precomputed positions
        var highlights = HighlightMemory.getHighlightedPositions();
        if (highlights.isEmpty()) return;

        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(matrix1);

        // Translate relative to the camera
        poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);

        RenderPlz2(highlights, poseStack, mc);
    }

    private static void RenderPlz1(List<HighlightEntry> highlights, PoseStack poseStack, Minecraft mc) {
        var bufferSource = mc.renderBuffers().bufferSource();

        for (var highlightEntry : highlights) {
            AABB box = new AABB(highlightEntry.pos).inflate(0.002);

            var red   = highlightEntry.color.getRed();
            var green = highlightEntry.color.getGreen();
            var blue  = highlightEntry.color.getBlue();
            var alpha = highlightEntry.color.getAlpha();

            DebugRenderer.renderFilledBox(poseStack, bufferSource, box, red, green, blue, alpha);
        }
        mc.renderBuffers().bufferSource().endBatch(RenderType.debugFilledBox());
    }

    private static void RenderPlz2(List<HighlightEntry> highlights, PoseStack poseStack, Minecraft mc) {
        var buffer = mc.renderBuffers().bufferSource().getBuffer(TRIANGLE);
        Matrix4f matrix = poseStack.last().pose();

        for (var entry : highlights) {
            BlockPos pos = entry.pos;

            float baseX = pos.getX();
            float baseY = pos.getY() + 1;
            float baseZ = pos.getZ();

            // Retrieve color components from the entry, scaled to [0, 255].
            int red   = (int) (entry.color.getRed()   * 255);
            int green = (int) (entry.color.getGreen() * 255);
            int blue  = (int) (entry.color.getBlue()  * 255);
            int alpha = (int) (entry.color.getAlpha() * 255);

            buffer.addVertex(matrix, baseX,     baseY, baseZ).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, baseX + 1, baseY, baseZ + 1).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, baseX + 1, baseY, baseZ).setColor(red, green, blue, alpha);
        }

        mc.renderBuffers().bufferSource().endBatch(TRIANGLE);
    }

    private static RenderStateShard.ShaderStateShard POSITION_COLOR_SHADER = new RenderStateShard.ShaderStateShard(CoreShaders.POSITION_COLOR);
    private static RenderStateShard.LayeringStateShard VIEW_OFFSET_Z_LAYERING = new RenderStateShard.LayeringStateShard("view_offset_z_layering", () -> {
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        RenderSystem.getProjectionType().applyLayeringTransform(matrix4fstack, 1.0F);
    }, () -> {
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.popMatrix();
    });
    private static RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
            "translucent_transparency",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(
                        GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
                );
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
    );

    private static final RenderType TRIANGLE = RenderType.create(
            "custom_triangle",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            256,  // Buffer size (adjust as needed)
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .createCompositeState(false)
    );
}
