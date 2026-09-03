package com.hbm.blocks.machine.foundry;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.blockentity.machine.foundry.FoundryChannelBlockEntity;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemScraps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge port of CE {@code FoundryChannel} - foundry channel for molten metal flow.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/blocks/machine/FoundryChannel.java
 * <p>
 * Connected-texture channel: uses 4-bit meta for NESW connections (CE :108-112).
 * Shovel interaction: scoop molten metal as scrap (CE :227-243).
 * Dynamic collision box based on connections (CE :138-156).
 */
public class BlockFoundryChannel extends Block implements EntityBlock, ICrucibleAcceptor {

    public static final MapCodec<BlockFoundryChannel> CODEC = simpleCodec(BlockFoundryChannel::new);
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);

    public BlockFoundryChannel(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(META, 0));
    }

    @Override
    protected @NotNull MapCodec<BlockFoundryChannel> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(META, getConnectionMeta(context.getLevel(), context.getClickedPos()));
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        updateMeta(level, pos);
    }

    @Override
    protected void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block neighborBlock, @NotNull BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        updateMeta(level, pos);
    }

    private void updateMeta(Level level, BlockPos pos) {
        BlockState oldState = level.getBlockState(pos);
        int oldMeta = oldState.getValue(META);
        int newMeta = getConnectionMeta(level, pos);

        if (newMeta == oldMeta) return;

        BlockEntity be = level.getBlockEntity(pos);
        CompoundTag nbt = null;
        if (be != null) {
            nbt = be.saveWithoutMetadata(level.registryAccess());
        }

        level.setBlock(pos, oldState.setValue(META, newMeta), 3);

        if (nbt != null) {
            BlockEntity newBe = level.getBlockEntity(pos);
            if (newBe != null) {
                newBe.loadWithComponents(nbt, level.registryAccess());
                newBe.setChanged();
            }
        }
    }

    private int getConnectionMeta(BlockGetter level, BlockPos pos) {
        int meta = 0;
        if (canConnectTo(level, pos, Direction.EAST)) meta |= 1;
        if (canConnectTo(level, pos, Direction.WEST)) meta |= 2;
        if (canConnectTo(level, pos, Direction.SOUTH)) meta |= 4;
        if (canConnectTo(level, pos, Direction.NORTH)) meta |= 8;
        return meta;
    }

    public boolean canConnectTo(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        Block neighborBlock = neighborState.getBlock();

        if (neighborBlock instanceof BlockFoundryOutlet) {
            return ((BlockFoundryOutlet) neighborBlock).getFacing(neighborState) == direction;
        }

        // TODO(CE: FoundryChannel.java:125): foundry_mold connection - not ported yet
        return neighborBlock instanceof BlockFoundryChannel;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new FoundryChannelBlockEntity(pos, state);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        int meta = state.getValue(META);
        VoxelShape shape = Shapes.box(0.3125, 0, 0.3125, 0.6875, 0.5, 0.6875);

        if ((meta & 1) != 0) shape = Shapes.or(shape, Shapes.box(0.6875, 0, 0.3125, 1, 0.5, 0.6875)); // +X
        if ((meta & 2) != 0) shape = Shapes.or(shape, Shapes.box(0, 0, 0.3125, 0.3125, 0.5, 0.6875)); // -X
        if ((meta & 4) != 0) shape = Shapes.or(shape, Shapes.box(0.3125, 0, 0.6875, 0.6875, 0.5, 1)); // +Z
        if ((meta & 8) != 0) shape = Shapes.or(shape, Shapes.box(0.3125, 0, 0, 0.6875, 0.5, 0.3125)); // -Z

        return shape;
    }

    @Override
    protected boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return true;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FoundryChannelBlockEntity channel)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!stack.isEmpty() && stack.getItem() instanceof TieredItem) {
            if (channel.amount > 0) {
                ItemStack scrap = ItemScraps.create(new Mats.MaterialStack(channel.type, channel.amount), false);
                if (!player.addItem(scrap)) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, scrap));
                }
                channel.amount = 0;
                channel.type = null;
                channel.propagateMaterial(null);
                channel.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FoundryChannelBlockEntity channel && channel.amount > 0) {
                ItemStack scrap = ItemScraps.create(new Mats.MaterialStack(channel.type, channel.amount), false);
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, scrap));
                channel.amount = 0;
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        BlockEntity te = world.getBlockEntity(pos);
        return te instanceof ICrucibleAcceptor && ((ICrucibleAcceptor) te).canAcceptPartialPour(world, pos, dX, dY, dZ, side, stack);
    }

    @Override
    public Mats.MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, Mats.MaterialStack stack) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof ICrucibleAcceptor) return ((ICrucibleAcceptor) te).pour(world, pos, dX, dY, dZ, side, stack);
        return stack;
    }

    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        BlockEntity te = world.getBlockEntity(pos);
        return te instanceof ICrucibleAcceptor && ((ICrucibleAcceptor) te).canAcceptPartialFlow(world, pos, side, stack);
    }

    @Override
    public Mats.MaterialStack flow(Level world, BlockPos pos, Direction side, Mats.MaterialStack stack) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof ICrucibleAcceptor) return ((ICrucibleAcceptor) te).flow(world, pos, side, stack);
        return stack;
    }
}
