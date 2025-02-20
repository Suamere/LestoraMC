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
    private final Coordinates _belowPos;
    private final Coordinates _feetPos;
    private final Coordinates _headPos;

    private EntityBlockInfo(BlockState supportingBlock, BlockState belowSupport,
                           BlockState feetBlock, BlockState headBlock,
                           Coordinates supportingPos, Coordinates belowPos,
                           Coordinates feetPos, Coordinates headPos) {
        this._supportingBlock = supportingBlock;
        this._belowSupport = belowSupport;
        this._feetBlock = feetBlock;
        this._headBlock = headBlock;
        this._supportingPos = supportingPos;
        this._belowPos = belowPos;
        this._feetPos = feetPos;
        this._headPos = headPos;
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
            return new EntityBlockInfo(sptState, belowState, sptState, headState, Coordinates.fromPos(sptBlk), Coordinates.fromPos(belowBlk), Coordinates.fromPos(sptBlk), Coordinates.fromPos(headBlk));
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
                return new EntityBlockInfo(sptState, belowState, sptState, headState, Coordinates.fromPos(sptBlk), Coordinates.fromPos(belowBlk), Coordinates.fromPos(sptBlk), Coordinates.fromPos(headBlk));
            }
            else {
                var headBlk = sptBlk.above();
                var headState = world.getBlockState(headBlk);
                return new EntityBlockInfo(sptState, belowState, sptState, headState, Coordinates.fromPos(sptBlk), Coordinates.fromPos(belowBlk), Coordinates.fromPos(sptBlk), Coordinates.fromPos(headBlk));
            }
        }

        var feetBlk = sptBlk.above();
        var feetState = world.getBlockState(feetBlk);
        var headBlk = feetBlk.above();
        var headState = world.getBlockState(headBlk);
        var belowBlk = sptBlk.below();
        var belowState = world.getBlockState(belowBlk);
        return new EntityBlockInfo(sptState, belowState, feetState, headState, Coordinates.fromPos(sptBlk), Coordinates.fromPos(belowBlk), Coordinates.fromPos(feetBlk), Coordinates.fromPos(headBlk));
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

    public Coordinates getBelowPos() {
        return _belowPos;
    }

    public Coordinates getFeetPos() {
        return _feetPos;
    }

    public Coordinates getHeadPos() {
        return _headPos;
    }

    @Override
    public String toString() {
        return "EntityBlockInfo{" +
                "supportingBlock=" + _supportingBlock +
                ", belowSupport=" + _belowSupport +
                ", feetBlock=" + _feetBlock +
                ", headBlock=" + _headBlock +
                ", supportingPos=" + _supportingPos +
                ", belowPos=" + _belowPos +
                ", feetPos=" + _feetPos +
                ", headPos=" + _headPos +
                '}';
    }
}
