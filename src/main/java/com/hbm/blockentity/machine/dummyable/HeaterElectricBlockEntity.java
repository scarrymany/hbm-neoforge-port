package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.HeaterElectricMenu;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityHeaterElectric.java}:47-61 — HE in, heat out.
 * consumption = (long)(pow(setting, 1.4) * 200), heat = setting * 100, buffer = consumption * 20.
 */
public class HeaterElectricBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IHeatSource, ITickableBE, MenuProvider {

    public long power;
    public int heatEnergy;
    public boolean isOn;
    public int setting = 1;

    public HeaterElectricBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heaterElectric");
    }

    public long getConsumption() {
        return (long) (Math.pow(setting, 1.4D) * 200D);
    }

    public int getHeatGen() {
        return setting * 100;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) trySubscribe(level, pos);
        }

        heatEnergy = (int) (heatEnergy * 0.999D);
        tryPullHeat();

        isOn = false;
        long cons = getConsumption();
        if (setting > 0 && power >= cons) {
            power -= cons;
            heatEnergy += getHeatGen();
            isOn = true;
        }

        dataChanged();
        networkPackMK2(25);
    }

    private void tryPullHeat() {
        if (level == null) return;
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below instanceof IHeatSource source && below != this) {
            int stored = source.getHeatStored();
            if (stored > 0) {
                heatEnergy += (int) (stored * 0.85D);
                source.useUpHeat(stored);
            }
        }
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        return new DirPos[]{new DirPos(worldPosition.relative(dir, 3), dir)};
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
    }

    public void bumpSetting(int delta) {
        setting = Math.max(0, Math.min(10, setting + delta));
        setChanged();
    }

    @Override
    public int getHeatStored() {
        return heatEnergy;
    }

    @Override
    public void useUpHeat(int heat) {
        heatEnergy = Math.max(heatEnergy - Math.max(0, heat), 0);
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
        return Math.max(getConsumption() * 20L, 200L);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("heat", heatEnergy);
        tag.putInt("set", setting);
        tag.putBoolean("on", isOn);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        heatEnergy = tag.getInt("heat");
        setting = tag.getInt("set");
        isOn = tag.getBoolean("on");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(heatEnergy);
        buf.writeInt(setting);
        buf.writeBoolean(isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        heatEnergy = buf.readInt();
        setting = buf.readInt();
        isOn = buf.readBoolean();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new HeaterElectricMenu(id, inv, this);
    }
}
