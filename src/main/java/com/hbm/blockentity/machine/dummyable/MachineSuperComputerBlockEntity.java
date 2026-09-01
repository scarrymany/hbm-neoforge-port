package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.SuperComputerMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidIdentifier;
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
 * CE {@code TileEntityMachineSuperComputer} — 8 slots, 2×4k tanks, 100k HE buffer.
 * Drive/blueprint GenericRecipe table skipped (EnumDriveType items unregistered).
 */
public class MachineSuperComputerBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 100_000;

    public final FluidTankNTM input;
    public final FluidTankNTM output;
    public long power;

    public MachineSuperComputerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 8, true, true);
        this.input = new FluidTankNTM(Fluids.NONE, 4_000).withOwner(this);
        this.output = new FluidTankNTM(Fluids.NONE, 4_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineSuperComputer");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 1) return true;
        if (slot >= 5) return false;
        return slot >= 2 && slot <= 4;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= 5 && slot <= 7;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{2, 3, 4, 5, 6, 7};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        power = Library.chargeTEFromItems(inventory, 0, power, MAX_POWER);

        ItemStack id = inventory.getStackInSlot(1);
        if (!id.isEmpty() && id.getItem() instanceof IItemFluidIdentifier ident) {
            input.setTankType(ident.getType(level, worldPosition, id));
        }

        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) {
                trySubscribe(level, pos);
                trySubscribe(input.getTankType(), level, pos);
            }
        }
        for (DirPos pos : getConPos()) {
            if (output.getFill() > 0) tryProvide(output, level, pos);
        }

        dataChanged();
        networkPackMK2(25);
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        return new DirPos[]{
                new DirPos(worldPosition.relative(dir, 9), dir),
                new DirPos(worldPosition.relative(dir, 7).relative(rot, 2), rot),
                new DirPos(worldPosition.relative(dir, 7).relative(rot, -2), rot.getOpposite()),
                new DirPos(worldPosition.relative(dir, 5).relative(rot, 2), rot),
                new DirPos(worldPosition.relative(dir, 5).relative(rot, -2), rot.getOpposite()),
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
        return List.of(input);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(output);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(input, output);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        input.writeToNBT(tag, "in");
        output.writeToNBT(tag, "out");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        input.readFromNBT(tag, "in");
        output.readFromNBT(tag, "out");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        input.serialize(buf);
        output.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        input.deserialize(buf);
        output.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SuperComputerMenu(id, inv, this);
    }
}
