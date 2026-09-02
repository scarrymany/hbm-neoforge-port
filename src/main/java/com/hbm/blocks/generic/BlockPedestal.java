package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Port of CE {@code BlockPedestal} (SHA {@code 293649fc}): red-room/loot-display pedestal that holds
 * one item. Right-click swaps/places/removes. Redstone-powered pedestal checks 3×3 grid for
 * {@link PedestalRecipe} multiblock patterns (moon/sun/karma conditions). CE implements redstone
 * ritual crafting; simplified here: only hold/display item, no recipes (PedestalRecipes not ported).
 * <p>
 * CE cite: {@code BlockPedestal.java:40-145} (block) + {@code :208-245} (TileEntity).
 */
public class BlockPedestal extends BaseEntityBlock {

    public static final MapCodec<BlockPedestal> CODEC = simpleCodec(BlockPedestal::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);

    public BlockPedestal(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
        return new PedestalBlockEntity(pos, state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PedestalBlockEntity pedestal && !pedestal.item.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, pedestal.item.copy()));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isCrouching()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PedestalBlockEntity pedestal)) return InteractionResult.PASS;

        ItemStack held = player.getMainHandItem();

        if (pedestal.item.isEmpty() && !held.isEmpty()) {
            pedestal.item = held.copy();
            player.setItemInHand(player.getUsedItemHand(), ItemStack.EMPTY);
            pedestal.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            return InteractionResult.SUCCESS;
        } else if (!pedestal.item.isEmpty() && held.isEmpty()) {
            player.setItemInHand(player.getUsedItemHand(), pedestal.item.copy());
            pedestal.item = ItemStack.EMPTY;
            pedestal.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            return InteractionResult.SUCCESS;
        } else if (!pedestal.item.isEmpty() && !held.isEmpty()) {
            ItemStack temp = held.copy();
            player.setItemInHand(player.getUsedItemHand(), pedestal.item.copy());
            pedestal.item = temp;
            pedestal.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * CE {@code TileEntityPedestal} — single-slot item holder for red-room loot display. Syncs to client
     * for rendering. CE recipe-checking logic (neighborChanged → check 3×3 pedestal grid → PedestalRecipes
     * match → consume inputs → spawn output) **not ported** — only storage/display implemented.
     */
    public static class PedestalBlockEntity extends BlockEntity {

        public ItemStack item = ItemStack.EMPTY;

        public PedestalBlockEntity(BlockPos pos, BlockState state) {
            super(GenericCrateBlocks.PEDESTAL_ENTITY_TYPE.get(), pos, state);
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
            if (tag.contains("item")) {
                item = ItemStack.parseOptional(registries, tag.getCompound("item"));
            } else {
                item = ItemStack.EMPTY;
            }
        }

        @Override
        public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            saveAdditional(tag, registries);
            return tag;
        }

        @Nullable
        @Override
        public ClientboundBlockEntityDataPacket getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
