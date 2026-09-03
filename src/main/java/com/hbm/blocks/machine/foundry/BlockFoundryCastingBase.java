package com.hbm.blocks.machine.foundry;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.blockentity.machine.foundry.FoundryCastingBaseBlockEntity;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemMold;
import com.hbm.items.machine.ItemScraps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge port of CE {@code FoundryCastingBase} - base class for foundry casting blocks.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/blocks/machine/FoundryCastingBase.java
 * <p>
 * Handles mold insertion (CE :113-135), output extraction (CE :99-109), shovel scrap (CE :138-151).
 */
public abstract class BlockFoundryCastingBase extends Block implements EntityBlock, ICrucibleAcceptor {

    protected BlockFoundryCastingBase(Properties properties) {
        super(properties);
    }

    public double getPH() {
        return 1;
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return true;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FoundryCastingBaseBlockEntity cast)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!cast.inventory.getStackInSlot(1).isEmpty()) {
            if (!player.addItem(cast.inventory.getStackInSlot(1).copy())) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, cast.inventory.getStackInSlot(1).copy()));
            }
            cast.inventory.setStackInSlot(1, ItemStack.EMPTY);
            cast.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            return ItemInteractionResult.SUCCESS;
        }

        if (!stack.isEmpty() && stack.getItem() instanceof ItemMold) {
            ItemMold.MoldEntry mold = ItemMold.getMold(stack);

            if (mold.large() == (cast.getMoldSize() == 1)) {
                if (!cast.inventory.getStackInSlot(0).isEmpty()) {
                    ItemStack prevMold = cast.inventory.getStackInSlot(0);
                    if (!player.addItem(prevMold)) {
                        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, prevMold));
                    }
                }

                ItemStack newMold = stack.copy();
                newMold.setCount(1);
                cast.inventory.setStackInSlot(0, newMold);
                if (!player.isCreative()) stack.shrink(1);

                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.5F, 1.0F);
                cast.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
                return ItemInteractionResult.SUCCESS;
            }
        }

        if (!stack.isEmpty() && stack.getItem() instanceof TieredItem) {
            if (cast.amount > 0) {
                ItemStack scrap = ItemScraps.create(new Mats.MaterialStack(cast.type, cast.amount), false);
                if (!player.addItem(scrap)) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, scrap));
                }
                cast.amount = 0;
                cast.type = null;
                cast.setChanged();
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
            if (be instanceof FoundryCastingBaseBlockEntity cast) {
                if (cast.amount > 0) {
                    ItemStack scrap = ItemScraps.create(new Mats.MaterialStack(cast.type, cast.amount), false);
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, scrap));
                    cast.amount = 0;
                }
                if (!cast.inventory.getStackInSlot(0).isEmpty()) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, cast.inventory.getStackInSlot(0)));
                }
                if (!cast.inventory.getStackInSlot(1).isEmpty()) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, cast.inventory.getStackInSlot(1)));
                }
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
