package com.hbm.blocks.generic;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Metal fence/pane, ported from CE's {@code BlockMetalFence}. CE hand-rolled its own
 * pane-connection blockstate machinery ({@code BlockPane} plus a bespoke {@code PILLAR} computed
 * property for "does this need a center post"). Vanilla's {@link IronBarsBlock} already implements
 * exactly that mechanism (NORTH/EAST/SOUTH/WEST connection properties, connects to solid faces and
 * to other {@code IronBarsBlock} instances, and only shows a center post when no two opposite sides
 * are both connected) - so this class collapses to a thin {@code IronBarsBlock} subclass, matching
 * the precedent set by {@link BlockGenericSlab} for vanilla-shaped collapses. CE's separate
 * {@code FORCE_POST} metadata bit (force-render a post even on a straight run) survives as a real
 * {@link BooleanProperty} for datagen/model use; it carries no gameplay behavior of its own, exactly
 * as in CE.
 */
public class BlockMetalFence extends IronBarsBlock {

    public static final BooleanProperty FORCE_POST = BooleanProperty.create("force_post");

    public BlockMetalFence(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FORCE_POST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORCE_POST);
    }
}
