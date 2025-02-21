package com.lestora;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mod("lestora")
public class LestoraMod {
//    private static final Logger LOGGER = LogManager.getLogger();

    public LestoraMod(FMLJavaModLoadingContext constructContext) {
//        LoggerContext context = (LoggerContext) LogManager.getContext(false);
//        Configuration config = context.getConfiguration();
//        PatternLayout layout = PatternLayout.newBuilder()
//                .withPattern("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n")
//                .build();
//        FileAppender appender = FileAppender.newBuilder()
//                .withFileName("C:/Source/Java/log.txt")
//                .withAppend(false)
//                .withName("TestFileAppender")
//                .setLayout(layout)
//                .build();
//        appender.start();
//        config.getRootLogger().addAppender(appender, org.apache.logging.log4j.Level.INFO, null);
//        context.updateLoggers();

        constructContext.registerConfig(ModConfig.Type.COMMON, LestoraConfig.COMMON_CONFIG, "lestora-common.toml");
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    // Listen for block break events
//    @SubscribeEvent
//    public void onBlockBreak(BlockEvent.BreakEvent event) {
//        Level level = (Level) event.getLevel();
//
//        if (level.isClientSide()) {
//            return;
//        }
//
//        BlockPos pos = event.getPos();
//        BlockPos abovePos = pos.above();
//        BlockState stateAbove = level.getBlockState(abovePos);
//
//        if (!stateAbove.isAir() &&
//                stateAbove.getBlock() != Blocks.WATER &&
//                stateAbove.getBlock() != Blocks.LAVA) {
//
//            level.removeBlock(abovePos, false);
//            MyFallingBlockEntity fallingBlock = new MyFallingBlockEntity(
//                    level,
//                    abovePos.getX() + 0.5,
//                    abovePos.getY(),
//                    abovePos.getZ() + 0.5,
//                    stateAbove
//            );
//
//            level.addFreshEntity(fallingBlock);
//        }
//    }
}