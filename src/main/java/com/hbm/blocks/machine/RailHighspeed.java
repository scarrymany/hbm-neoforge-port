package com.hbm.blocks.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * CE {@code RailHighspeed} ({@code ModBlocks.java}:836) — straight powered-rail shape, max speed
 * {@code 1.0F} ({@code RailHighspeed.java}:40-42). Diagonals rejected the same way CE filtered
 * {@code NORTH_EAST}/{@code NORTH_WEST}/{@code SOUTH_EAST}/{@code SOUTH_WEST}.
 */
public class RailHighspeed extends BaseRailBlock {

    public static final MapCodec<RailHighspeed> CODEC = simpleCodec(RailHighspeed::new);
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;

    public RailHighspeed(Properties properties) {
        super(true, properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(SHAPE, RailShape.NORTH_SOUTH)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseRailBlock> codec() {
        return CODEC;
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, WATERLOGGED);
    }

    @Override
    public float getRailMaxSpeed(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        return 1.0F;
    }

    @Override
    public boolean isValidRailShape(RailShape shape) {
        return shape != RailShape.NORTH_EAST
                && shape != RailShape.NORTH_WEST
                && shape != RailShape.SOUTH_EAST
                && shape != RailShape.SOUTH_WEST;
    }
}
