package com.hbm.blocks.generic;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Decorative pole cap, ported from CE's {@code DecoPoleTop}. No tile entity, no other coupling.
 */
public class DecoPoleTop extends Block {

    public DecoPoleTop(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
