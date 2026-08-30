package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Sandbag wall/pile, ported from CE's {@code BlockSandbags}. CE's per-side "does this face connect
 * to a neighboring sandbag/solid block" flags lived on a Forge {@code IUnlistedProperty}
 * ({@code ExtendedBlockState}), a mechanism NeoForge 1.21 does not carry forward - per the port
 * report's explicit direction, this is reworked as four ordinary {@link BooleanProperty}
 * blockstate properties, recomputed on placement and on neighbor change exactly like vanilla's own
 * connected-block families ({@code IronBarsBlock}, fences) already do.
 */
public class BlockSandbags extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CONN_NEG_X = BooleanProperty.create("conn_neg_x");
    public static final BooleanProperty CONN_POS_X = BooleanProperty.create("conn_pos_x");
    public static final BooleanProperty CONN_NEG_Z = BooleanProperty.create("conn_neg_z");
    public static final BooleanProperty CONN_POS_Z = BooleanProperty.create("conn_pos_z");

    private static final float MIN = 4.0F;
    private static final float MAX = 12.0F;

    public BlockSandbags(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(CONN_NEG_X, false)
                .setValue(CONN_POS_X, false)
                .setValue(CONN_NEG_Z, false)
                .setValue(CONN_POS_Z, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONN_NEG_X, CONN_POS_X, CONN_NEG_Z, CONN_POS_Z);
    }

    private static boolean connects(BlockGetter level, BlockPos neighborPos, Block self) {
        BlockState neighbor = level.getBlockState(neighborPos);
        return neighbor.is(self) || neighbor.isFaceSturdy(level, neighborPos, Direction.UP);
    }

    private BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        boolean negX = connects(level, pos.west(), this);
        boolean posX = connects(level, pos.east(), this);
        boolean negZ = connects(level, pos.north(), this);
        boolean posZ = connects(level, pos.south(), this);
        return state.setValue(CONN_NEG_X, negX).setValue(CONN_POS_X, posX).setValue(CONN_NEG_Z, negZ).setValue(CONN_POS_Z, posZ);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return withConnections(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction.getAxis().isHorizontal() ? withConnections(state, level, pos) : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        float minX = state.getValue(CONN_NEG_X) ? 0.0F : MIN;
        float minZ = state.getValue(CONN_NEG_Z) ? 0.0F : MIN;
        float maxX = state.getValue(CONN_POS_X) ? 16.0F : MAX;
        float maxZ = state.getValue(CONN_POS_Z) ? 16.0F : MAX;
        return Block.box(minX, 0, minZ, maxX, 16, maxZ);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}
