package com.lestora.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class EntityBlockInfo {
    // BlockStates
    private final BlockState _supportingBlock;
    private final BlockState _belowSupport;
    private final BlockState _feetBlock;
    private final BlockState _headBlock;

    // Coordinates for the respective blocks.
    private final Coordinates _supportingPos;

    private EntityBlockInfo(BlockState supportingBlock, BlockState belowSupport,
                           BlockState feetBlock, BlockState headBlock,
                           Coordinates supportingPos) {
        this._supportingBlock = supportingBlock;
        this._belowSupport = belowSupport;
        this._feetBlock = feetBlock;
        this._headBlock = headBlock;
        this._supportingPos = supportingPos;
    }

    public static EntityBlockInfo fromSupport(BlockState supportState, BlockPos supportPos) {
        Level world = Minecraft.getInstance().level;
        var sptBlk = supportPos;
        var sptState = supportState;
        if (supportState.getBlock() == Blocks.WATER) {
            var headBlk = sptBlk.above();
            var headState = world.getBlockState(headBlk);
            var belowBlk = sptBlk.below();
            var belowState = world.getBlockState(belowBlk);
            return new EntityBlockInfo(sptState, belowState, sptState, headState, Coordinates.fromPos(sptBlk));
        }
        else if (sptState.getBlock() == Blocks.AIR) {
            var belowBlk = sptBlk.below();
            var belowState = world.getBlockState(belowBlk);
            if (belowState.getBlock() == Blocks.WATER) {
                var headBlk = sptBlk;
                var headState = sptState;
                sptBlk = belowBlk;
                sptState = belowState;
                belowBlk = sptBlk.below();
                belowState = world.getBlockState(belowBlk);
                return new EntityBlockInfo(sptState, belowState, sptState, headState, Coordinates.fromPos(sptBlk));
            }
            else {
                var headBlk = sptBlk.above();
                var headState = world.getBlockState(headBlk);
                return new EntityBlockInfo(sptState, belowState, sptState, headState, Coordinates.fromPos(sptBlk));
            }
        }

        var feetBlk = sptBlk.above();
        var feetState = world.getBlockState(feetBlk);
        var headBlk = feetBlk.above();
        var headState = world.getBlockState(headBlk);
        var belowBlk = sptBlk.below();
        var belowState = world.getBlockState(belowBlk);
        return new EntityBlockInfo(sptState, belowState, feetState, headState, Coordinates.fromPos(sptBlk));
    }

    public BlockState getSupportingBlock() {
        return _supportingBlock;
    }

    public BlockState getBelowSupport() {
        return _belowSupport;
    }

    public BlockState getFeetBlock() {
        return _feetBlock;
    }

    public BlockState getHeadBlock() {
        return _headBlock;
    }

    public Coordinates getSupportingPos() {
        return _supportingPos;
    }
}
