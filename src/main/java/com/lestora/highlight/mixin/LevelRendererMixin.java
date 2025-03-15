package com.lestora.highlight.mixin;

import com.lestora.highlight.HighlightEntry;
import com.lestora.highlight.HighlightFace;
import com.lestora.highlight.HighlightMemory;
import com.lestora.highlight.PointLocation;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
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

        for (var entry : highlights) {
            if (entry.face == null) continue;
            BlockPos pos = entry.pos;
            // Retrieve color components from the entry, scaled to [0, 255].
            int red   = (int) (entry.color.getRed()   * 255);
            int green = (int) (entry.color.getGreen() * 255);
            int blue  = (int) (entry.color.getBlue()  * 255);
            int alpha = (int) (entry.color.getAlpha() * 255);

            float[] arr = switch(entry.corner) {
                // For non-diagonals, choose the adjacent points differently for EAST and WEST faces.
                case UP    -> Combine(entry.face, PointLocation.MiddleMiddle, PointLocation.BottomRight, PointLocation.BottomLeft);
                case DOWN  -> Combine(entry.face, PointLocation.MiddleMiddle, PointLocation.TopLeft, PointLocation.TopRight);
                case LEFT  -> {
                    if (entry.face == HighlightFace.EAST || entry.face == HighlightFace.WEST) {
                        yield Combine(entry.face, PointLocation.MiddleMiddle, PointLocation.TopLeft, PointLocation.BottomLeft);
                    } else {
                        yield Combine(entry.face, PointLocation.MiddleMiddle, PointLocation.TopRight, PointLocation.BottomRight);
                    }
                }
                case RIGHT -> {
                    if (entry.face == HighlightFace.EAST || entry.face == HighlightFace.WEST) {
                        yield Combine(entry.face, PointLocation.MiddleMiddle, PointLocation.TopRight, PointLocation.BottomRight);
                    } else {
                        yield Combine(entry.face, PointLocation.MiddleMiddle, PointLocation.TopLeft, PointLocation.BottomLeft);
                    }
                }
                // For corners, keep your existing logic.
                case TOP_LEFT     -> Combine(entry.face, PointLocation.TopLeft, PointLocation.BottomLeft, PointLocation.TopRight);
                case TOP_RIGHT    -> Combine(entry.face, PointLocation.TopRight, PointLocation.TopLeft, PointLocation.BottomRight);
                case BOTTOM_LEFT  -> Combine(entry.face, PointLocation.BottomLeft, PointLocation.TopLeft, PointLocation.BottomRight);
                case BOTTOM_RIGHT -> Combine(entry.face, PointLocation.BottomRight, PointLocation.BottomLeft, PointLocation.TopRight);
            };
            buffer.addVertex(matrix, pos.getX() + arr[0], pos.getY() + arr[1], pos.getZ() + arr[2]).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, pos.getX() + arr[3], pos.getY() + arr[4], pos.getZ() + arr[5]).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, pos.getX() + arr[6], pos.getY() + arr[7], pos.getZ() + arr[8]).setColor(red, green, blue, alpha);
        }

        mc.renderBuffers().bufferSource().endBatch(TRIANGLE);
    }

    private static float[] Combine(HighlightFace face, PointLocation p1, PointLocation p2, PointLocation p3) {
        var pt1 = getPoint(face, p1);
        var pt2 = getPoint(face, p2);
        var pt3 = getPoint(face, p3);
        float[] v1, v2, v3, expected;
        switch(face) {
            case UP:    v1 = new float[]{pt1[0],1,pt1[1]}; v2 = new float[]{pt2[0],1,pt2[1]}; v3 = new float[]{pt3[0],1,pt3[1]}; expected = new float[]{0,1,0}; break;
            case DOWN:  v1 = new float[]{pt1[0],0,pt1[1]}; v2 = new float[]{pt2[0],0,pt2[1]}; v3 = new float[]{pt3[0],0,pt3[1]}; expected = new float[]{0,-1,0}; break;
            case NORTH: v1 = new float[]{pt1[0],pt1[1],0}; v2 = new float[]{pt2[0],pt2[1],0}; v3 = new float[]{pt3[0],pt3[1],0}; expected = new float[]{0,0,-1}; break;
            case SOUTH: v1 = new float[]{pt1[0],pt1[1],1}; v2 = new float[]{pt2[0],pt2[1],1}; v3 = new float[]{pt3[0],pt3[1],1}; expected = new float[]{0,0,1}; break;
            case EAST:  v1 = new float[]{1,pt1[0],pt1[1]}; v2 = new float[]{1,pt2[0],pt2[1]}; v3 = new float[]{1,pt3[0],pt3[1]}; expected = new float[]{1,0,0}; break;
            case WEST:  v1 = new float[]{0,pt1[0],pt1[1]}; v2 = new float[]{0,pt2[0],pt2[1]}; v3 = new float[]{0,pt3[0],pt3[1]}; expected = new float[]{-1,0,0}; break;
            default: return new float[]{0,0,0,0,0,0,0,0,0};
        }
        // Compute triangle normal: cross(v2-v1, v3-v1)
        float[] a = new float[]{ v2[0]-v1[0], v2[1]-v1[1], v2[2]-v1[2] },
                b = new float[]{ v3[0]-v1[0], v3[1]-v1[1], v3[2]-v1[2] },
                n = new float[]{ a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0] };
        float dot = n[0]*expected[0] + n[1]*expected[1] + n[2]*expected[2];
        // If the computed normal is opposite of the expected one, swap pt2 and pt3.
        if(dot < 0) {
            float[] tmp = pt2; pt2 = pt3; pt3 = tmp;
            // Recompute v2 and v3 with swapped points.
            switch(face) {
                case UP:    v2 = new float[]{pt2[0],1,pt2[1]}; v3 = new float[]{pt3[0],1,pt3[1]}; break;
                case DOWN:  v2 = new float[]{pt2[0],0,pt2[1]}; v3 = new float[]{pt3[0],0,pt3[1]}; break;
                case NORTH: v2 = new float[]{pt2[0],pt2[1],0}; v3 = new float[]{pt3[0],pt3[1],0}; break;
                case SOUTH: v2 = new float[]{pt2[0],pt2[1],1}; v3 = new float[]{pt3[0],pt3[1],1}; break;
                case EAST:  v2 = new float[]{1,pt2[0],pt2[1]}; v3 = new float[]{1,pt3[0],pt3[1]}; break;
                case WEST:  v2 = new float[]{0,pt2[0],pt2[1]}; v3 = new float[]{0,pt3[0],pt3[1]}; break;
            }
        }
        return new float[]{ v1[0], v1[1], v1[2], v2[0], v2[1], v2[2], v3[0], v3[1], v3[2] };
    }

    private static float[] getPoint(HighlightFace face, PointLocation loc) {
        return switch(face) {
            case SOUTH -> switch(loc) {
                // (constant Z=1) 2D coords are (x, y)
                case TopLeft -> new float[]{0f, 1f}; case TopRight -> new float[]{1f, 1f};
                case BottomLeft -> new float[]{0f, 0f}; case BottomRight -> new float[]{1f, 0f};
                case TopMiddle -> new float[]{0.5f, 1f}; case LeftMiddle -> new float[]{0f, 0.5f};
                case BottomMiddle -> new float[]{0.5f, 0f}; case RightMiddle -> new float[]{1f, 0.5f};
                case MiddleMiddle -> new float[]{0.5f, 0.5f};
            };
            case NORTH -> switch(loc) {
                // (constant Z=0) 2D coords are (x, y), but x is mirrored
                case TopLeft -> new float[]{1f, 1f}; case TopRight -> new float[]{0f, 1f};
                case BottomLeft -> new float[]{1f, 0f}; case BottomRight -> new float[]{0f, 0f};
                case TopMiddle -> new float[]{0.5f, 1f}; case LeftMiddle -> new float[]{1f, 0.5f};
                case BottomMiddle -> new float[]{0.5f, 0f}; case RightMiddle -> new float[]{0f, 0.5f};
                case MiddleMiddle -> new float[]{0.5f, 0.5f};
            };
            case EAST -> switch(loc) {
                // (constant X=0), 2D coords are (y, z)
                case TopLeft -> new float[]{1f, 0f}; case TopRight -> new float[]{1f, 1f};
                case BottomLeft -> new float[]{0f, 0f}; case BottomRight -> new float[]{0f, 1f};
                case TopMiddle -> new float[]{1f, 0.5f}; case LeftMiddle -> new float[]{0.5f, 0f};
                case BottomMiddle -> new float[]{0f, 0.5f}; case RightMiddle -> new float[]{0.5f, 1f};
                case MiddleMiddle -> new float[]{0.5f, 0.5f};
            };
            case WEST -> switch(loc) {
                // (constant X=1) 2D coords are (y, z), but z is mirrored
                case TopLeft -> new float[]{1f, 1f}; case TopRight -> new float[]{1f, 0f};
                case BottomLeft -> new float[]{0f, 1f}; case BottomRight -> new float[]{0f, 0f};
                case TopMiddle -> new float[]{1f, 0.5f}; case LeftMiddle -> new float[]{0.5f, 1f};
                case BottomMiddle -> new float[]{0f, 0.5f}; case RightMiddle -> new float[]{0.5f, 0f};
                case MiddleMiddle -> new float[]{0.5f, 0.5f};
            };
            case UP -> switch(loc) {
                // (constant Y=1), 2D coords are (x, z)
                case TopLeft -> new float[]{0f, 0f}; case TopRight -> new float[]{1f, 0f};
                case BottomLeft -> new float[]{0f, 1f}; case BottomRight -> new float[]{1f, 1f};
                case TopMiddle -> new float[]{0.5f, 0f}; case LeftMiddle -> new float[]{0f, 0.5f};
                case BottomMiddle -> new float[]{0.5f, 1f}; case RightMiddle -> new float[]{1f, 0.5f};
                case MiddleMiddle -> new float[]{0.5f, 0.5f};
            };
            case DOWN -> switch(loc) {
                // (constant Y=0), 2D coords are (x, z), but z is mirrored.
                case TopLeft -> new float[]{0f, 1f}; case TopRight -> new float[]{1f, 1f};
                case BottomLeft -> new float[]{0f, 0f}; case BottomRight -> new float[]{1f, 0f};
                case TopMiddle -> new float[]{0.5f, 1f}; case LeftMiddle -> new float[]{0f, 0.5f};
                case BottomMiddle -> new float[]{0.5f, 0f}; case RightMiddle -> new float[]{1f, 0.5f};
                case MiddleMiddle -> new float[]{0.5f, 0.5f};
            };
        };
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
