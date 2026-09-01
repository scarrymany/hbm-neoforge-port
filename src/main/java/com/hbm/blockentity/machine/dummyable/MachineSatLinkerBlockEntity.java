package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.SatLinkerMenu;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.satellites.SatelliteSavedData;
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

/** CE {@code TileEntityMachineSatLinker} — copy freq 0→1, random unused freq on slot 2. */
public class MachineSatLinkerBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    public MachineSatLinkerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 3, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.satLinker");
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
        if (src.getItem() instanceof ISatChip && dest.getItem() instanceof ISatChip) {
            ISatChip.setFreqS(dest, ISatChip.getFreqS(src));
        }

        ItemStack random = inventory.getStackInSlot(2);
        if (random.getItem() instanceof ISatChip) {
            SatelliteSavedData satelliteData = SatelliteSavedData.getData(level);
            int newId = level.random.nextInt(100000);
            if (!satelliteData.isFreqTaken(newId)) {
                ISatChip.setFreqS(random, newId);
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SatLinkerMenu(id, inv, this);
    }
}
