package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Construction scaffold, ported from CE's {@code BlockScaffold}. CE's {@code VARIANT} (steel/red/
 * white/yellow) was an {@code IBlockMulti} item-metadata pick rather than placement-derived state -
 * per the flattening rule each variant becomes its own registry entry here, selected once at
 * construction via {@link Variant}, mirroring the fixed-per-instance {@code Kind}/type fields
 * already used for {@link BlockRailing} and {@link BlockWoodStructure}. {@code ORIENT} genuinely is
 * placement-derived (which way the scaffold pole runs) and survives as a real
 * {@link EnumProperty}. The custom {@code .obj}-modeled baked model
 * ({@code BlockScaffoldBakedModel} over an {@code HFRWavefrontObject}) has no ported NeoForge
 * geometry-loader equivalent yet - flagged in the port report rather than guessed at; this class's
 * placement/shape/collision logic is fully faithful regardless.
 */
public class BlockScaffold extends Block {

    public enum Variant { STEEL, RED, WHITE, YELLOW }

    public enum Orient implements StringRepresentable {
        HORIZONTAL_NS("horizontal_north_south"),
        HORIZONTAL_EW("horizontal_east_west"),
        VERTICAL_NS("vertical_north_south"),
        VERTICAL_EW("vertical_east_west");

        private final String id;

        Orient(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public static final EnumProperty<Orient> ORIENT = EnumProperty.create("orient", Orient.class);

    private static final float MARGIN = 2.0F;

    private final Variant variant;

    public BlockScaffold(Properties properties, Variant variant) {
        super(properties);
        this.variant = variant;
        this.registerDefaultState(this.stateDefinition.any().setValue(ORIENT, Orient.HORIZONTAL_NS));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIENT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        Orient orient;

        if (facing.getAxis().isVertical()) {
            Direction playerFacing = context.getHorizontalDirection();
            orient = playerFacing.getAxis() == Direction.Axis.Z ? Orient.HORIZONTAL_NS : Orient.HORIZONTAL_EW;
        } else if (facing.getAxis() == Direction.Axis.Z) {
            orient = Orient.VERTICAL_NS;
        } else {
            orient = Orient.VERTICAL_EW;
        }

        return this.defaultBlockState().setValue(ORIENT, orient);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(ORIENT)) {
            case HORIZONTAL_EW -> Block.box(MARGIN, 0, 0, 16 - MARGIN, 16, 16);
            case HORIZONTAL_NS -> Block.box(0, 0, MARGIN, 16, 16, 16 - MARGIN);
            default -> Block.box(0, MARGIN, 0, 16, 16 - MARGIN, 16);
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    public Variant getVariant() {
        return variant;
    }
}
