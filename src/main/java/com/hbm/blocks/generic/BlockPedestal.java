package com.hbm.blocks.generic;

import com.hbm.inventory.recipes.PedestalRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * {@link PedestalRecipes.PedestalRecipe} multiblock patterns (moon/sun/karma conditions).
 * <p>
 * CE cite: {@code BlockPedestal.java:40-145} (block) + {@code :208-245} (TileEntity) +
 * {@code :144-196} (neighborChanged → scan 3×3 → match recipe → consume → spawn output).
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

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;

        // CE BlockPedestal.java:144-196: check if center pedestal has redstone power
        if (!level.hasNeighborSignal(pos)) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PedestalBlockEntity centerPedestal)) return;
        if (centerPedestal.item.isEmpty()) return;

        // Scan 3×3 pedestal grid: 8 pedestals at distance 3 in cardinal/diagonal directions + center
        // CE BlockPedestal.java:144-156: NW/N/NE/W/center/E/SW/S/SE order
        BlockPos[] offsets = {
                pos.offset(-3, 0, -3), // NW
                pos.offset(0, 0, -3),  // N
                pos.offset(3, 0, -3),  // NE
                pos.offset(-3, 0, 0),  // W
                pos,                   // center
                pos.offset(3, 0, 0),   // E
                pos.offset(-3, 0, 3),  // SW
                pos.offset(0, 0, 3),   // S
                pos.offset(3, 0, 3)    // SE
        };

        ItemStack[] stacks = new ItemStack[9];
        PedestalBlockEntity[] pedestals = new PedestalBlockEntity[9];

        for (int i = 0; i < 9; i++) {
            BlockEntity entity = level.getBlockEntity(offsets[i]);
            if (entity instanceof PedestalBlockEntity pedestal) {
                pedestals[i] = pedestal;
                stacks[i] = pedestal.item.copy();
            } else {
                stacks[i] = ItemStack.EMPTY;
            }
        }

        // Find matching recipe
        PedestalRecipes.PedestalRecipe recipe = PedestalRecipes.findRecipe(stacks, level, pos);
        if (recipe == null) return;

        // Consume inputs from all 9 pedestals
        for (int i = 0; i < 9; i++) {
            if (pedestals[i] != null) {
                int[] ringIndices = {0, 1, 2, 3, 5, 6, 7, 8};
                if (i == 4) {
                    // Center
                    pedestals[i].item.shrink(recipe.centerInput.count());
                } else {
                    // Ring
                    int ringIdx = -1;
                    for (int j = 0; j < 8; j++) {
                        if (ringIndices[j] == i) {
                            ringIdx = j;
                            break;
                        }
                    }
                    if (ringIdx >= 0 && recipe.ring[ringIdx] != null) {
                        pedestals[i].item.shrink(recipe.ring[ringIdx].count());
                    }
                }
                pedestals[i].setChanged();
                level.sendBlockUpdated(offsets[i], state, state, 3);
            }
        }

        // Spawn output at center pedestal position
        ItemEntity outputEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, recipe.output.copy());
        level.addFreshEntity(outputEntity);
    }

    /**
     * CE {@code TileEntityPedestal} — single-slot item holder for red-room loot display. Syncs to client
     * for rendering. Recipe-checking logic (neighborChanged → check 3×3 pedestal grid → PedestalRecipes
     * match → consume inputs → spawn output) now fully ported.
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
