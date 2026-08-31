package com.hbm.blockentity.machine.accel;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.machine.accel.PaPartBlock;
import com.hbm.inventory.container.machine.accel.PaPartMenu;
import com.hbm.items.machine.ItemPACoil;
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
import org.jetbrains.annotations.Nullable;

/**
 * Shared PA part TE. CE {@code TileEntityPASource}/{@code PARFC}/{@code PADipole}/{@code PAQuadrupole}:
 * battery + coil. Beamline is inventory-less energy pass-through.
 */
public class PaPartBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 1_000_000L;

    public final PaPartBlock.Kind kind;
    public long power;

    public PaPartBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, PaPartBlock.Kind kind) {
        super(type, pos, state, kind == PaPartBlock.Kind.BEAMLINE ? 0 : 2, true, true);
        this.kind = kind;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(switch (kind) {
            case RFC -> "container.paRFC";
            case QUADRUPOLE -> "container.paQuadrupole";
            case DIPOLE -> "container.paDipole";
            case SOURCE -> "container.paSource";
            default -> "container.paBeamline";
        });
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (kind == PaPartBlock.Kind.BEAMLINE) return false;
        if (slot == 0) return Library.isBattery(stack);
        return slot == 1 && stack.getItem() instanceof ItemPACoil;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        if (kind != PaPartBlock.Kind.BEAMLINE) {
            power = Library.chargeTEFromItems(inventory, 0, power, MAX_POWER);
        }
        for (Direction dir : Direction.values()) {
            trySubscribe(level, worldPosition.relative(dir), dir);
        }
        dataChanged();
        networkPackMK2(50);
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (kind == PaPartBlock.Kind.BEAMLINE) return null;
        return new PaPartMenu(containerId, playerInventory, this);
    }
}
