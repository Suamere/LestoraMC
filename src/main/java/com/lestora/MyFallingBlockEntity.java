//package com.lestora;
//
//import net.minecraft.world.entity.item.FallingBlockEntity;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.state.BlockState;
//import java.lang.reflect.Field;
//
//public class MyFallingBlockEntity extends FallingBlockEntity {
//    public MyFallingBlockEntity(Level level, double x, double y, double z, BlockState state) {
//        super(EntityType.FALLING_BLOCK, level);
//        this.moveTo(x, y, z);
//        setBlockStateReflectively(state);
//    }
//
//    private void setBlockStateReflectively(BlockState state) {
//        try {
//            Field blockStateField = FallingBlockEntity.class.getDeclaredField("blockState");
//            blockStateField.setAccessible(true);
//            blockStateField.set(this, state);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
