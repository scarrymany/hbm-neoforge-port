package com.hbm.blockentity.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.blocks.network.BlockCraneBoxer;
import com.hbm.entity.ConveyorEntityTypes;
import com.hbm.entity.item.EntityMovingPackage;
import com.hbm.menu.CraneBoxerMenu;
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
 * NeoForge port of CE's {@code TileEntityCraneBoxer} - packages items into boxes.
 * CE logic: collects full stacks, packages them based on mode (4/8/16 items or redstone trigger).
 * Spawns EntityMovingPackage onto adjacent conveyor belt.
 */
public class CraneBoxerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INVENTORY_SIZE = 21;
    public static final byte MODE_4 = 0;
    public static final byte MODE_8 = 1;
    public static final byte MODE_16 = 2;
    public static final byte MODE_REDSTONE = 3;

    private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public byte mode = MODE_4; // CE default: MODE_4
    private boolean lastRedstone = false;

    public CraneBoxerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        boolean redstone = level.hasNeighborSignal(worldPosition);

        // CE MODE_REDSTONE: package on redstone pulse (edge trigger)
        if (mode == MODE_REDSTONE && redstone && !lastRedstone) {
            trySpawnPackage(-1); // -1 = any non-empty slots
        }

        lastRedstone = redstone;

        // CE other modes: package every 2 ticks when enough full stacks collected
        if (mode != MODE_REDSTONE && level.getGameTime() % 2 == 0) {
            int packSize = switch (mode) {
                case MODE_4 -> 4;
                case MODE_8 -> 8;
                case MODE_16 -> 16;
                default -> 1;
            };

            int fullStacks = 0;
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getCount() == stack.getMaxStackSize()) {
                    fullStacks++;
                }
            }

            if (fullStacks >= packSize) {
                trySpawnPackage(packSize);
            }
        }
    }

    /**
     * CE's package spawning logic: collect items, spawn EntityMovingPackage on conveyor.
     * @param packSize -1 for "any non-empty", else exact count of full stacks to pack
     */
    private void trySpawnPackage(int packSize) {
        Direction outputSide = getOutputSide();
        BlockPos outputPos = worldPosition.relative(outputSide);
        IConveyorBelt belt = null;

        if (level.getBlockState(outputPos).getBlock() instanceof IConveyorBelt b) {
            belt = b;
        }

        if (belt == null) {
            return;
        }

        // Collect items to pack
        ItemStack[] box;
        if (packSize < 0) {
            // MODE_REDSTONE: pack any non-empty
            int count = 0;
            for (int i = 0; i < inventory.getSlots(); i++) {
                if (!inventory.getStackInSlot(i).isEmpty()) {
                    count++;
                }
            }
            if (count == 0) return;

            box = new ItemStack[count];
            int idx = 0;
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    box[idx++] = stack.copy();
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        } else {
            // Other modes: pack exact count of full stacks
            box = new ItemStack[packSize];
            int packed = 0;
            for (int i = 0; i < inventory.getSlots() && packed < packSize; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getCount() == stack.getMaxStackSize()) {
                    box[packSize - 1 - packed] = stack.copy();
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                    packed++;
                }
            }
        }

        // Spawn EntityMovingPackage
        EntityMovingPackage pkg = new EntityMovingPackage(ConveyorEntityTypes.MOVING_PACKAGE.get(), level);
        Vec3 spawnPos = new Vec3(
                worldPosition.getX() + 0.5 + outputSide.getStepX() * 0.55,
                worldPosition.getY() + 0.5 + outputSide.getStepY() * 0.55,
                worldPosition.getZ() + 0.5 + outputSide.getStepZ() * 0.55
        );
        Vec3 snap = belt.getClosestSnappingPosition(level, outputPos, spawnPos);
        pkg.setPos(snap.x, snap.y, snap.z);
        pkg.setItemStacks(box);
        level.addFreshEntity(pkg);
        setChanged();
    }

    /**
     * CE's getOutputSide: opposite of block facing (matches CraneInserter pattern).
     */
    private Direction getOutputSide() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof BlockCraneBoxer) {
            return state.getValue(BlockCraneBoxer.FACING).getOpposite();
        }
        return Direction.NORTH;
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
