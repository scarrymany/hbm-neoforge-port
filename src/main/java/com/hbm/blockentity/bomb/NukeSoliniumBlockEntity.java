package com.hbm.blockentity.bomb;

import com.hbm.inventory.container.bomb.NukeSoliniumMenu;
import com.hbm.items.bomb.NukeCasingItems;
import com.hbm.items.machine.Phase11ProcessItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * CE {@code TileEntityNukeSolinium} — 9 slots: igniter/propellant/igniter + core + igniter/propellant/igniter.
 */
public class NukeSoliniumBlockEntity extends NukeCasingBlockEntity {

    public NukeSoliniumBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 9);
    }

    public boolean isReady() {
        return inventory.getStackInSlot(0).getItem() == Phase11ProcessItems.SOLINIUM_IGNITER.get()
                && inventory.getStackInSlot(1).getItem() == Phase11ProcessItems.SOLINIUM_PROPELLANT.get()
                && inventory.getStackInSlot(2).getItem() == Phase11ProcessItems.SOLINIUM_PROPELLANT.get()
                && inventory.getStackInSlot(3).getItem() == Phase11ProcessItems.SOLINIUM_IGNITER.get()
                && inventory.getStackInSlot(4).getItem() == NukeCasingItems.SOLINIUM_CORE.get()
                && inventory.getStackInSlot(5).getItem() == Phase11ProcessItems.SOLINIUM_IGNITER.get()
                && inventory.getStackInSlot(6).getItem() == Phase11ProcessItems.SOLINIUM_PROPELLANT.get()
                && inventory.getStackInSlot(7).getItem() == Phase11ProcessItems.SOLINIUM_PROPELLANT.get()
                && inventory.getStackInSlot(8).getItem() == Phase11ProcessItems.SOLINIUM_IGNITER.get();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeSolinium");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NukeSoliniumMenu(containerId, playerInventory, this);
    }
}
