package com.lestora;

import com.lestora.AI.AIRequestThread;
import com.lestora.common.data.SQLiteManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("lestora")
public class LestoraMod {
    public LestoraMod(FMLJavaModLoadingContext constructContext) {
        SQLiteManager.init();
        AIRequestThread.startBackgroundProcessing();
    }
}