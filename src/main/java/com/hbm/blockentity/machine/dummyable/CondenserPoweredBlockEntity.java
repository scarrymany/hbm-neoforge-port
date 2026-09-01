package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityCondenserPowered} — 1M tanks, 10 HE/mB, 10M HE buffer.
 * Spin / particles skipped.
 */
public class CondenserPoweredBlockEntity extends CondenserBlockEntity implements IEnergyReceiverMK2 {

    public static final long MAX_POWER = 10_000_000L;
    public static final int POWER_PER_MB = 10;
    public long power;

    public CondenserPoweredBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1_000_000, 1_000_000, true);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (level == null || level.isClientSide) return;
        if (level.getGameTime() % 20 == 0) {
            for (Direction d : Direction.values()) trySubscribe(level, worldPosition.relative(d), d);
        }
    }

    @Override
    protected boolean extraCondition(int convert) {
        return power >= (long) convert * POWER_PER_MB;
    }

    @Override
    protected void postConvert(int convert) {
        power = Math.max(0, power - (long) convert * POWER_PER_MB);
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
}
