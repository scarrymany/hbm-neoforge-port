package com.hbm.blocks.generic;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Decorative CRT monitor prop, ported from CE's {@code BlockDecoCRT} (via {@code BlockDecoModel}).
 * CE packs a 4-value content variant (clean/broken/blinking/bsod) and a 4-value placement rotation
 * into one metadata int; the port flattens the content variant into its own registry entry per the
 * metadata-flattening ground rule (one {@code BlockDecoCRT} instance per {@link Variant}) and keeps
 * rotation as a real {@code FACING} block-state property, matching the convention already
 * established for {@code BlockRedBrick}/{@code DecoBlock}.
 * <p>
 * CE's bespoke {@code .obj} model ({@code HFRWavefrontObject} via {@code BlockDecoBakedModel}) has
 * no confirmed NeoForge 1.21 geometry-loader equivalent; per the port instructions this registers
 * with a plain default model instead of guessing an API - a documented rendering gap.
 */
public class BlockDecoCRT extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public enum Variant { CLEAN, BROKEN, BLINKING, BSOD }

    private final Variant variant;

    public BlockDecoCRT(Properties properties, Variant variant) {
        super(properties);
        this.variant = variant;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Variant getVariant() {
        return variant;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
