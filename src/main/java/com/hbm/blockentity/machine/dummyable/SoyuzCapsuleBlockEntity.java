package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.SoyuzCapsuleMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** CE {@code TileEntitySoyuzCapsule} — 19-slot cargo (3×6 + soyuz item). */
public class SoyuzCapsuleBlockEntity extends MachineBaseBlockEntity implements MenuProvider {

    public SoyuzCapsuleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 19, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.soyuzCapsule");
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18};
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SoyuzCapsuleMenu(id, inv, this);
    }
}
