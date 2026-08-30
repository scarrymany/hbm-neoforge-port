package com.hbm.blocks.network.energy;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.energymk2.IEnergyConnectorMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.energy.CableBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ported from CE's {@code com.hbm.blocks.network.energy.BlockCable} (read in full): a plain 6-way
 * conductor with a dynamic connection-mask blockstate. {@link com.hbm.blocks.network.energy.BlockCableClassic}
 * is a trivial texture-only subclass (no logic override) and reuses everything here unchanged, exactly
 * matching CE.
 *
 * <p>Per the research report's "dynamic connection-mask block state" decision: CE's
 * {@code IExtendedBlockState}/{@code IUnlistedProperty<Boolean>} render-time mask (1.12 Forge-only,
 * confirmed no NeoForge equivalent) is replaced by six ordinary listed {@link BooleanProperty}s -
 * CE's own property names ({@code POS_X}/{@code NEG_X}/...) kept verbatim - recomputed in
 * {@link #updateShape}/{@link #getStateForPlacement} instead of at render time. This also directly
 * drives {@link #getShape}'s collision/selection box, matching CE's {@code AABB_BY_MASK} table
 * exactly (same pixel math, same 64-entry-by-mask shape).
 *
 * <p><b>Simplification vs. CE</b>: CE's {@code computeConnectToNeighbor} also falls back to a raw
 * Forge {@code IEnergyStorage} capability check on non-HBM neighbors (the FE-bridge visual, gated by
 * the same {@code autoCableConversion} config as the tile entity's FE half). That whole path is
 * dropped here along with the FE bridge itself (see {@link CableBaseBlockEntity}'s javadoc) - the
 * connection mask only reflects HBM's own {@link IEnergyConnectorMK2}/{@link IEnergyConnectorBlock}.
 */
public class BlockCable extends Block implements EntityBlock {

    public static final BooleanProperty POS_X = BooleanProperty.create("posx");
    public static final BooleanProperty NEG_X = BooleanProperty.create("negx");
    public static final BooleanProperty POS_Y = BooleanProperty.create("posy");
    public static final BooleanProperty NEG_Y = BooleanProperty.create("negy");
    public static final BooleanProperty POS_Z = BooleanProperty.create("posz");
    public static final BooleanProperty NEG_Z = BooleanProperty.create("negz");

    /** Lazily populated by {@link #boxForMask}, keyed by the 6-bit connection mask. */
    private static final VoxelShape[] SHAPE_BY_MASK = new VoxelShape[64];

    public BlockCable(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(POS_X, false).setValue(NEG_X, false)
                .setValue(POS_Y, false).setValue(NEG_Y, false)
                .setValue(POS_Z, false).setValue(NEG_Z, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POS_X, NEG_X, POS_Y, NEG_Y, POS_Z, NEG_Z);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBaseBlockEntity(EnergyNetworkBlockEntities.CABLE.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == EnergyNetworkBlockEntities.CABLE.get() ? ITickableBE.ticker() : null;
    }

    /**
     * Whether the block at {@code neighborPos} accepts a connection from {@code dirFromSelf}
     * (this block's own facing towards it) - checks the neighbor's block entity first
     * ({@link IEnergyConnectorMK2}, the normal case for any HBM conductor/receiver/provider), then
     * falls back to the neighbor block itself ({@link IEnergyConnectorBlock}, for plain blocks with
     * no qualifying block entity - CE's own "visual only" contract).
     */
    public static boolean canConnectToNeighbor(BlockGetter level, BlockPos neighborPos, Direction dirFromSelf) {
        BlockEntity be = level.getBlockEntity(neighborPos);
        if (be instanceof IEnergyConnectorMK2 con) {
            return con.canConnect(dirFromSelf.getOpposite());
        }
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof IEnergyConnectorBlock connBlock) {
            return connBlock.canConnect(level, neighborPos, dirFromSelf.getOpposite());
        }
        return false;
    }

    public static int computeConnectionMask(BlockGetter level, BlockPos pos) {
        int mask = 0;
        if (canConnectToNeighbor(level, pos.relative(Direction.EAST), Direction.EAST)) mask |= 1 << 0;
        if (canConnectToNeighbor(level, pos.relative(Direction.WEST), Direction.WEST)) mask |= 1 << 1;
        if (canConnectToNeighbor(level, pos.relative(Direction.UP), Direction.UP)) mask |= 1 << 2;
        if (canConnectToNeighbor(level, pos.relative(Direction.DOWN), Direction.DOWN)) mask |= 1 << 3;
        if (canConnectToNeighbor(level, pos.relative(Direction.SOUTH), Direction.SOUTH)) mask |= 1 << 4;
        if (canConnectToNeighbor(level, pos.relative(Direction.NORTH), Direction.NORTH)) mask |= 1 << 5;
        return mask;
    }

    public static BlockState withConnectionState(BlockState state, BlockGetter level, BlockPos pos) {
        if (!state.hasProperty(POS_X)) return state;
        return state
                .setValue(POS_X, canConnectToNeighbor(level, pos.relative(Direction.EAST), Direction.EAST))
                .setValue(NEG_X, canConnectToNeighbor(level, pos.relative(Direction.WEST), Direction.WEST))
                .setValue(POS_Y, canConnectToNeighbor(level, pos.relative(Direction.UP), Direction.UP))
                .setValue(NEG_Y, canConnectToNeighbor(level, pos.relative(Direction.DOWN), Direction.DOWN))
                .setValue(POS_Z, canConnectToNeighbor(level, pos.relative(Direction.SOUTH), Direction.SOUTH))
                .setValue(NEG_Z, canConnectToNeighbor(level, pos.relative(Direction.NORTH), Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnectionState(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return withConnectionState(state, level, pos);
    }

    private static int maskFromState(BlockState state) {
        int mask = 0;
        if (state.getValue(POS_X)) mask |= 1 << 0;
        if (state.getValue(NEG_X)) mask |= 1 << 1;
        if (state.getValue(POS_Y)) mask |= 1 << 2;
        if (state.getValue(NEG_Y)) mask |= 1 << 3;
        if (state.getValue(POS_Z)) mask |= 1 << 4;
        if (state.getValue(NEG_Z)) mask |= 1 << 5;
        return mask;
    }

    static VoxelShape boxForMask(int mask) {
        VoxelShape cached = SHAPE_BY_MASK[mask & 0x3F];
        if (cached != null) return cached;

        double pixel = 0.0625D;
        double min = pixel * 5.5D;
        double max = pixel * 10.5D;

        double minX = (mask & (1 << 1)) != 0 ? 0D : min;
        double maxX = (mask & (1 << 0)) != 0 ? 1D : max;
        double minY = (mask & (1 << 3)) != 0 ? 0D : min;
        double maxY = (mask & (1 << 2)) != 0 ? 1D : max;
        double minZ = (mask & (1 << 5)) != 0 ? 0D : min;
        double maxZ = (mask & (1 << 4)) != 0 ? 1D : max;

        VoxelShape shape = Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
        SHAPE_BY_MASK[mask & 0x3F] = shape;
        return shape;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return boxForMask(maskFromState(state));
    }
}
