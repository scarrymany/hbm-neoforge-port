package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.TeleLinkerMenu;
import com.hbm.items.machine.ItemTurretBiometry;
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

/** CE {@code TileEntityMachineTeleLinker} — copy names 0→1, clear slot 2. */
public class MachineTeleLinkerBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    public MachineTeleLinkerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 3, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.teleLinker");
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
        if (src.getItem() instanceof ItemTurretBiometry && dest.getItem() instanceof ItemTurretBiometry) {
            String[] names = ItemTurretBiometry.getNames(src);
            if (names != null) {
                for (String name : names) {
                    ItemTurretBiometry.addName(dest, name);
                }
            }
        }

        ItemStack wipe = inventory.getStackInSlot(2);
        if (wipe.getItem() instanceof ItemTurretBiometry) {
            ItemTurretBiometry.clearNames(wipe);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new TeleLinkerMenu(id, inv, this);
    }
}
