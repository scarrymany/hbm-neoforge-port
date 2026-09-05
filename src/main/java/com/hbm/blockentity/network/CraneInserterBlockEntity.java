package com.hbm.blockentity.network;

import com.hbm.menu.CraneInserterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge port of CE's {@code TileEntityCraneInserter} - item inserter crane with 21-slot buffer.
 * Ported exactly from CE: tries to push items from internal buffer into adjacent inventory on output side.
 * When powered by redstone, pauses insertion.
 */
public class CraneInserterBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INVENTORY_SIZE = 21;

    private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public boolean isIndirectlyPowered = false;
    public boolean destroyer = true; // CE default: destroy items if can't insert

    public CraneInserterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide || isIndirectlyPowered) {
            return;
        }
        tryFillTe();
    }

    /**
     * CE's tryFillTe: attempts to push items from internal buffer into adjacent inventory on output side.
     */
    private void tryFillTe() {
        Direction outputSide = getOutputSide();
        Direction accessSide = outputSide.getOpposite();
        BlockPos targetPos = worldPosition.relative(outputSide);
        BlockEntity targetBE = level.getBlockEntity(targetPos);

        if (targetBE != null) {
            IItemHandler targetHandler = level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    targetPos,
                    targetBE.getBlockState(),
                    targetBE,
                    accessSide
            );

            if (targetHandler != null) {
                for (int i = 0; i < inventory.getSlots(); i++) {
                    tryFillContainerCap(targetHandler, i);
                }
            }
        }
    }

    /**
     * CE's tryFillContainerCap: tries to insert stack from inventory slot into target handler.
     */
    private boolean tryFillContainerCap(IItemHandler target, int invSlot) {
        ItemStack stack = inventory.getStackInSlot(invSlot);
        if (stack.isEmpty()) {
            return false;
        }
        return tryInsertItemCap(target, stack);
    }

    /**
     * CE's tryInsertItemCap: inserts as much of the stack as possible into target handler.
     * Ported binary search logic exactly from CE for slot limit handling.
     */
    public boolean tryInsertItemCap(IItemHandler target, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        boolean movedAny = false;

        for (int i = 0; i < target.getSlots() && !stack.isEmpty(); i++) {
            ItemStack probe = stack.copy();
            probe.setCount(1);
            ItemStack simOne = target.insertItem(i, probe, true);
            if (!simOne.isEmpty()) {
                continue;
            }

            int maxTry = Math.min(stack.getCount(), target.getSlotLimit(i));
            int accepted = findMaxInsertable(target, i, stack, maxTry);

            if (accepted > 0) {
                ItemStack toInsert = stack.copy();
                toInsert.setCount(accepted);
                ItemStack rest = target.insertItem(i, toInsert, false);

                int actuallyInserted = accepted - (!rest.isEmpty() ? rest.getCount() : 0);
                if (actuallyInserted > 0) {
                    stack.shrink(actuallyInserted);
                    movedAny = true;
                }
            }
        }

        return movedAny;
    }

    /**
     * CE's findMaxInsertable: binary search to find max insertable count for a slot.
     */
    private int findMaxInsertable(IItemHandler target, int slot, ItemStack stack, int upperBound) {
        int lo = 0;
        int hi = upperBound;

        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            ItemStack test = stack.copy();
            test.setCount(mid);
            ItemStack res = target.insertItem(slot, test, true);

            if (res.isEmpty()) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }

        return lo;
    }

    /**
     * CE's tryFillTeDirect: accepts item from conveyor/external source directly into buffer.
     */
    public boolean tryFillTeDirect(ItemStack stack) {
        return tryInsertItemCap(inventory, stack);
    }

    private Direction getOutputSide() {
        // Simplified: output is opposite of input (CE's outputOverride screwdriver system deferred)
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        return facing.getOpposite();
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putBoolean("Destroyer", destroyer);
        tag.putBoolean("Powered", isIndirectlyPowered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        destroyer = tag.getBoolean("Destroyer");
        isIndirectlyPowered = tag.getBoolean("Powered");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm.crane_inserter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CraneInserterMenu(containerId, playerInventory, this);
    }
}
