package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.api.rbmk.IRBMKFluxReceiver;
import com.hbm.api.rbmk.IRBMKLoadable;
import com.hbm.api.rbmk.RBMKDials;
import com.hbm.handler.neutron.NeutronStream;
import com.hbm.handler.neutron.RBMKNeutronHandler;
import com.hbm.inventory.container.machine.rbmk.RBMKOutgasserMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.lib.DirPos;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Outgasser column. Exact CE {@code TileEntityRBMKOutgasser.java:68-140}: flux efficiency
 * {@code min(1 - fluxRatio * 0.8, 1)} × {@code getOutgasserMod}, progress in {@code receiveFlux},
 * {@code lastUsedFlux} per-tick accumulate, item-change progress reset, neutron-activate fallback,
 * default {@code getConPos} column-top + below. {@code rbmk_loader} branches stay skipped.
 * <p>
 * Recipe table: {@link OutgasserRecipes} (CE {@code :145-187}).
 */
public class RBMKOutgasserBlockEntity extends RBMKSlottedBlockEntity implements IRBMKFluxReceiver, IFluidStandardSenderMK2, IRBMKLoadable, MenuProvider {

    public final FluidTankNTM gas;
    public double progress = 0;
    public int duration = 10_000;
    public double lastUsedFlux = 0;
    private long lastFluxTick = -1;
    private ItemStack previousStack = ItemStack.EMPTY;

    public RBMKOutgasserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2);
        gas = new FluidTankNTM(Fluids.TRITIUM, 64_000).withOwner(this);
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RBMKOutgasserMenu(containerId, playerInventory, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkOutgasser");
    }

    // CE TileEntityRBMKOutgasser.java:117-140
    @Override
    public void receiveFlux(NeutronStream stream) {
        double efficiency = Math.min(1 - stream.fluxRatio * 0.8, 1);

        if (canProcess()) {
            double usedFlux = stream.fluxQuantity * efficiency
                    * (level instanceof ServerLevel serverLevel ? RBMKDials.getOutgasserMod(serverLevel) : 1D);
            progress += usedFlux;

            long now = level != null ? level.getGameTime() : 0L;
            if (now != lastFluxTick) {
                lastFluxTick = now;
                lastUsedFlux = 0;
            }
            lastUsedFlux += usedFlux;

            if (progress > duration) {
                process();
                setChanged();
            }
        } else if (!inventory.getStackInSlot(0).isEmpty()) {
            ContaminationUtil.neutronActivateItem(
                    inventory.getStackInSlot(0),
                    (float) (stream.fluxQuantity * efficiency * 0.001),
                    1F);
            setChanged();
        }
    }

    public boolean canProcess() {
        if (inventory.getStackInSlot(0).isEmpty()) return false;
        OutgasserRecipes.OutgasserRecipe output = OutgasserRecipes.getRecipe(inventory.getStackInSlot(0));
        if (output == null || output.fusionOnly) return false;

        FluidStack fluid = output.fluidOutput;
        if (fluid != null) {
            if (gas.getTankType() != fluid.type && gas.getFill() > 0) return false;
            gas.setTankType(fluid.type);
            if (gas.getFill() + fluid.fill > gas.getMaxFill()) return false;
        }

        ItemStack out = output.solidOutput;
        if (out == null || inventory.getStackInSlot(1).isEmpty()) return true;
        ItemStack leftover = inventory.insertItem(1, out.copy(), true);
        return leftover.isEmpty();
    }

    private void process() {
        OutgasserRecipes.OutgasserRecipe output = OutgasserRecipes.getRecipe(inventory.getStackInSlot(0));
        inventory.extractItem(0, 1, false);
        this.progress = 0;
        if (output == null) return;
        if (output.fluidOutput != null) {
            gas.setFill(gas.getFill() + output.fluidOutput.fill);
        }
        if (output.solidOutput != null) {
            inventory.insertItem(1, output.solidOutput.copy(), false);
        }
    }

    // implements IRBMKFluxReceiver.canReceiveFlux() - CE: TileEntityRBMKOutgasser#canProcess(), per that interface's own javadoc
    @Override
    public boolean canReceiveFlux() {
        return canProcess();
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide) {
            // CE :71-73
            if (level.getGameTime() != lastFluxTick) {
                lastUsedFlux = 0;
            }
            // CE :75-77 — reset when unprocessable or input item changed
            if (!canProcess() || !ItemStack.isSameItem(previousStack, inventory.getStackInSlot(0))) {
                this.progress = 0;
            }

            if (this.gas.getFill() > 0) {
                for (DirPos pos : getConPos()) {
                    tryProvide(gas, level, pos);
                }
            }
            previousStack = inventory.getStackInSlot(0).copy();
        }

        super.updateEntity();
    }

    /** CE {@code :108-113} default (no {@code rbmk_loader}). */
    public DirPos[] getConPos() {
        int height = level instanceof ServerLevel serverLevel ? RBMKDials.getColumnHeight(serverLevel) : 0;
        return new DirPos[]{
                new DirPos(worldPosition.getX(), worldPosition.getY() + height + 1, worldPosition.getZ(), Direction.UP),
                new DirPos(worldPosition.getX(), worldPosition.getY() - 1, worldPosition.getZ(), Direction.DOWN)
        };
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
        // CE :260-262
        return toLoad != null && inventory.insertItem(0, toLoad.copy(), true).isEmpty();
    }

    @Override
    public void load(ItemStack toLoad) {
        inventory.insertItem(0, toLoad.copy(), false);
        setChanged();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot != 0) return false;
        OutgasserRecipes.OutgasserRecipe recipe = OutgasserRecipes.getRecipe(stack);
        return recipe != null && !recipe.fusionOnly;
    }

    @Override
    public boolean canUnload() {
        return !inventory.getStackInSlot(1).isEmpty();
    }

    @Override
    public ItemStack provideNext() {
        return inventory.getStackInSlot(1);
    }

    @Override
    public void unload() {
        inventory.setStackInSlot(1, ItemStack.EMPTY);
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
