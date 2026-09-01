package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.AshpitMenu;
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

/**
 * CE {@code TileEntityAshpit} — 5 output slots. {@code powder_ash} / {@code EnumAshType} still
 * unregistered → conversion skipped (same skip as firebox ashpit feed).
 */
public class MachineAshpitBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, MenuProvider {

    public MachineAshpitBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.ashpit");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return true;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        dataChanged();
        networkPackMK2(50);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AshpitMenu(id, inv, this);
    }
}
