package com.hbm.blocks.generic;

import com.hbm.lib.HBMSoundHandler;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
 * Supply-drop loot container, ported from CE's {@code BlockSupplyCrate}. CE round-trips its
 * contents through the dropped item's NBT when mined normally (so the crate can be picked back up
 * and replaced with its contents intact) and empties into the world when broken with a crowbar.
 * That NBT round-trip becomes an item data component in the port; see {@link #getCloneItemStack}
 * and the block-entity's save/load for the two ends of that trip.
 */
public class BlockSupplyCrate extends BaseEntityBlock {

    /**
     * NeoForge 1.20.5+ requires every {@link BaseEntityBlock} subtype to hand back a
     * {@link MapCodec} for data-driven (de)serialization; see the reference NeoForge 1.21.1 port's
     * {@code CrateBlock}/{@code BarrelBlock} for the same {@code simpleCodec} pattern.
     */
    public static final MapCodec<BlockSupplyCrate> CODEC = simpleCodec(BlockSupplyCrate::new);

    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);

    public BlockSupplyCrate(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<BlockSupplyCrate> codec() {
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
        return new SupplyCrateBlockEntity(pos, state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SupplyCrateBlockEntity crate) {
                ItemStack drop = new ItemStack(this);
                CompoundTag saved = crate.saveContents();
                if (!saved.isEmpty()) {
                    drop.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                            net.minecraft.world.item.component.CustomData.of(saved));
                }
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        net.minecraft.world.item.component.CustomData customData =
                stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SupplyCrateBlockEntity crate) {
                crate.loadContents(customData.copyTag());
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(BlockCrate.CROWBAR_TAG)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SupplyCrateBlockEntity crate) {
                for (ItemStack content : crate.items) {
                    Block.popResource(level, pos, content);
                }
            }
            level.removeBlock(pos, false);
            level.playSound(null, pos, HBMSoundHandler.crateBreak.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return ItemInteractionResult.SUCCESS;
    }

    public static class SupplyCrateBlockEntity extends BlockEntity {

        public final List<ItemStack> items = new ArrayList<>();

        public SupplyCrateBlockEntity(BlockPos pos, BlockState state) {
            super(GenericCrateBlocks.SUPPLY_CRATE_ENTITY_TYPE.get(), pos, state);
        }

        CompoundTag saveContents() {
            CompoundTag tag = new CompoundTag();
            if (!items.isEmpty()) {
                ListTag list = new ListTag();
                for (ItemStack stack : items) {
                    list.add(stack.save(this.level.registryAccess(), new CompoundTag()));
                }
                tag.put("items", list);
            }
            return tag;
        }

        void loadContents(CompoundTag tag) {
            items.clear();
            if (this.level == null) {
                return;
            }
            ListTag list = tag.getList("items", 10);
            for (int i = 0; i < list.size(); i++) {
                items.add(ItemStack.parseOptional(this.level.registryAccess(), list.getCompound(i)));
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.merge(saveContents());
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
    }
}
