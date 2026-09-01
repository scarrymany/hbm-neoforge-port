package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.StorageDrumMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.StorageDrumRecipes;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityStorageDrum.java}:38- — 24 slots, WASTEFLUID/WASTEGAS 16k.
 * Depleted-waste I/O unregistered → only rows with live ids convert.
 */
public class StorageDrumBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardSenderMK2, ITickableBE, MenuProvider {

    public final FluidTankNTM liquid;
    public final FluidTankNTM gas;

    public StorageDrumBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 24, true, false);
        this.liquid = new FluidTankNTM(Fluids.WASTEFLUID, 16_000).withOwner(this);
        this.gas = new FluidTankNTM(Fluids.WASTEGAS, 16_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.storageDrum");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return StorageDrumRecipes.isInput(stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return !StorageDrumRecipes.isInput(stack);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        int[] slots = new int[24];
        for (int i = 0; i < 24; i++) slots[i] = i;
        return slots;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        StorageDrumRecipes.register();
        for (int i = 0; i < 24; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            StorageDrumRecipes.WasteData data = StorageDrumRecipes.getWaste(stack);
            if (data == null) continue;
            if (level.random.nextInt(Math.max(1, data.chance())) != 0) continue;
            if (liquid.getFill() + data.liquid() > liquid.getMaxFill()) continue;
            if (gas.getFill() + data.gas() > gas.getMaxFill()) continue;
            inventory.setStackInSlot(i, data.output().copy());
            liquid.setFill(liquid.getFill() + data.liquid());
            gas.setFill(gas.getFill() + data.gas());
        }
        long age = level.getGameTime() % 20;
        for (DirPos pos : getConPos()) {
            if (age == 9 || age == 19) tryProvide(liquid, level, pos);
            if (age == 8 || age == 18) tryProvide(gas, level, pos);
        }
        dataChanged();
        networkPackMK2(25);
    }

    public DirPos[] getConPos() {
        return new DirPos[]{
                new DirPos(worldPosition.above(), Direction.UP),
                new DirPos(worldPosition.below(), Direction.DOWN),
                new DirPos(worldPosition.north(), Direction.NORTH),
                new DirPos(worldPosition.south(), Direction.SOUTH),
                new DirPos(worldPosition.east(), Direction.EAST),
                new DirPos(worldPosition.west(), Direction.WEST),
        };
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(liquid, gas);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(liquid, gas);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        liquid.writeToNBT(tag, "l");
        gas.writeToNBT(tag, "g");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        liquid.readFromNBT(tag, "l");
        gas.readFromNBT(tag, "g");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        liquid.serialize(buf);
        gas.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        liquid.deserialize(buf);
        gas.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new StorageDrumMenu(id, inv, this);
    }
}
