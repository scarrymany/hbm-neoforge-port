package com.hbm.blocks.network.energy;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.energy.CableBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.blocks.network.energy.PowerCableBox} (read in full): a boxier
 * conductor variant sharing {@link CableBaseBlockEntity} with {@link BlockCable} outright (CE's own
 * {@code createNewTileEntity}/{@code createTileEntity} both return {@code TileEntityCableBaseNT}) and
 * the identical connection-mask logic (CE's own comment: "duplicated {@code resolveMask}" against
 * {@code BlockCable}) - reused here via {@link BlockCable#withConnectionState}/
 * {@link BlockCable#canConnectToNeighbor} instead of a second copy.
 *
 * <p><b>Simplification vs. CE</b>: CE ships this as five separate creative-tab sub-items (a 0-4
 * {@code PropertyInteger} thickness selected by item damage value, each with its own bounding-box/
 * collision math) purely as a cosmetic model variant - conductivity is identical at every thickness.
 * The {@link #THICKNESS} state property is kept (so a future model/datagen pass can wire the
 * remaining four sub-variants without touching this class's shape or connection logic again), but
 * only the single default ({@code THICKNESS = 0}, CE's thinnest/most permissive box) is registered as
 * an item for now - porting CE's damage-value sub-item enumeration is a creative-tab/model concern,
 * out of this network-graph pass's scope.
 */
public class PowerCableBoxBlock extends BaseEntityBlock {

    public static final IntegerProperty THICKNESS = IntegerProperty.create("thickness", 0, 4);

    /** {@code thickness * 64 + connectionMask -> VoxelShape}, computed on demand. */
    private static final Map<Integer, VoxelShape> SHAPE_CACHE = new HashMap<>();

    public PowerCableBoxBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(THICKNESS, 0)
                .setValue(BlockCable.POS_X, false).setValue(BlockCable.NEG_X, false)
                .setValue(BlockCable.POS_Y, false).setValue(BlockCable.NEG_Y, false)
                .setValue(BlockCable.POS_Z, false).setValue(BlockCable.NEG_Z, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THICKNESS, BlockCable.POS_X, BlockCable.NEG_X, BlockCable.POS_Y, BlockCable.NEG_Y, BlockCable.POS_Z, BlockCable.NEG_Z);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBaseBlockEntity(EnergyNetworkBlockEntities.CABLE.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == EnergyNetworkBlockEntities.CABLE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return BlockCable.withConnectionState(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return BlockCable.withConnectionState(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int thickness = state.getValue(THICKNESS);
        boolean posX = state.getValue(BlockCable.POS_X);
        boolean negX = state.getValue(BlockCable.NEG_X);
        boolean posY = state.getValue(BlockCable.POS_Y);
        boolean negY = state.getValue(BlockCable.NEG_Y);
        boolean posZ = state.getValue(BlockCable.POS_Z);
        boolean negZ = state.getValue(BlockCable.NEG_Z);
        int mask = (posX ? 32 : 0) | (negX ? 16 : 0) | (posY ? 8 : 0) | (negY ? 4 : 0) | (posZ ? 2 : 0) | (negZ ? 1 : 0);

        int key = thickness * 64 + mask;
        return SHAPE_CACHE.computeIfAbsent(key, k -> buildShape(thickness, posX, negX, posY, negY, posZ, negZ));
    }

    /**
     * Ported from CE's own {@code getBoundingBox}/{@code addCollisionBoxToList} arm-building logic
     * (identical pixel math and per-thickness/per-arm branching), just producing a {@link VoxelShape}
     * union instead of a raw {@code AxisAlignedBB} list.
     */
    private static VoxelShape buildShape(int thickness, boolean posX, boolean negX, boolean posY, boolean negY, boolean posZ, boolean negZ) {
        double lower = 0.125D;
        double upper = 0.875D;
        for (int i = 0; i < thickness; i++) {
            lower += 0.0625D;
            upper -= 0.0625D;
        }
        if (lower > 0.5D) lower = 0.5D;
        if (upper < 0.5D) upper = 0.5D;

        VoxelShape shape = Shapes.box(lower, lower, lower, upper, upper, upper);
        if (posX) shape = Shapes.joinUnoptimized(shape, Shapes.box(upper, lower, lower, 1D, upper, upper), BooleanOp.OR);
        if (negX) shape = Shapes.joinUnoptimized(shape, Shapes.box(0D, lower, lower, lower, upper, upper), BooleanOp.OR);
        if (posY) shape = Shapes.joinUnoptimized(shape, Shapes.box(lower, upper, lower, upper, 1D, upper), BooleanOp.OR);
        if (negY) shape = Shapes.joinUnoptimized(shape, Shapes.box(lower, 0D, lower, upper, lower, upper), BooleanOp.OR);
        if (posZ) shape = Shapes.joinUnoptimized(shape, Shapes.box(lower, lower, upper, upper, upper, 1D), BooleanOp.OR);
        if (negZ) shape = Shapes.joinUnoptimized(shape, Shapes.box(lower, lower, 0D, upper, upper, lower), BooleanOp.OR);
        return shape.isEmpty() ? Shapes.block() : shape;
    }
}
