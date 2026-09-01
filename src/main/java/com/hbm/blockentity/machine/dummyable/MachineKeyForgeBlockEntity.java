package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.KeyForgeMenu;
import com.hbm.items.tool.ItemKeyPin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** CE {@code TileEntityMachineKeyForge} — copy pins 0→1, randomize slot 2. */
public class MachineKeyForgeBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    public MachineKeyForgeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 3, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.keyForge");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemKeyPin pin && pin.canTransfer();
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return true;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        ItemStack src = inventory.getStackInSlot(0);
        ItemStack dest = inventory.getStackInSlot(1);
        if (src.getItem() instanceof ItemKeyPin a && dest.getItem() instanceof ItemKeyPin b
                && a.canTransfer() && b.canTransfer()) {
            ItemKeyPin.setPins(dest, ItemKeyPin.getPins(src));
        }

        ItemStack blank = inventory.getStackInSlot(2);
        if (blank.getItem() instanceof ItemKeyPin pin && pin.canTransfer()) {
            ItemKeyPin.setPins(blank, level.random.nextInt(900) + 100);
        }

        dataChanged();
        networkPackMK2(15);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new KeyForgeMenu(id, inv, this);
    }
}
