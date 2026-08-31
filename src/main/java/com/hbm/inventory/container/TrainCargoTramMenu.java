package com.hbm.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.entity.train.TrainCargoTram;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Port of CE's {@code com.hbm.entity.train.TrainCargoTram.ContainerTrainCargoTram} - pixel positions
 * and slot layout copied verbatim (4x7 cargo grid, slot 28 the dedicated battery slot at (152,72)).
 * See {@link EntityMenuBase} for why this is a standalone entity-backed menu rather than
 * {@link MenuBase}.
 */
public class TrainCargoTramMenu extends EntityMenuBase<TrainCargoTram> {

    public TrainCargoTramMenu(int id, Inventory playerInventory, TrainCargoTram train) {
        super(ModMenuTypes.TRAIN_CARGO_TRAM.get(), id, train);

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 7; j++) {
                this.addSlot(new Slot(train, i * 7 + j, 8 + j * 18, 18 + i * 18));
            }
        }
        this.addSlot(new Slot(train, 28, 152, 72));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 122 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 180));
        }
    }

    public static TrainCargoTramMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        if (playerInventory.player.level().getEntity(entityId) instanceof TrainCargoTram train) {
            return new TrainCargoTramMenu(id, playerInventory, train);
        }
        throw new IllegalStateException("No TrainCargoTram with id " + entityId);
    }

    /** CE: {@code ContainerTrainCargoTram.transferStackInSlot} - shift-click into the player inventory
     * from the cargo/battery range, or (from the player inventory) into the battery slot if the stack
     * is an {@link IBatteryItem}, otherwise into the cargo range excluding the battery slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            newStack = stack.copy();

            if (slotIndex < entity.getContainerSize()) {
                if (!this.moveItemStackTo(stack, entity.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (stack.getItem() instanceof IBatteryItem) {
                    if (!this.moveItemStackTo(stack, 28, 29, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(stack, 0, 28, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return newStack;
    }
}
