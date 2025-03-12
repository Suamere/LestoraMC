package com.lestora.highlight;

import net.minecraft.core.BlockPos;


public class HighlightEntry {
    public final BlockPos pos;
    public final HighlightColor color;
    public final HighlightFace face;
    public final HighlightCorner corner;

    public HighlightEntry(BlockPos pos, HighlightColor color, HighlightFace face, HighlightCorner corner) {
        this.pos = pos;
        this.color = color;
        this.face = face;
        this.corner = corner;
    }

    private HighlightEntry(BlockPos pos, HighlightColor color) {
        this.pos = pos;
        this.color = color;
        this.face = null;
        this.corner = null;
    }

    public static HighlightEntry Whole(BlockPos neighborPos, HighlightColor red) {
        return new HighlightEntry(neighborPos, red);
    }
}