package com.hbm.blockentity.network;

import com.hbm.entity.item.EntityMovingItem;
import com.hbm.menu.CraneGrabberMenu;
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
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * NeoForge port of CE's {@code TileEntityCraneGrabber} - grabs EntityMovingItem from conveyor above.
 * Ported exactly from CE: scans for EntityMovingItem in input side area, filters, inserts into output.
 */
public class CraneGrabberBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INVENTORY_SIZE = 11;
    public static final int[] FILTER_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8}; // 9 filter slots
    // Slots 9-10: upgrade slots (stack/ejector) - deferred

    private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public boolean isIndirectlyPowered = false;
    public boolean isWhitelist = false; // CE default: blacklist mode

    private int tickCounter = 0;

    public CraneGrabberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide || isIndirectlyPowered) {
            return;
        }

        tickCounter++;
        int delay = 20; // CE default: 1s between grabs (upgrades deferred)

        if (tickCounter >= delay) {
            tickCounter = 0;
            int amount = 1; // CE default: 1 item per grab (stack upgrades deferred)

            Direction inputSide = getInputSide();
            double reach = 1.0; // CE: adjusts for conveyor_double/triple (deferred)
            BlockPos inputPos = worldPosition.relative(inputSide);
            double x = (inputPos.getX() - worldPosition.getX()) * reach + worldPosition.getX();
            double y = (inputPos.getY() - worldPosition.getY()) * reach + worldPosition.getY();
            double z = (inputPos.getZ() - worldPosition.getZ()) * reach + worldPosition.getZ();

            // CE: scan for EntityMovingItem in 0.625 x 0.625 x 0.625 box (from 0.1875 to 0.8125)
            AABB scanBox = new AABB(
                    x + 0.1875, y + 0.1875, z + 0.1875,
                    x + 0.8125, y + 0.8125, z + 0.8125
            );

            List<EntityMovingItem> items = level.getEntitiesOfClass(EntityMovingItem.class, scanBox);
            for (EntityMovingItem item : items) {
                ItemStack stack = item.getItemStack().copy();
                
                // CE filter logic
                boolean match = matchesFilter(stack);
                if ((isWhitelist && !match) || (!isWhitelist && match)) {
                    continue;
                }

                int count = stack.getCount();
                int toGrab = Math.min(count, amount);
                stack.setCount(toGrab);

                // Try insert into output inventory
                if (tryFillTe(stack)) {
                    // Reduce or remove entity
                    int remaining = count - toGrab + stack.getCount();
                    if (remaining <= 0) {
                        item.discard(); // CE: setDead()
                    } else {
                        stack.setCount(remaining);
                        item.setItemStack(stack);
                    }
                }
            }
        }
    }

    /**
     * CE's filter matching: check if stack matches any filter slot (simplified - no ModulePatternMatcher yet).
     */
    private boolean matchesFilter(ItemStack stack) {
        for (int i : FILTER_SLOTS) {
            ItemStack filter = inventory.getStackInSlot(i);
            if (!filter.isEmpty() && ItemStack.isSameItemSameComponents(filter, stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * CE's tryFillTe: tries to insert grabbed stack into adjacent inventory on output side.
     */
    private boolean tryFillTe(ItemStack stack) {
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
                return tryInsertItemCap(targetHandler, stack);
            }
        }
        return false;
    }

    /**
     * CE's tryInsertItemCap: inserts as much of the stack as possible into target handler.
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

    private Direction getInputSide() {
        return getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
    }

    private Direction getOutputSide() {
        return getInputSide().getOpposite();
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public void dropContents(Level level, BlockPos pos) {
        // CE: only drop filter slots 9-10 (upgrades), not filter slots 0-8
        for (int i = 9; i < 11; i++) {
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
        tag.putBoolean("Whitelist", isWhitelist);
        tag.putBoolean("Powered", isIndirectlyPowered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        isWhitelist = tag.getBoolean("Whitelist");
        isIndirectlyPowered = tag.getBoolean("Powered");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm.crane_grabber");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CraneGrabberMenu(containerId, playerInventory, this);
    }
}
