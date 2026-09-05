package com.hbm.inventory.container.bomb;

import com.hbm.blockentity.bomb.BombMultiBlockEntity;
import com.hbm.inventory.container.MenuBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Ported from CE's {@code ContainerBombMulti} (56 lines). 6 machine slots (3×2 grid) + player inv.
 */
public class BombMultiMenu extends MenuBase<BombMultiBlockEntity> {

    public BombMultiMenu(int id, Inventory playerInv, BombMultiBlockEntity be) {
        super(ModBombMenus.BOMB_MULTI.get(), id, be);

        addSlot(new SlotItemHandler(be.inventory, 0, 44, 26));
        addSlot(new SlotItemHandler(be.inventory, 1, 62, 26));
        addSlot(new SlotItemHandler(be.inventory, 2, 80, 26));
        addSlot(new SlotItemHandler(be.inventory, 3, 44, 44));
        addSlot(new SlotItemHandler(be.inventory, 4, 62, 44));
        addSlot(new SlotItemHandler(be.inventory, 5, 80, 44));

        playerInv(playerInv, 8, 84);
    }

    public static BombMultiMenu fromNetwork(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInv.player.level().getBlockEntity(pos) instanceof BombMultiBlockEntity be) {
            return new BombMultiMenu(id, playerInv, be);
        }
        throw new IllegalStateException("No BombMultiBlockEntity at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return be.isUseableByPlayer(player);
    }
}
