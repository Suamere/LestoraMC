package com.lestora;

import com.lestora.HighlightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ParticleHighlighter {
    private static int tickCounter = 0;

//    @SubscribeEvent
//    public static void onClientTick(TickEvent.ClientTickEvent event) {
//        Minecraft mc = Minecraft.getInstance();
//        if (mc.level == null) return;
//
//        // Use command-set center if a radius is set; otherwise fallback to player position
//        BlockPos center;
//        if (HighlightConfig.getHighlightRadius() > 0) {
//            center = new BlockPos((int)HighlightConfig.getCenterX(), (int)HighlightConfig.getCenterY(), (int)HighlightConfig.getCenterZ());
//        } else if (mc.player != null) {
//            center = mc.player.blockPosition();
//        } else {
//            return;
//        }
//
//        // Run only every 20 ticks (~1 second)
//        tickCounter++;
//        if (tickCounter % 20 != 0) return;
//
//        Level world = mc.level;
//        double radius = HighlightConfig.getHighlightRadius();
//        double radiusSq = radius * radius;
//
//        for (int dx = -(int)radius; dx <= radius; dx++) {
//            for (int dy = -(int)radius; dy <= radius; dy++) {
//                for (int dz = -(int)radius; dz <= radius; dz++) {
//                    BlockPos pos = center.offset(dx, dy, dz);
//                    if (center.distSqr(pos) <= radiusSq) {
//                        BlockState state = world.getBlockState(pos);
//                        if (state.canOcclude()) {
//                            // Emit particle slightly offset from the block's top surface
//                            double x = pos.getX() + 0.5;
//                            double y = pos.getY() + 1;
//                            double z = pos.getZ() + 0.5;
//                            world.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
//                        }
//                    }
//                }
//            }
//        }
//    }
}
