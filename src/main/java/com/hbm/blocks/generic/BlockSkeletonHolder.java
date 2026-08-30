package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Dungeon prop holding one {@code ItemStack}, ported from CE's {@code BlockSkeletonHolder}. CE's
 * bespoke {@code .obj} model ({@code HFRWavefrontObject} via {@code StaticMetaWavefrontBakedModel})
 * has no confirmed NeoForge 1.21 geometry-loader equivalent; per the port instructions, this block
 * registers with a plain default model instead of guessing an API - the rendering gap (no held-item
 * display, no custom mesh) is a known, documented follow-up for whoever ports a geometry pipeline.
 */
public class BlockSkeletonHolder extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public BlockSkeletonHolder(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SkeletonHolderBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SkeletonHolderBlockEntity holder)) {
            return InteractionResult.PASS;
        }

        if (holder.item.isEmpty() && !stack.isEmpty()) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            holder.item = stack.copy();
            stack.shrink(stack.getCount());
            holder.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SkeletonHolderBlockEntity holder) || holder.item.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        player.getInventory().placeItemBackInInventory(holder.item.copy());
        holder.item = ItemStack.EMPTY;
        holder.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SkeletonHolderBlockEntity holder && !holder.item.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, holder.item.copy()));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static class SkeletonHolderBlockEntity extends BlockEntity {

        public ItemStack item = ItemStack.EMPTY;

        public SkeletonHolderBlockEntity(BlockPos pos, BlockState state) {
            super(GenericCrateBlocks.SKELETON_HOLDER_ENTITY_TYPE.get(), pos, state);
        }

        @Override
        protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            if (!item.isEmpty()) {
                tag.put("item", item.save(registries, new CompoundTag()));
            }
        }

        @Override
        protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            item = tag.contains("item") ? ItemStack.parseOptional(registries, tag.getCompound("item")) : ItemStack.EMPTY;
        }

        @Override
        public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            saveAdditional(tag, registries);
            return tag;
        }

        @Override
        public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
            return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
