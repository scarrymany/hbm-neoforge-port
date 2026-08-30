package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.api.rbmk.IRBMKFluxReceiver;
import com.hbm.api.rbmk.IRBMKLoadable;
import com.hbm.handler.neutron.NeutronStream;
import com.hbm.handler.neutron.RBMKNeutronHandler;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Outgasser column - absorbs flux (like a rod) into an inserted item over {@link #duration} ticks
 * worth of accumulated flux, producing tritium gas. Ported (simplified fill logic; tank/duration
 * constants CE-confirmed) from CE's {@code TileEntityRBMKOutgasser} (391 lines, signature-level
 * survey). {@code receiveFlux} terminates the stream exactly like a fuel rod - see CE's
 * {@code RBMKNeutronHandler.RBMKNeutronStream.runStreamInteraction}'s {@code OUTGASSER} branch
 * (forward reference).
 * <p>
 * CE's real recipe-driven item processing ({@code OutgasserRecipes}/{@code RecipesCommon}, and the
 * {@code ContaminationUtil} call when nothing is loaded) are out of scope per the research report's
 * own Deferred Scope - the item slot here accepts anything and is never consumed, matching CE's
 * fallback "item just sits there generating gas" case rather than a real recipe lookup.
 */
public class RBMKOutgasserBlockEntity extends RBMKSlottedBlockEntity implements IRBMKFluxReceiver, IFluidStandardSenderMK2, IRBMKLoadable {

    public final FluidTankNTM gas;
    public double progress = 0;
    public int duration = 10_000;
    public double lastUsedFlux = 0;
    public double fluxQuantity;

    public RBMKOutgasserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
        gas = new FluidTankNTM(Fluids.TRITIUM, 64_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkOutgasser");
    }

    // implements IRBMKFluxReceiver.receiveFlux(NeutronStream) - CE: TileEntityRBMKOutgasser.receiveFlux
    @Override
    public void receiveFlux(NeutronStream stream) {
        this.fluxQuantity += stream.fluxQuantity;
    }

    public boolean canProcess() {
        return !inventory.getStackInSlot(0).isEmpty() && gas.getFill() < gas.getMaxFill();
    }

    // implements IRBMKFluxReceiver.canReceiveFlux() - CE: TileEntityRBMKOutgasser#canProcess(), per that interface's own javadoc
    @Override
    public boolean canReceiveFlux() {
        return canProcess();
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide) {
            this.lastUsedFlux = this.fluxQuantity;

            if (canProcess() && fluxQuantity > 0) {
                progress += fluxQuantity;
                if (progress >= duration) {
                    int batches = (int) (progress / duration);
                    progress -= batches * duration;
                    gas.setFill(Math.min(gas.getMaxFill(), gas.getFill() + batches * 100));
                }
            }
            this.fluxQuantity = 0;

            tryProvide(gas, level, worldPosition.above(), Direction.UP);
        }

        super.updateEntity();
    }

    @Override
    public void onMelt(int reduce) {
        for (int i = 0; i < 2; i++) spawnDebris("BLANK");
        standardMelt(reduce);
    }

    @Override
    public RBMKNeutronHandler.RBMKType getRBMKType() {
        return RBMKNeutronHandler.RBMKType.OUTGASSER;
    }

    @Override
    public RBMKColumn.ColumnType getConsoleType() {
        return RBMKColumn.ColumnType.OUTGASSER;
    }

    @Override
    public RBMKColumn getConsoleData() {
        RBMKColumn.OutgasserColumn data = (RBMKColumn.OutgasserColumn) super.getConsoleData();
        data.gas = gas.getFill();
        data.maxGas = gas.getMaxFill();
        data.progress = progress;
        data.maxProgress = duration;
        data.usedFlux = lastUsedFlux;
        return data;
    }

    @Override
    public boolean canLoad(ItemStack toLoad) {
        return !toLoad.isEmpty() && inventory.getStackInSlot(0).isEmpty();
    }

    @Override
    public void load(ItemStack toLoad) {
        inventory.setStackInSlot(0, toLoad.copy());
        setChanged();
    }

    @Override
    public boolean canUnload() {
        return !inventory.getStackInSlot(0).isEmpty();
    }

    @Override
    public ItemStack provideNext() {
        return inventory.getStackInSlot(0);
    }

    @Override
    public void unload() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(gas);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(gas);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("progress", progress);
        tag.putInt("duration", duration);
        gas.writeToNBT(tag, "gas");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getDouble("progress");
        if (tag.contains("duration")) duration = tag.getInt("duration");
        gas.readFromNBT(tag, "gas");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(lastUsedFlux);
        buf.writeDouble(progress);
        gas.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        lastUsedFlux = buf.readDouble();
        progress = buf.readDouble();
        gas.deserialize(buf);
    }
}
