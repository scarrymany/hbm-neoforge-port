package com.hbm.blockentity.network;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.network.FluidCounterValveBlock;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Redstone-gated fluid-throughput counter, ported from CE's
 * {@code com.hbm.tileentity.network.TileEntityFluidCounterValve} minus its OpenComputers integration
 * ({@code SimpleComponent}/{@code CompatHandler.OCComponent}) - deferred per
 * {@code docs/phase2/network_fluid_ducts.md}'s Deferred scope, which recommends deferring/stubbing
 * third-party mod-integration surfaces not otherwise established for this port. The Redstone-over-Radio
 * ({@link IRORValueProvider}/{@link IRORInteractive}) half is kept - both interfaces are already fully
 * ported in this port with no forward-reference gaps.
 */
public class FluidCounterValveBlockEntity extends PipeBaseBlockEntity implements IRORValueProvider, IRORInteractive {

    private long counter;

    public FluidCounterValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (level != null && !level.isClientSide) {
            updateCounter();
            networkPackNT(25);
        }
    }

    private void updateCounter() {
        if (node != null && node.net != null && getType() != Fluids.NONE) {
            counter += node.net.fluidTracker;
        }
    }

    @Override
    public boolean shouldCreateNode() {
        return getBlockState().getValue(FluidCounterValveBlock.ACTIVE);
    }

    /** Called by the block after its {@link FluidCounterValveBlock#ACTIVE} state flips. */
    public void updateState() {
        if (level == null || level.isClientSide) return;
        if (!getBlockState().getValue(FluidCounterValveBlock.ACTIVE) && this.node != null) {
            UniNodespace.destroyNode(level, node);
            this.node = null;
        }
    }

    public long getCounter() {
        return counter;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        counter = tag.getLong("counter");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("counter", counter);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(counter);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.counter = Math.max(buf.readLong(), 0);
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "value").equals(name)) return String.valueOf(counter);
        if ((PREFIX_VALUE + "state").equals(name)) return getBlockState().getValue(FluidCounterValveBlock.ACTIVE) ? "1" : "0";
        return null;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] { PREFIX_VALUE + "value", PREFIX_VALUE + "state", PREFIX_FUNCTION + "reset",
                PREFIX_FUNCTION + "setState" + NAME_SEPARATOR + "state" };
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if (name.equals(PREFIX_FUNCTION + "reset")) {
            updateCounter();
            counter = 0;
            setChanged();
        } else if (name.equals(PREFIX_FUNCTION + "setState") && params.length > 0 && level != null) {
            boolean active = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            level.setBlock(worldPosition, getBlockState().setValue(FluidCounterValveBlock.ACTIVE, active), 2);
            updateState();
        }
        return null;
    }
}
