package com.lestora;

import com.lestora.AI.AIRequestThread;
import com.lestora.common.data.SQLiteManager;
import com.lestora.dynamiclighting.ConfigLighting;
import com.lestora.temperature.ConfigBiomeTemp;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("lestora")
public class LestoraMod {
    //public static final Logger LOGGER = LogManager.getLogger("lestora");

    public LestoraMod(FMLJavaModLoadingContext constructContext) {
        System.err.println("Mistral WUT!?");
        SQLiteManager.init();
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

        constructContext.registerConfig(ModConfig.Type.COMMON, ConfigLighting.LIGHTING_CONFIG, "lestora-lighting.toml");
        constructContext.registerConfig(ModConfig.Type.COMMON, ConfigBiomeTemp.BIOME_CONFIG, "lestora-biome.toml");
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
        AIRequestThread.startBackgroundProcessing();
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