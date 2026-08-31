package com.hbm.inventory.container;

import com.hbm.entity.train.TrainCargoTramTrailer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Port of CE's {@code com.hbm.entity.train.TrainCargoTramTrailer.ContainerTrainCargoTramTrailer} -
 * pixel positions and slot layout copied verbatim (5x9 cargo grid, no battery slot). See
 * {@link EntityMenuBase} for why this is a standalone entity-backed menu rather than {@link MenuBase}.
 */
public class TrainCargoTramTrailerMenu extends EntityMenuBase<TrainCargoTramTrailer> {

    public TrainCargoTramTrailerMenu(int id, Inventory playerInventory, TrainCargoTramTrailer train) {
        super(ModMenuTypes.TRAIN_CARGO_TRAM_TRAILER.get(), id, train);

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(train, i * 9 + j, 8 + j * 18, 18 + i * 18));
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 198));
        }
    }

    public static TrainCargoTramTrailerMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        if (playerInventory.player.level().getEntity(entityId) instanceof TrainCargoTramTrailer train) {
            return new TrainCargoTramTrailerMenu(id, playerInventory, train);
        }
        throw new IllegalStateException("No TrainCargoTramTrailer with id " + entityId);
    }

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
            } else if (!this.moveItemStackTo(stack, 0, 45, false)) {
                return ItemStack.EMPTY;
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
