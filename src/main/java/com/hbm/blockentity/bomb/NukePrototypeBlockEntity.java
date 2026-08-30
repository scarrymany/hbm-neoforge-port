package com.hbm.blockentity.bomb;

import com.hbm.inventory.container.bomb.NukePrototypeMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemBreedingRod.BreedingRodType;
import com.hbm.items.machine.MachineItems;
import com.hbm.items.special.ItemCell;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Ported from CE's {@code TileEntityNukePrototype} (149 lines, read in full) - 14-slot antischrabidium
 * test-rig casing, each slot capped to a single item. {@code isReady()} checks a fixed sandwich of
 * SAS3 cells (slots 0/1/12/13) around 5 breeding-rod pairs (slots 2-11) - CE's
 * {@code isItemEqual(new ItemStack(ModItems.rod_quad, 1, BreedingRodType.X.ordinal()))} reduces to a
 * flat identity check against this port's per-{@link BreedingRodType} flattened
 * {@link MachineItems#BREEDING_ROD_QUAD} registry.
 */
public class NukePrototypeBlockEntity extends NukeCasingBlockEntity {

    public NukePrototypeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 14, 1);
    }

    private boolean rod(int slot, BreedingRodType type) {
        return inventory.getStackInSlot(slot).getItem() == MachineItems.BREEDING_ROD_QUAD.get(type).get();
    }

    private boolean sas3(int slot) {
        return ItemCell.isFullCell(inventory.getStackInSlot(slot), Fluids.SAS3);
    }

    public boolean isReady() {
        return sas3(0) && sas3(1)
                && rod(2, BreedingRodType.URANIUM) && rod(3, BreedingRodType.URANIUM)
                && rod(4, BreedingRodType.LEAD) && rod(5, BreedingRodType.LEAD)
                && rod(6, BreedingRodType.NP237) && rod(7, BreedingRodType.NP237)
                && rod(8, BreedingRodType.LEAD) && rod(9, BreedingRodType.LEAD)
                && rod(10, BreedingRodType.URANIUM) && rod(11, BreedingRodType.URANIUM)
                && sas3(12) && sas3(13);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukePrototype");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NukePrototypeMenu(containerId, playerInventory, this);
    }
}
