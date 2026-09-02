package com.hbm.blockentity.network;

import com.hbm.menu.CraneBoxerMenu;
import net.minecraft.core.BlockPos;
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
 * NeoForge port of CE's {@code TileEntityCraneBoxer} - packages items into boxes.
 * Simplified without EntityMovingPackage: just a 21-slot buffer accepting items from conveyor.
 * CE logic: collects full stacks, packages them based on mode (4/8/16 items or redstone trigger).
 * Deferred: EntityMovingPackage spawning (needs conveyor package entity system).
 */
public class CraneBoxerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INVENTORY_SIZE = 21;

    private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public byte mode = 0; // CE modes: 0=4 items, 1=8 items, 2=16 items, 3=redstone trigger

    public CraneBoxerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        // CE logic: package items into EntityMovingPackage based on mode
        // Simplified: no EntityMovingPackage system yet - just buffer
        // TODO(CE): Port EntityMovingPackage to enable boxer output functionality
    }

    /**
     * CE's tryFillTeDirect: accepts item from conveyor into buffer.
     */
    public boolean tryFillTeDirect(ItemStack stack) {
        return tryInsertItemCap(inventory, stack);
    }

    /**
     * CE's tryInsertItemCap: inserts as much of the stack as possible into inventory.
     */
    private static boolean tryInsertItemCap(IItemHandler target, ItemStack stack) {
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

    private static int findMaxInsertable(IItemHandler target, int slot, ItemStack stack, int upperBound) {
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
        tag.putByte("Mode", mode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        mode = tag.getByte("Mode");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm.crane_boxer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CraneBoxerMenu(containerId, playerInventory, this);
    }
}
