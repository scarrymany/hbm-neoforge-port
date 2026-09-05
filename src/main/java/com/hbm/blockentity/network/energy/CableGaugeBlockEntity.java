package com.hbm.blockentity.network.energy;

import com.hbm.api.energymk2.PowerNetMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code BlockCableGauge.TileEntityCableGauge} ({@code BlockCableGauge.java:118-168}).
 * Extends {@link CableBaseBlockEntity} (CE {@code TileEntityCableBaseNT}). OC callbacks not ported.
 * TESR needle ({@code RenderCableGauge}) not ported — ILookOverlay + ROR is the playable readout.
 */
public class CableGaugeBlockEntity extends CableBaseBlockEntity implements IRORValueProvider {

    private long deltaTick = 10;
    private long deltaSecond = 0;
    public long deltaLastSecond = 0;

    public CableGaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (level == null || level.isClientSide) return;

        // CE :131-141
        if (this.node != null && this.node.net != null) {
            PowerNetMK2 net = this.node.net;
            this.deltaTick = net.energyTracker;
            if (level.getGameTime() % 20 == 0) {
                this.deltaLastSecond = this.deltaSecond;
                this.deltaSecond = 0;
            }
            this.deltaSecond += deltaTick;
        }

        networkPackNT(25);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        // CE :148-151 — not a noisy machine, skip super (LoadedBase javadoc)
        buf.writeLong(deltaTick);
        buf.writeLong(deltaLastSecond);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        // CE :154-157
        this.deltaTick = Math.max(buf.readLong(), 0);
        this.deltaLastSecond = Math.max(buf.readLong(), 0);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE :160-162
        return new String[]{
                PREFIX_VALUE + "deltatick",
                PREFIX_VALUE + "deltasecond"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :165-168
        if ((PREFIX_VALUE + "deltatick").equals(name)) return "" + deltaTick;
        if ((PREFIX_VALUE + "deltasecond").equals(name)) return "" + deltaLastSecond;
        return null;
    }
}
