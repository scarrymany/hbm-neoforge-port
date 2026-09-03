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
 * Drone logistics dock - spawn point and home base for delivery drones. Ported (simplified for Phase 4
 * vertical slice) from CE's {@code TileEntityDroneDock} (~300 lines).
 * <p>
 * <b>Phase 4 implementation:</b> Sets nearby drones' target to this dock's position (home/spawn behavior).
 * Transfers cargo between dock inventory and arrived drones (pull from drone, push to drone if queued).
 * Full CE features deferred:
 * - Refueling system (kerosene consumption tracking)
 * - Network path start point (link to waypoints/providers/requesters)
 * - 5-chunk radius scanning for linkable blocks
 * - GUI for configuration
 * - Fluid tank transfer
 * <p>
 * TODO(CE: TileEntityDroneDock full port): Add refuel logic, network scanning, GUI (GUIDroneDock),
 * fluid transfer. See CE TileEntityDroneDock.java for complete behavior.
 */
public class DroneDockBlockEntity extends BlockEntity implements ITickableBE, IDroneLinkable, Container {

    private static final double SEARCH_RADIUS = 32.0;
    private static final int UPDATE_INTERVAL = 20;
    private static final double ARRIVAL_DISTANCE = 2.0; // Drone considered "arrived" within 2 blocks
    public static final int SLOTS = 18; // Match CE drone cargo capacity

    private int tickCounter = 0;
    private NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public DroneDockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) return;

        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL) return;
        tickCounter = 0;

        BlockPos dockPos = this.getBlockPos();
        AABB searchBox = new AABB(dockPos).inflate(SEARCH_RADIUS);
        List<EntityDroneBase> drones = level.getEntitiesOfClass(EntityDroneBase.class, searchBox);

        for (EntityDroneBase drone : drones) {
            // Set dock as target - CE behavior: drones return to dock when idle or for refuel
            drone.setTarget(dockPos.getX() + 0.5, dockPos.getY() + 1.5, dockPos.getZ() + 0.5);

            // Transfer cargo if drone is at dock (arrived within 2 blocks)
            if (drone instanceof EntityDeliveryDrone deliveryDrone) {
                double distSq = drone.distanceToSqr(dockPos.getX() + 0.5, dockPos.getY() + 1.5, dockPos.getZ() + 0.5);
                if (distSq < ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
                    transferCargo(deliveryDrone);
                }
            }
        }
    }

    /**
     * CE cargo transfer: pull items from drone into dock, push queued items from dock into drone.
     * Simplified - full CE checks fuel, path state, priority, etc.
     */
    private void transferCargo(EntityDeliveryDrone drone) {
        // Pull items from drone to dock (if dock has space)
        for (int droneSlot = 0; droneSlot < drone.getContainerSize(); droneSlot++) {
            ItemStack droneStack = drone.getItem(droneSlot);
            if (!droneStack.isEmpty()) {
                // Try to merge into dock inventory
                for (int dockSlot = 0; dockSlot < this.getContainerSize(); dockSlot++) {
                    ItemStack dockStack = this.getItem(dockSlot);
                    if (dockStack.isEmpty()) {
                        // Empty slot - transfer whole stack
                        this.setItem(dockSlot, droneStack.copy());
                        drone.setItem(droneSlot, ItemStack.EMPTY);
                        this.setChanged();
                        break;
                    } else if (ItemStack.isSameItemSameComponents(dockStack, droneStack)) {
                        // Same item - merge stacks
                        int space = dockStack.getMaxStackSize() - dockStack.getCount();
                        if (space > 0) {
                            int transfer = Math.min(space, droneStack.getCount());
                            dockStack.grow(transfer);
                            droneStack.shrink(transfer);
                            if (droneStack.isEmpty()) {
                                drone.setItem(droneSlot, ItemStack.EMPTY);
                            }
                            this.setChanged();
                            if (droneStack.isEmpty()) break;
                        }
                    }
                }
            }
        }

        // Push items from dock to drone (if drone has space)
        // CE: checks if drone has active delivery task first - simplified here
        for (int dockSlot = 0; dockSlot < this.getContainerSize(); dockSlot++) {
            ItemStack dockStack = this.getItem(dockSlot);
            if (!dockStack.isEmpty()) {
                for (int droneSlot = 0; droneSlot < drone.getContainerSize(); droneSlot++) {
                    ItemStack droneStack = drone.getItem(droneSlot);
                    if (droneStack.isEmpty()) {
                        // Empty drone slot - transfer from dock
                        drone.setItem(droneSlot, dockStack.copy());
                        this.setItem(dockSlot, ItemStack.EMPTY);
                        this.setChanged();
                        break;
                    }
                }
                if (this.getItem(dockSlot).isEmpty()) continue; // Transferred successfully
            }
        }
    }

    // Container implementation
    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(inventory, slot, amount);
        if (!result.isEmpty()) this.setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(inventory, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clear();
    }

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
