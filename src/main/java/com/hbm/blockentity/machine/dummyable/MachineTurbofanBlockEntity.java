package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.TurbofanMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm.inventory.recipes.EngineRecipes;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
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
 * CE {@code TileEntityMachineTurbofan}: AERO fuel, AFTERBURN upgrade. Pollution/particles skipped.
 * {@code setType(4)} / {@code loadTank(0,1)} Exact CE {@code TileEntityMachineTurbofan.java:156-157}.
 */
public class MachineTurbofanBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, IFluidStandardReceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 1_000_000;

    public final FluidTankNTM tank;
    public long power;
    public boolean wasOn;
    public int afterburner;

    public MachineTurbofanBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, true, true);
        this.tank = new FluidTankNTM(Fluids.KEROSENE, 24_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineTurbofan");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 3) return Library.isBattery(stack);
        if (slot == 2) return stack.getItem() instanceof ItemMachineUpgrade;
        if (slot == 4) return stack.getItem() instanceof IItemFluidIdentifier;
        return slot == 0;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 1 || slot == 3;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 3};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        // CE TileEntityMachineTurbofan.java:156-157
        tank.setType(4, inventory);
        tank.loadTank(0, 1, inventory);

        afterburner = 0;
        ItemStack up = inventory.getStackInSlot(2);
        if (up.getItem() instanceof ItemMachineUpgrade u && u.getType() == UpgradeType.AFTERBURN) {
            afterburner = Math.min(u.getTier(), 3);
        }

        wasOn = false;
        int amount = 1 + afterburner;
        int burn = Math.min(amount, tank.getFill());
        long burnValue = 0;
        FT_Combustible trait = tank.getTankType().getTrait(FT_Combustible.class);
        if (trait != null && trait.getGrade() == FuelGrade.AERO) {
            burnValue = trait.getCombustionEnergy() / 1_000L;
        } else if (EngineRecipes.getEnergy(tank.getTankType()) > 0 && trait != null && trait.getGrade() == FuelGrade.AERO) {
            burnValue = EngineRecipes.getEnergy(tank.getTankType()) / 1_000L;
        }

        boolean redstone = false;
        for (DirPos pos : getConPos()) {
            if (level.hasNeighborSignal(pos.getPos())) {
                redstone = true;
                break;
            }
        }

        if (!redstone && burn > 0 && burnValue > 0) {
            tank.setFill(tank.getFill() - burn);
            long out = (long) (burnValue * burn * (1 + Math.min(afterburner / 3D, 4)));
            power = Math.min(MAX_POWER, power + out);
            wasOn = true;
        }

        power = Library.chargeItemsFromTE(inventory, 3, power, MAX_POWER);

        for (DirPos pos : getConPos()) {
            tryProvide(level, pos.getPos(), pos.getDir());
            trySubscribe(tank.getTankType(), level, pos);
        }
        dataChanged();
        networkPackMK2(50);
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        return new DirPos[]{
                new DirPos(worldPosition.relative(rot, 3), rot),
                new DirPos(worldPosition.relative(rot.getOpposite(), 3), rot.getOpposite()),
        };
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
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
        return List.of(tank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tank.writeToNBT(tag, "t0");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        tank.readFromNBT(tag, "t0");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(wasOn);
        buf.writeInt(afterburner);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        wasOn = buf.readBoolean();
        afterburner = buf.readInt();
        tank.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new TurbofanMenu(id, inv, this);
    }
}
