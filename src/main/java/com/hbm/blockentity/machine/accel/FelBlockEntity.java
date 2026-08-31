package com.hbm.blockentity.machine.accel;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blockentity.machine.chem.SilexBlockEntity;
import com.hbm.blocks.machine.accel.FelBlock;
import com.hbm.inventory.container.machine.accel.FelMenu;
import com.hbm.items.machine.ItemFELCrystal;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code TileEntityFEL.java}: maxPower 2e9, powerReq 1000 * 4^ordinal.
 * Scans facing axis, sets {@link SilexBlockEntity#setLaserMode}.
 */
public class FelBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider {

    private static final int SLOT_BATTERY = 0;
    private static final int SLOT_CRYSTAL = 1;
    public static final long MAX_POWER = 2_000_000_000L;
    public static final int POWER_REQ = 1000;
    public static final int RANGE = 24;

    public long power;
    public EnumWavelengths mode = EnumWavelengths.NULL;
    public int distance = RANGE;
    public boolean isOn;

    public FelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, true, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineFEL");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return Library.isBattery(stack);
        return slot == SLOT_CRYSTAL && stack.getItem() instanceof ItemFELCrystal;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, MAX_POWER);
        trySubscribe(level, worldPosition.relative(getFacing().getOpposite()), getFacing().getOpposite());

        ItemStack crystal = inventory.getStackInSlot(SLOT_CRYSTAL);
        if (!crystal.isEmpty() && crystal.getItem() instanceof ItemFELCrystal fel) {
            mode = fel.getWavelength();
            isOn = true;
        } else {
            mode = EnumWavelengths.NULL;
            isOn = false;
        }

        long cost = powerCost();
        if (isOn && mode != EnumWavelengths.NULL && power >= cost) {
            power -= cost;
            fireBeam();
        } else {
            mode = EnumWavelengths.NULL;
            distance = RANGE;
        }

        dataChanged();
        networkPackMK2(100);
    }

    private long powerCost() {
        if (mode == EnumWavelengths.NULL || mode.ordinal() == 0) return 0;
        return (long) (POWER_REQ * Math.pow(4, mode.ordinal()));
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(FelBlock.FACING) ? state.getValue(FelBlock.FACING) : Direction.NORTH;
    }

    private void fireBeam() {
        Direction dir = getFacing();
        distance = RANGE;
        for (int i = 2; i < RANGE; i++) {
            BlockPos hit = worldPosition.relative(dir, i).above();
            SilexBlockEntity silex = findSilex(level, hit);
            if (silex != null) {
                silex.setLaserMode(mode);
                distance = i;
                return;
            }
            BlockState b = level.getBlockState(hit);
            if (!b.isAir() && b.canOcclude()) {
                distance = i;
                return;
            }
        }
    }

    public static EnumWavelengths laserHitting(Level level, BlockPos silexPos) {
        if (level == null) return EnumWavelengths.NULL;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int i = 2; i < RANGE; i++) {
                BlockPos p = silexPos.relative(dir, i).below();
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof FelBlockEntity fel && fel.mode != EnumWavelengths.NULL
                        && fel.getFacing() == dir.getOpposite()) {
                    return fel.mode;
                }
            }
        }
        return EnumWavelengths.NULL;
    }

    private static SilexBlockEntity findSilex(Level level, BlockPos around) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockEntity be = level.getBlockEntity(around.offset(dx, dy, dz));
                    if (be instanceof SilexBlockEntity silex) return silex;
                }
            }
        }
        return null;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long i) {
        power = i;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("distance", distance);
        tag.putString("mode", mode.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        distance = tag.getInt("distance");
        try {
            mode = EnumWavelengths.valueOf(tag.getString("mode"));
        } catch (IllegalArgumentException ignored) {
            mode = EnumWavelengths.NULL;
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(distance);
        buf.writeUtf(mode.name());
        buf.writeBoolean(isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        distance = buf.readInt();
        try {
            mode = EnumWavelengths.valueOf(buf.readUtf());
        } catch (IllegalArgumentException ignored) {
            mode = EnumWavelengths.NULL;
        }
        isOn = buf.readBoolean();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FelMenu(containerId, playerInventory, this);
    }
}
