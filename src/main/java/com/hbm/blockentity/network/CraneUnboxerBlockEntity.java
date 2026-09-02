package com.hbm.blockentity.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.blocks.network.BlockCraneUnboxer;
import com.hbm.entity.ConveyorEntityTypes;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.menu.CraneUnboxerMenu;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge port of CE's {@code TileEntityCraneUnboxer} - unpacks boxes into individual items.
 * CE logic: receives EntityMovingPackage (via onPackageEnter), buffers items, outputs them as EntityMovingItem.
 * Simplified: no upgrade slots (ejector speed/stack size) - using CE defaults (20 tick delay, 1 item/cycle).
 */
public class CraneUnboxerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INVENTORY_SIZE = 21;

    private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int tickCounter = 0;

    public CraneUnboxerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        tickCounter++;

        // CE default: 20 tick delay (1s), 1 item per output
        int delay = 20;
        int amount = 1;

        if (tickCounter >= delay && !level.hasNeighborSignal(worldPosition)) {
            tickCounter = 0;

            Direction outputSide = getOutputSide();
            BlockPos outputPos = worldPosition.relative(outputSide);
            IConveyorBelt belt = null;

            if (level.getBlockState(outputPos).getBlock() instanceof IConveyorBelt b) {
                belt = b;
            }

            if (belt != null) {
                // Find first non-empty slot and output
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack stack = inventory.getStackInSlot(i);

                    if (!stack.isEmpty()) {
                        int toSend = Math.min(amount, stack.getCount());
                        ItemStack outStack = stack.copy();
                        stack.shrink(toSend);
                        if (stack.isEmpty()) {
                            inventory.setStackInSlot(i, ItemStack.EMPTY);
                        }
                        outStack.setCount(toSend);

                        // Spawn EntityMovingItem on conveyor
                        EntityMovingItem item = new EntityMovingItem(ConveyorEntityTypes.MOVING_ITEM.get(), level);
                        Vec3 spawnPos = new Vec3(
                                worldPosition.getX() + 0.5 + outputSide.getStepX() * 0.55,
                                worldPosition.getY() + 0.5 + outputSide.getStepY() * 0.55,
                                worldPosition.getZ() + 0.5 + outputSide.getStepZ() * 0.55
                        );
                        Vec3 snap = belt.getClosestSnappingPosition(level, outputPos, spawnPos);
                        item.setPos(snap.x, snap.y, snap.z);
                        item.setItemStack(outStack);
                        level.addFreshEntity(item);
                        setChanged();
                        break;
                    }
                }
            }
        }
    }

    /**
     * CE's getOutputSide: same as block facing (matches CE's crane_unboxer pattern).
     */
    private Direction getOutputSide() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof BlockCraneUnboxer) {
            return state.getValue(BlockCraneUnboxer.FACING);
        }
        return Direction.NORTH;
    }

    /**
     * CE's tryFillTeDirect: accepts item from EntityMovingPackage into buffer.
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
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        tickCounter = tag.getInt("TickCounter");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm.crane_unboxer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CraneUnboxerMenu(containerId, playerInventory, this);
    }
}
