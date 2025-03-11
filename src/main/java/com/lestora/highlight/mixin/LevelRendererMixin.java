package com.lestora.highlight.mixin;

import com.lestora.highlight.HighlightEntry;
import com.lestora.highlight.HighlightFace;
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

        RenderPlz1(highlights, poseStack, mc);
        RenderPlz2(highlights, poseStack, mc);
    }

    private static void RenderPlz1(List<HighlightEntry> highlights, PoseStack poseStack, Minecraft mc) {
        var bufferSource = mc.renderBuffers().bufferSource();

        for (var highlightEntry : highlights) {
            if (highlightEntry.face != null) continue;
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
        float h = 0.5f;

        for (var entry : highlights) {
            if (entry.face == null) continue;
            BlockPos pos = entry.pos;

            // Retrieve color components from the entry, scaled to [0, 255].
            int red   = (int) (entry.color.getRed()   * 255);
            int green = (int) (entry.color.getGreen() * 255);
            int blue  = (int) (entry.color.getBlue()  * 255);
            int alpha = (int) (entry.color.getAlpha() * 255);

            float baseX = pos.getX();
            float baseY = pos.getY();
            float baseZ = pos.getZ();
            float[] arr = new float[9];
            // ToDo: Only UP and DOWN are probably right, lol
            switch (entry.face) {
                case UP:
                    baseY += 1;
                    arr = switch (entry.corner) {
                        case NORTH_EAST -> new float[]{0, 0, 0, 1, 0, 1, 1, 0, 0};
                        case SOUTH_EAST -> new float[]{0, 0, 1, 1, 0, 1, 1, 0, 0};
                        case SOUTH_WEST -> new float[]{0, 0, 0, 0, 0, 1, 1, 0, 1};
                        case NORTH_WEST -> new float[]{0, 0, 0, 0, 0, 1, 1, 0, 0};
                        case NORTH ->      new float[]{h, 0, h, 0, 0, 1, 1, 0, 1};
                        case SOUTH ->      new float[]{h, 0, h, 1, 0, 0, 0, 0, 0};
                        case EAST ->       new float[]{h, 0, h, 0, 0, 0, 0, 0, 1};
                        case WEST ->       new float[]{h, 0, h, 1, 0, 1, 1, 0, 0};
                        default -> arr;
                    };
                    break;
                case DOWN: baseY -= 1; break;
                case NORTH: baseX += 1; break;
                case SOUTH:
                    baseZ += 1;
                    arr = switch (entry.corner) {
                        case BOTTOM_WEST -> new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0};
                        case BOTTOM_EAST -> new float[]{0, 0, 0, 1, 0, 0, 1, 1, 0};
                        default -> arr;
                    };
                    break;
                case WEST: baseZ += 1; break;
                case EAST: baseZ -= 1; break;
            }
            buffer.addVertex(matrix, baseX + arr[0], baseY + arr[1], baseZ + arr[2]).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, baseX + arr[3], baseY + arr[4], baseZ + arr[5]).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, baseX + arr[6], baseY + arr[7], baseZ + arr[8]).setColor(red, green, blue, alpha);
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
