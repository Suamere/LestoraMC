package com.lestora.highlight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockWithFace {
    private final BlockPos neighborPos;
    private final BlockState neighborState;
    private final HighlightFace neighborFace;

    public BlockWithFace(BlockPos neighborPos, HighlightFace neighborFace, BlockState neighborState) {
        this.neighborPos = neighborPos;
        this.neighborState = neighborState;
        this.neighborFace = neighborFace;
    }

    public BlockPos getNeighborPos() {
        return neighborPos;
    }

    public BlockState getNeighborState() {
        return neighborState;
    }

    public HighlightFace getNeighborFace() {
        return neighborFace;
    }
}
