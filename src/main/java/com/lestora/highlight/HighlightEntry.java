package com.lestora.highlight;

import net.minecraft.core.BlockPos;

public class HighlightEntry {
    public final BlockPos pos;
    public final HighlightColor color;
    public HighlightEntry(BlockPos pos, HighlightColor color) {
        this.pos = pos;
        this.color = color;
    }
}