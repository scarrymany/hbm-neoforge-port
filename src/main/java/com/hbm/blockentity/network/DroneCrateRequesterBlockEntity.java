package com.hbm.blockentity.network;

import com.hbm.api.drone.IDroneLinkable;
import com.hbm.blockentity.ITickableBE;
import com.hbm.entity.item.EntityDeliveryDrone;
import com.hbm.entity.item.EntityDroneBase;
import com.hbm.inventory.gui.DroneCrateRequesterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Minimal CE {@code TileEntityDroneRequester} - requests items from delivery drones.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/tileentity/network/TileEntityDroneRequester.java
 * <p>
 * Phase 4 vertical slice: 18-slot inventory (9 filters + 9 stock). When drone arrives with items, pull into requester stock slots.
 * TODO(CE): RequestNetwork integration (RequestNode, ModulePatternMatcher filters, network-wide requests, filter modes).
 */
public class DroneCrateRequesterBlockEntity extends BlockEntity implements ITickableBE, IDroneLinkable, Container, MenuProvider {

    private static final double SEARCH_RADIUS = 16.0;
    private static final int UPDATE_INTERVAL = 20;
    private static final double ARRIVAL_DISTANCE = 2.0;
    public static final int SLOTS = 18; // CE: TileEntityDroneRequester super(18) - 9 filter + 9 stock

    private int tickCounter = 0;
    private NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public DroneCrateRequesterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) return;

        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL) return;
        tickCounter = 0;

        BlockPos requesterPos = this.getBlockPos();
        AABB searchBox = new AABB(requesterPos).inflate(SEARCH_RADIUS);
        List<EntityDroneBase> drones = level.getEntitiesOfClass(EntityDroneBase.class, searchBox);

        for (EntityDroneBase drone : drones) {
            if (drone instanceof EntityDeliveryDrone deliveryDrone) {
                double distSq = drone.distanceToSqr(requesterPos.getX() + 0.5, requesterPos.getY() + 1.5, requesterPos.getZ() + 0.5);
                if (distSq < ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
                    pullItemsFromDrone(deliveryDrone);
                }
            }
        }
    }

    /**
     * Pull items from drone cargo into requester stock slots (9-17). CE behavior: requester pulls matching items from drone.
     * Minimal filter logic: if filter slot (0-8) has item, only pull matching items to corresponding stock slot (9-17).
     */
    private void pullItemsFromDrone(EntityDeliveryDrone drone) {
        for (int droneSlot = 0; droneSlot < drone.getContainerSize(); droneSlot++) {
            ItemStack droneStack = drone.getItem(droneSlot);
            if (droneStack.isEmpty()) continue;

            // Try to match with filter slots and insert into corresponding stock slots
            for (int filterIdx = 0; filterIdx < 9; filterIdx++) {
                ItemStack filterStack = inventory.get(filterIdx);
                int stockSlot = filterIdx + 9;
                ItemStack stockStack = inventory.get(stockSlot);

                // If no filter, or filter matches drone item
                if (filterStack.isEmpty() || ItemStack.isSameItemSameComponents(filterStack, droneStack)) {
                    if (stockStack.isEmpty()) {
                        // Empty stock slot - transfer entire stack
                        inventory.set(stockSlot, droneStack.copy());
                        drone.setItem(droneSlot, ItemStack.EMPTY);
                        setChanged();
                        break;
                    } else if (ItemStack.isSameItemSameComponents(stockStack, droneStack)) {
                        // Matching item - merge stacks
                        int transferAmount = Math.min(droneStack.getCount(), stockStack.getMaxStackSize() - stockStack.getCount());
                        if (transferAmount > 0) {
                            stockStack.grow(transferAmount);
                            droneStack.shrink(transferAmount);
                            if (droneStack.isEmpty()) {
                                drone.setItem(droneSlot, ItemStack.EMPTY);
                            }
                            setChanged();
                            break;
                        }
                    }
                }
            }
        }
    }

    // Container implementation
    @Override public int getContainerSize() { return SLOTS; }
    @Override public boolean isEmpty() { return inventory.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return inventory.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return ContainerHelper.removeItem(inventory, slot, amount); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(inventory, slot); }
    @Override public void setItem(int slot, ItemStack stack) { inventory.set(slot, stack); setChanged(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { inventory.clear(); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, inventory, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, inventory, registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.droneRequester");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DroneCrateRequesterMenu(containerId, playerInventory, this);
    }
}
