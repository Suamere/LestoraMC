package com.lestora;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ParticleHighlighter {
    private static int tickCounter = 0;

    // Six cardinal directions
    private static final int[][] DIRECTIONS = {
            { 1,  0,  0}, // East
            {-1,  0,  0}, // West
            { 0,  1,  0}, // Up
            { 0, -1,  0}, // Down
            { 0,  0,  1}, // South
            { 0,  0, -1}  // North
    };

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Use command-set center if a radius is set; otherwise fallback to player position
        BlockPos center;
        if (HighlightConfig.getHighlightRadius() <= 0)
            return;

        center = new BlockPos(
                (int)Math.floor(HighlightConfig.getCenterX()),
                (int)Math.floor(HighlightConfig.getCenterY()),
                (int)Math.floor(HighlightConfig.getCenterZ())
        );

        // Run only every 20 ticks (~1 second)
        tickCounter++;
        if (tickCounter % 20 != 0) return;

        Level world = mc.level;
        double radius = HighlightConfig.getHighlightRadius();
        double radiusSq = radius * radius;

        for (int dx = -(int)radius; dx <= radius; dx++) {
            for (int dy = -(int)radius; dy <= radius; dy++) {
                for (int dz = -(int)radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (center.distSqr(pos) <= radiusSq) {
                        BlockState state = world.getBlockState(pos);
                        if (state.canOcclude()) {
                            // For each face/direction, check if the adjacent block does NOT occlude
                            for (int[] dir : DIRECTIONS) {
                                int offX = dir[0];
                                int offY = dir[1];
                                int offZ = dir[2];
                                BlockPos neighborPos = pos.offset(offX, offY, offZ);
                                BlockState neighborState = world.getBlockState(neighborPos);
                                if (!neighborState.canOcclude()) {
                                    // Determine the particle position: center of the face
                                    double particleX = pos.getX() + 0.5 + offX * 0.5;
                                    double particleY = pos.getY() + 0.5 + offY * 0.5;
                                    double particleZ = pos.getZ() + 0.5 + offZ * 0.5;
                                    DustParticleOptions dust = new DustParticleOptions(0xFF0000, 1.0F);
                                    world.addParticle(dust, particleX, particleY, particleZ, 0, 0, 0);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
