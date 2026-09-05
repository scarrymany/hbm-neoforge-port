package com.hbm.blockentity.network;

import com.hbm.menu.CraneExtractorMenu;
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
 * NeoForge port of CE's {@code TileEntityCraneExtractor} - pulls items from adjacent inventory into buffer.
 * Simplified port without pattern matcher/upgrades initially - will add if needed.
 * Ported CE core extraction logic exactly: pulls from input side (opposite of inserter), filters, whitelist/blacklist.
 */
public class CraneExtractorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INVENTORY_SIZE = 20;
    public static final int[] FILTER_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8}; // 9 filter slots
    public static final int[] BUFFER_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17}; // 9 buffer slots
    // Slots 18-19: upgrade slots (stack/ejector) - deferred

    private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public boolean isIndirectlyPowered = false;
    public boolean isWhitelist = false; // CE default: blacklist mode
    public boolean maxEject = false; // CE: only extract full stacks

    private int tickCounter = 0;

    public CraneExtractorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide || isIndirectlyPowered) {
            return;
        }

        tickCounter++;
        int delay = 20; // CE default: 1s between extractions (upgrades deferred)

        if (tickCounter >= delay) {
            tickCounter = 0;
            int amount = 1; // CE default: 1 item per extraction (stack upgrades deferred)

            Direction inputSide = getOutputSide(); // CE switcheroo: extractor input = normal output side
            Direction inputAccessSide = inputSide.getOpposite();
            BlockPos targetPos = worldPosition.relative(inputSide);
            BlockEntity targetBE = level.getBlockEntity(targetPos);

            if (targetBE != null) {
                IItemHandler sourceHandler = level.getCapability(
                        net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                        targetPos,
                        targetBE.getBlockState(),
                        targetBE,
                        inputAccessSide
                );

                if (sourceHandler != null) {
                    tryExtractFromSource(sourceHandler, amount);
                }
            }
        }
    }

    /**
     * CE's core extraction loop: scan source inventory, extract items matching filter, insert into buffer.
     */
    private void tryExtractFromSource(IItemHandler source, int amount) {
        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack stack = source.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

            // CE filter logic
            boolean match = matchesFilter(stack);
            if ((isWhitelist && !match) || (!isWhitelist && match)) {
                continue; // Skip if whitelist and no match, or blacklist and match
            }

            // CE maxEject logic
            if (maxEject && stack.getCount() < amount) {
                continue;
            }

            // Try extract
            int toExtract = Math.min(amount, stack.getCount());
            ItemStack simExtracted = source.extractItem(i, toExtract, true);
            if (!simExtracted.isEmpty()) {
                int filled = tryInsertIntoBuffer(simExtracted.copy());
                if (filled > 0) {
                    source.extractItem(i, filled, false);
                    break; // CE: extract one stack per tick
                }
            }
        }
    }

    /**
     * CE's tryInsertItemCap logic for buffer slots only.
     */
    private int tryInsertIntoBuffer(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        int filledAmount = 0;

        for (int slot : BUFFER_SLOTS) {
            if (stack.isEmpty()) {
                break;
            }

            ItemStack probe = stack.copy();
            probe.setCount(1);
            ItemStack simOne = inventory.insertItem(slot, probe, true);
            if (!simOne.isEmpty()) {
                continue;
            }

            int maxTry = Math.min(stack.getCount(), inventory.getSlotLimit(slot));
            int accepted = findMaxInsertable(slot, stack, maxTry);

            if (accepted > 0) {
                ItemStack toInsert = stack.copy();
                toInsert.setCount(accepted);
                ItemStack rest = inventory.insertItem(slot, toInsert, false);

                int actuallyInserted = accepted - (!rest.isEmpty() ? rest.getCount() : 0);
                if (actuallyInserted > 0) {
                    stack.shrink(actuallyInserted);
                    filledAmount += actuallyInserted;
                }
            }
        }

        return filledAmount;
    }

    private int findMaxInsertable(int slot, ItemStack stack, int upperBound) {
        int lo = 0;
        int hi = upperBound;

        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            ItemStack test = stack.copy();
            test.setCount(mid);
            ItemStack res = inventory.insertItem(slot, test, true);

            if (res.isEmpty()) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }

        return lo;
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

    private Direction getOutputSide() {
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        return facing.getOpposite();
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public void dropContents(Level level, BlockPos pos) {
        // CE: only drop buffer slots (9-17), keep filter slots
        for (int i = 9; i < 18; i++) {
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
        tag.putBoolean("MaxEject", maxEject);
        tag.putBoolean("Powered", isIndirectlyPowered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        isWhitelist = tag.getBoolean("Whitelist");
        maxEject = tag.getBoolean("MaxEject");
        isIndirectlyPowered = tag.getBoolean("Powered");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm.crane_extractor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CraneExtractorMenu(containerId, playerInventory, this);
    }
}
