package com.hbm.blocks.network;

import com.hbm.api.block.IToolable;
import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.entity.ConveyorEntityTypes;
import com.hbm.entity.item.EntityMovingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * Ported from CE's {@code com.hbm.blocks.network.BlockConveyorBase} (read in full). Owns the shared
 * {@code FACING} property, the {@link IConveyorBelt} travel/snapping math, the vanilla-item ->
 * {@link EntityMovingItem} pickup on {@link #entityInside}, and the shared non-full/non-opaque
 * bounding box.
 * <p>
 * CE's metadata plumbing ({@code getMetaFromState}/{@code getStateFromMeta}/
 * {@code createBlockState() -> BlockStateContainer}) is not ported - metadata itself no longer
 * exists post-flattening, blockstate properties already round-trip through
 * {@code createBlockStateDefinition} with no translation layer needed. Likewise the manual
 * {@code isFullCube}/{@code isBlockNormalCube}/{@code isNormalCube}/{@code isOpaqueCube}/
 * {@code getBlockFaceShape} overrides are replaced by the modern shape-derived equivalents: the
 * non-full {@link #getShape} return value already makes the block non-full/non-opaque for occlusion
 * purposes, and every conveyor block is registered with {@code Properties.noOcclusion()} (see
 * {@code ConveyorBlocks}) for the rest.
 */
public abstract class BlockConveyorBase extends Block implements IConveyorBelt, IToolable {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.25, 1.0);

    protected BlockConveyorBase(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public boolean canItemStay(Level world, int x, int y, int z, Vec3 itemPos) {
        return true;
    }

    @Override
    public Vec3 getTravelLocation(Level world, int x, int y, int z, Vec3 itemPos, double speed) {
        BlockPos pos = new BlockPos(x, y, z);
        Direction dir = this.getTravelDirection(world, pos, itemPos);
        Vec3 snap = this.getClosestSnappingPosition(world, pos, itemPos);
        Vec3 dest = new Vec3(
                snap.x - dir.getStepX() * speed,
                snap.y - dir.getStepY() * speed,
                snap.z - dir.getStepZ() * speed
        );
        Vec3 delta = dest.subtract(itemPos);
        double d2 = delta.lengthSqr();
        if (d2 < 1.0e-12) {
            return new Vec3(
                    itemPos.x - dir.getStepX() * speed,
                    itemPos.y - dir.getStepY() * speed,
                    itemPos.z - dir.getStepZ() * speed
            );
        }
        double inv = speed / Math.sqrt(d2);
        return new Vec3(
                itemPos.x + delta.x * inv,
                itemPos.y + delta.y * inv,
                itemPos.z + delta.z * inv
        );
    }

    public Direction getInputDirection(Level world, BlockPos pos) {
        return world.getBlockState(pos).getValue(FACING);
    }

    public Direction getOutputDirection(Level world, BlockPos pos) {
        return world.getBlockState(pos).getValue(FACING).getOpposite();
    }

    public Direction getTravelDirection(Level world, BlockPos pos, Vec3 itemPos) {
        return world.getBlockState(pos).getValue(FACING);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level world, BlockPos pos, Vec3 itemPos) {
        Direction dir = this.getTravelDirection(world, pos, itemPos);

        double posX = Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1);
        double posZ = Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1);

        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        double y = pos.getY() + 0.25;

        if (dir.getAxis() == Direction.Axis.X) {
            x = posX;
        } else if (dir.getAxis() == Direction.Axis.Z) {
            z = posZ;
        }

        return new Vec3(x, y, z);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide) {
            if (entity instanceof ItemEntity itemEntity && entity.tickCount > 10 && !entity.isRemoved()) {

                EntityMovingItem item = new EntityMovingItem(ConveyorEntityTypes.MOVING_ITEM.get(), level);
                item.setItemStack(itemEntity.getItem());
                Vec3 entityPos = entity.position();
                Vec3 snap = this.getClosestSnappingPosition(level, pos, entityPos);
                item.moveTo(snap.x, snap.y, snap.z, 0, 0);
                level.addFreshEntity(item);

                entity.discard();
            }
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ, InteractionHand hand,
                            IToolable.ToolType tool) {
        if (tool != IToolable.ToolType.SCREWDRIVER) {
            return false;
        }

        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);

        if (!player.isShiftKeyDown()) {
            world.setBlock(pos, state.rotate(Rotation.CLOCKWISE_90), 3);
        } else if (state.getBlock() instanceof BlockConveyorChute) {
            int type = state.getValue(BlockConveyorChute.TYPE);
            int newType = (type + 1) % 3; // 0 -> 1 -> 2 -> 0
            world.setBlock(pos, state.setValue(BlockConveyorChute.TYPE, newType), 3);
        }

        return true;
    }

    @Override
    protected @NotNull BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
