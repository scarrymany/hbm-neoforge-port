package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FileCabinetMenu;
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
 * CE {@code TileEntityFileCabinet} — 8 slots. No hopper automation
 * ({@code getAccessibleSlotsFromSide} empty). {@code isItemValidForSlot} stays true so
 * {@link com.hbm.inventory.container.MenuBase} checked inventory still accepts player clicks.
 * TESR drawer extents skipped.
 */
public class FilingCabinetBlockEntity extends MachineBaseBlockEntity implements MenuProvider {

    public FilingCabinetBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 8, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fileCabinet");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[0];
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FileCabinetMenu(id, inv, this);
    }
}
