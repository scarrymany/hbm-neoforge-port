package com.hbm.blockentity.network;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.ExponentialMovingAverage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Read-only fill-level/throughput display duct, ported from CE's
 * {@code FluidDuctGauge$TileEntityPipeGauge} minus its OpenComputers integration (same deferral
 * rationale as {@link FluidCounterValveBlockEntity}). Keeps the Redstone-over-Radio half.
 */
public class PipeGaugeBlockEntity extends PipeBaseBlockEntity implements IRORValueProvider {

    private long deltaTick = 0;
    private long deltaSecond = 0;
    private long lastSecond = 0;
    private final ExponentialMovingAverage secondEMA = new ExponentialMovingAverage(0.05);

    public PipeGaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (level != null && !level.isClientSide) {
            if (node != null && node.net != null && getType() != Fluids.NONE) {
                deltaTick = node.net.fluidTracker;
                if (level.getGameTime() % 20L == 0) {
                    secondEMA.next(this.lastSecond = this.deltaSecond);
                    deltaSecond = 0;
                }
                deltaSecond += deltaTick;
            }
            networkPackNT(25);
        }
    }

    public long getDeltaTick() {
        return deltaTick;
    }

    public long getLastSecond() {
        return lastSecond;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(deltaTick);
        buf.writeLong(secondEMA.getValue());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        deltaTick = Math.max(buf.readLong(), 0);
        lastSecond = Math.max(buf.readLong(), 0);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] { PREFIX_VALUE + "deltatick", PREFIX_VALUE + "deltasecond" };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "deltatick").equals(name)) return String.valueOf(deltaTick);
        if ((PREFIX_VALUE + "deltasecond").equals(name)) return String.valueOf(lastSecond);
        return null;
    }
}
