package com.hbm.blockentity.network;

import com.hbm.api.drone.IDroneLinkable;
import com.hbm.blockentity.ITickableBE;
import com.hbm.entity.item.EntityDeliveryDrone;
import com.hbm.entity.item.EntityDroneBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Minimal CE {@code TileEntityDroneProvider} - provides items to delivery drones.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/tileentity/network/TileEntityDroneProvider.java
 * <p>
 * Phase 4 vertical slice: 9-slot provider inventory. When delivery drone arrives nearby, push items from provider into drone cargo.
 * TODO(CE): RequestNetwork integration (OfferNode, network-wide item offers, pathfinding).
 */
public class DroneCrateProviderBlockEntity extends BlockEntity implements ITickableBE, IDroneLinkable, Container {

    private static final double SEARCH_RADIUS = 16.0;
    private static final int UPDATE_INTERVAL = 20;
    private static final double ARRIVAL_DISTANCE = 2.0;
    public static final int SLOTS = 9; // CE: TileEntityDroneProvider super(9)

    private int tickCounter = 0;
    private NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public DroneCrateProviderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) return;

        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL) return;
        tickCounter = 0;

        BlockPos providerPos = this.getBlockPos();
        AABB searchBox = new AABB(providerPos).inflate(SEARCH_RADIUS);
        List<EntityDroneBase> drones = level.getEntitiesOfClass(EntityDroneBase.class, searchBox);

        for (EntityDroneBase drone : drones) {
            if (drone instanceof EntityDeliveryDrone deliveryDrone) {
                double distSq = drone.distanceToSqr(providerPos.getX() + 0.5, providerPos.getY() + 1.5, providerPos.getZ() + 0.5);
                if (distSq < ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
                    pushItemsToDrone(deliveryDrone);
                }
            }
        }
    }

    /**
     * Push items from provider inventory into drone cargo. CE behavior: provider offers items, drone picks up.
     */
    private void pushItemsToDrone(EntityDeliveryDrone drone) {
        for (int providerSlot = 0; providerSlot < inventory.size(); providerSlot++) {
            ItemStack providerStack = inventory.get(providerSlot);
            if (providerStack.isEmpty()) continue;

            // Try to insert into drone's cargo inventory (slots 0-17, same as dock)
            for (int droneSlot = 0; droneSlot < drone.getContainerSize(); droneSlot++) {
                ItemStack droneStack = drone.getItem(droneSlot);
                if (droneStack.isEmpty()) {
                    // Empty drone slot - transfer entire stack
                    drone.setItem(droneSlot, providerStack.copy());
                    inventory.set(providerSlot, ItemStack.EMPTY);
                    setChanged();
                    break;
                } else if (ItemStack.isSameItemSameComponents(droneStack, providerStack)) {
                    // Matching item - merge stacks
                    int transferAmount = Math.min(providerStack.getCount(), droneStack.getMaxStackSize() - droneStack.getCount());
                    if (transferAmount > 0) {
                        droneStack.grow(transferAmount);
                        providerStack.shrink(transferAmount);
                        if (providerStack.isEmpty()) {
                            inventory.set(providerSlot, ItemStack.EMPTY);
                        }
                        setChanged();
                        break;
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
}
