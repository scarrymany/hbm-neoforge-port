package com.hbm.inventory.container;

import com.hbm.entity.cart.EntityMinecartCrate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Port of CE's {@code EntityMinecartCrate.ContainerCartCrate} - pixel positions/slot layout copied
 * verbatim (6x9 cargo grid). See {@link EntityMenuBase} for why this is a standalone entity-backed
 * menu rather than {@link MenuBase} - the same design gap {@code
 * docs/phase4/entities_vehicles_aircraft.md} flagged and this port's sibling rail/train package
 * (see {@link TrainCargoTramMenu}) already solved; this class follows that exact precedent.
 */
public class MinecartCrateMenu extends EntityMenuBase<EntityMinecartCrate> {

    public MinecartCrateMenu(int id, Inventory playerInventory, EntityMinecartCrate crate) {
        super(ModMenuTypes.CART_CRATE.get(), id, crate);

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(crate, j + i * 9, 8 + j * 18, 18 + i * 18));
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

    public static MinecartCrateMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        if (playerInventory.player.level().getEntity(entityId) instanceof EntityMinecartCrate crate) {
            return new MinecartCrateMenu(id, playerInventory, crate);
        }
        throw new IllegalStateException("No EntityMinecartCrate with id " + entityId);
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
            } else if (!this.moveItemStackTo(stack, 0, entity.getContainerSize(), false)) {
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
