package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.VacuumDistillMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.VacuumDistillRecipes;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.util.Tuple.Quartet;
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
 * CE {@code TileEntityMachineVacuumDistill}: 10k HE / 100 mB @ PU2.
 * {@code setType(9)} / four {@code unloadTank} Exact CE {@code :75-82}.
 */
public class MachineVacuumDistillBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 1_000_000;

    public final FluidTankNTM input;
    public final FluidTankNTM heavy;
    public final FluidTankNTM reformate;
    public final FluidTankNTM light;
    public final FluidTankNTM gas;
    public long power;
    public boolean isOn;

    public MachineVacuumDistillBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 10, true, true);
        this.input = new FluidTankNTM(Fluids.OIL, 64_000).withOwner(this).withPressure(2);
        this.heavy = new FluidTankNTM(Fluids.HEAVYOIL_VACUUM, 24_000).withOwner(this);
        this.reformate = new FluidTankNTM(Fluids.REFORMATE, 24_000).withOwner(this);
        this.light = new FluidTankNTM(Fluids.LIGHTOIL_VACUUM, 24_000).withOwner(this);
        this.gas = new FluidTankNTM(Fluids.SOURGAS, 24_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.vacuumDistill");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 9) return stack.getItem() instanceof IItemFluidIdentifier;
        return slot == 1 || slot == 3 || slot == 5 || slot == 7;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 2 || slot == 4 || slot == 6 || slot == 8;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        isOn = false;
        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) {
                trySubscribe(level, pos);
                trySubscribe(input.getTankType(), level, pos);
            }
        }
        power = Library.chargeTEFromItems(inventory, 0, power, MAX_POWER);
        // CE TileEntityMachineVacuumDistill.java:75-82
        input.setType(9, inventory);
        refine();
        heavy.unloadTank(1, 2, inventory);
        reformate.unloadTank(3, 4, inventory);
        light.unloadTank(5, 6, inventory);
        gas.unloadTank(7, 8, inventory);
        for (DirPos pos : getConPos()) {
            if (heavy.getFill() > 0) tryProvide(heavy, level, pos);
            if (reformate.getFill() > 0) tryProvide(reformate, level, pos);
            if (light.getFill() > 0) tryProvide(light, level, pos);
            if (gas.getFill() > 0) tryProvide(gas, level, pos);
        }
        dataChanged();
        networkPackMK2(25);
    }

    private void refine() {
        Quartet<FluidStack, FluidStack, FluidStack, FluidStack> ref = VacuumDistillRecipes.getOutput(input.getTankType());
        if (ref == null) {
            heavy.setTankType(Fluids.NONE);
            reformate.setTankType(Fluids.NONE);
            light.setTankType(Fluids.NONE);
            gas.setTankType(Fluids.NONE);
            return;
        }
        FluidStack[] stacks = {ref.getW(), ref.getX(), ref.getY(), ref.getZ()};
        FluidTankNTM[] outs = {heavy, reformate, light, gas};
        for (int i = 0; i < 4; i++) outs[i].setTankType(stacks[i].type);
        if (power < 10_000) return;
        if (input.getFill() < 100) return;
        for (int i = 0; i < 4; i++) {
            if (outs[i].getFill() + stacks[i].fill > outs[i].getMaxFill()) return;
        }
        isOn = true;
        power -= 10_000;
        input.setFill(input.getFill() - 100);
        for (int i = 0; i < 4; i++) outs[i].setFill(outs[i].getFill() + stacks[i].fill);
    }

    public DirPos[] getConPos() {
        return new DirPos[]{
                new DirPos(worldPosition.getX() + 2, worldPosition.getY(), worldPosition.getZ() + 1, Direction.EAST),
                new DirPos(worldPosition.getX() + 2, worldPosition.getY(), worldPosition.getZ() - 1, Direction.EAST),
                new DirPos(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() + 1, Direction.WEST),
                new DirPos(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 1, Direction.WEST),
                new DirPos(worldPosition.getX() + 1, worldPosition.getY(), worldPosition.getZ() + 2, Direction.SOUTH),
                new DirPos(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() + 2, Direction.SOUTH),
                new DirPos(worldPosition.getX() + 1, worldPosition.getY(), worldPosition.getZ() - 2, Direction.NORTH),
                new DirPos(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 2, Direction.NORTH),
        };
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(input);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(heavy, reformate, light, gas);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(input, heavy, reformate, light, gas);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        input.writeToNBT(tag, "input");
        heavy.writeToNBT(tag, "heavy");
        reformate.writeToNBT(tag, "reformate");
        light.writeToNBT(tag, "light");
        gas.writeToNBT(tag, "gas");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        input.readFromNBT(tag, "input");
        heavy.readFromNBT(tag, "heavy");
        reformate.readFromNBT(tag, "reformate");
        light.readFromNBT(tag, "light");
        gas.readFromNBT(tag, "gas");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(isOn);
        input.serialize(buf);
        heavy.serialize(buf);
        reformate.serialize(buf);
        light.serialize(buf);
        gas.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        isOn = buf.readBoolean();
        input.deserialize(buf);
        heavy.deserialize(buf);
        reformate.deserialize(buf);
        light.deserialize(buf);
        gas.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new VacuumDistillMenu(id, inv, this);
    }
}
