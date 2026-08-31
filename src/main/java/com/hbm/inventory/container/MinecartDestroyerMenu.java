package com.hbm.inventory.container;

import com.hbm.entity.cart.EntityMinecartDestroyer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Port of CE's {@code EntityMinecartDestroyer.ContainerCartDestroyer} (18 filter-template slots in
 * two 3x3 banks). CE backs its 18 slots with a special {@code SlotPattern} class (not ported - see
 * {@link #clicked} below) whose whole point is letting a player stamp <i>any</i> held item directly
 * into a slot (including an empty hand, to clear it) without the normal take/place exchange, since
 * these slots are a read-only filter template ({@link EntityMinecartDestroyer#canPlaceItem} is always
 * {@code false}) rather than real storage. Reimplemented here as a {@link #clicked} override on this
 * menu directly (CE's own {@code ContainerCartDestroyer.slotClick} override, ported 1:1) rather than
 * porting a whole separate {@code Slot} subclass for a behavior only this one menu needs.
 */
public class MinecartDestroyerMenu extends EntityMenuBase<EntityMinecartDestroyer> {

    public MinecartDestroyerMenu(int id, Inventory playerInventory, EntityMinecartDestroyer cart) {
        super(ModMenuTypes.CART_DESTROYER.get(), id, cart);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new Slot(cart, j + i * 3, 10 + j * 18, 17 + i * 18));
                this.addSlot(new Slot(cart, j + i * 3 + 9, 114 + j * 18, 17 + i * 18));
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    public static MinecartDestroyerMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        if (playerInventory.player.level().getEntity(entityId) instanceof EntityMinecartDestroyer cart) {
            return new MinecartDestroyerMenu(id, playerInventory, cart);
        }
        throw new IllegalStateException("No EntityMinecartDestroyer with id " + entityId);
    }

    /** CE: {@code ContainerCartDestroyer.transferStackInSlot { return ItemStack.EMPTY; }} - shift-click
     *  never moves anything (the filter slots are not real storage). */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    /** CE: {@code ContainerCartDestroyer.slotClick} - stamps whatever is on the cursor (including
     *  nothing, to clear the slot) directly into a filter slot, without taking or exchanging it. Any
     *  slot outside the cart's own range falls back to normal vanilla click handling. */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= entity.getContainerSize()) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        Slot slot = this.getSlot(slotId);
        slot.setByPlayer(this.getCarried().copy());
    }
}
