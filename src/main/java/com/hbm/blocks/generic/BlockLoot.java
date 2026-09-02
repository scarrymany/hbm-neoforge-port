package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Loot container, ported from CE's {@code BlockLoot}: a slab-shaped world-gen prop that dumps its
 * pre-placed {@code ItemStack}s and vanishes on right-click (or on being broken). CE stores each
 * item alongside a per-item render offset for its bespoke static-multi-item baked model; since this
 * area is not porting a custom {@code BlockEntityRenderer} for it (no confirmed NeoForge geometry
 * path per the model-gap note in the port instructions), the offsets are dropped and only the
 * {@code ItemStack} list survives - a plain default model stands in for CE's baked pile.
 */
public class BlockLoot extends BaseEntityBlock {

    public static final MapCodec<BlockLoot> CODEC = simpleCodec(BlockLoot::new);

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    public BlockLoot(Properties properties) {
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
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LootBlockEntity(pos, state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LootBlockEntity loot) {
                for (ItemStack stack : loot.items) {
                    if (!stack.isEmpty()) {
                        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, stack.copy()));
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!player.isShiftKeyDown()) {
            level.removeBlock(pos, false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public static class LootBlockEntity extends BlockEntity {

        public final List<ItemStack> items = new ArrayList<>();

        public LootBlockEntity(BlockPos pos, BlockState state) {
            super(GenericCrateBlocks.LOOT_ENTITY_TYPE.get(), pos, state);
        }

        public LootBlockEntity addItem(ItemStack stack) {
            items.add(stack);
            return this;
        }

        /**
         * CE {@code BlockLoot.TileEntityLoot.addItem} signature with render offsets (x/y/z floats for bespoke
         * multi-item static pile model). This port doesn't render the baked pile model — offsets ignored.
         */
        public LootBlockEntity addItem(ItemStack stack, float x, float y, float z) {
            items.add(stack);
            return this;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            ListTag list = new ListTag();
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    list.add(stack.save(registries, new CompoundTag()));
                }
            }
            tag.put("items", list);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            items.clear();
            ListTag list = tag.getList("items", 10);
            for (int i = 0; i < list.size(); i++) {
                items.add(ItemStack.parseOptional(registries, list.getCompound(i)));
            }
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
