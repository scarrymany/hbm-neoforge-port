package com.hbm.blockentity.machine.chem;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.container.machine.chem.ChemPlantMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.chem.ChemPlantRecipes;
import com.hbm.inventory.recipes.chem.ChemPlantRecipes.ChemPlantRecipe;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ported from CE's {@code TileEntityMachineChemicalPlant} (single-module chemical plant, distinct
 * from the 4-module {@code TileEntityMachineChemicalFactory} - not ported this pass, see
 * {@code docs/phase2/machines_chemical_isotope.md}'s Chemical Plant section and this task's own
 * per-machine list, which names "chemical plant" only). Delegates all recipe data to
 * {@link ChemPlantRecipes}; see that class's header for the documented auto-recognition model
 * (this port matches by current input contents, not CE's player-selected-by-name GUI dropdown) and
 * scope trim (representative recipe subset).
 * <p>
 * 3 item input / 3 item output slots, 3x24000mB fluid input tanks / 3x24000mB fluid output tanks -
 * matching CE's per-module tank count and capacity, renumbered inventory (no fluid-ID/canister slots,
 * same pre-existing gap as every other machine in this pass).
 */
public class ChemPlantBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, IPersistentNBT, MenuProvider {

    private static final int ITEM_IN_START = 0;
    private static final int ITEM_OUT_START = 3;
    private static final int BATTERY_SLOT = 6;

    public static final long MIN_MAX_POWER = 100_000L;
    public static final int TANK_CAPACITY = 24_000;

    public final FluidTankNTM[] inputTanks = new FluidTankNTM[3];
    public final FluidTankNTM[] outputTanks = new FluidTankNTM[3];

    public long power;
    public long maxPower = MIN_MAX_POWER;
    public int progress;
    public boolean isProcessing;
    private String activeRecipeName;

    /**
     * Input tanks are fixed to one real fluid each rather than starting {@link Fluids#NONE} - same
     * reasoning as {@code GasCentrifugeBlockEntity}'s tank: {@code IFluidStandardReceiverMK2}'s demand
     * check requires {@code tank.getTankType() == type} already, so a {@code NONE}-typed tank can
     * never receive anything through the pipe network without CE's dropped item-identifier retyping
     * mechanic. The three fixed types ({@link Fluids#WATER}, {@link Fluids#AIR}, {@link Fluids#LAVA})
     * cover every fluid input this pass's {@link ChemPlantRecipes} subset needs.
     */
    private static final FluidType[] FIXED_INPUT_TYPES = {Fluids.WATER, Fluids.AIR, Fluids.LAVA};

    public ChemPlantBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 7, true, true);
        for (int i = 0; i < 3; i++) {
            inputTanks[i] = new FluidTankNTM(FIXED_INPUT_TYPES[i], TANK_CAPACITY).withOwner(this);
            outputTanks[i] = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineChemicalPlant");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) return Library.isBattery(stack);
        return slot >= ITEM_IN_START && slot < ITEM_IN_START + 3;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= ITEM_OUT_START && slot < ITEM_OUT_START + 3;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5};
    }

    public int getProgressScaled(int i) {
        ChemPlantRecipe recipe = findRecipe();
        int duration = recipe == null ? 1 : recipe.duration;
        return (progress * i) / Math.max(1, duration);
    }

    private ChemPlantRecipe findRecipe() {
        for (ChemPlantRecipe recipe : ChemPlantRecipes.RECIPES) {
            if (matchesItems(recipe) && matchesFluids(recipe)) return recipe;
        }
        return null;
    }

    private boolean matchesItems(ChemPlantRecipe recipe) {
        for (int i = 0; i < recipe.inputItems.length; i++) {
            AStack key = recipe.inputItems[i];
            if (i >= 3 || !key.matchesRecipe(inventory.getStackInSlot(ITEM_IN_START + i), false)) return false;
        }
        return true;
    }

    /** Searches by fluid type across all 3 input tanks, not by index - tanks are fixed to one type each (see constructor javadoc), not positionally assigned per recipe. */
    private FluidTankNTM findInputTank(FluidType type) {
        for (FluidTankNTM tank : inputTanks) {
            if (tank.getTankType() == type) return tank;
        }
        return null;
    }

    private boolean matchesFluids(ChemPlantRecipe recipe) {
        for (FluidStack need : recipe.inputFluids) {
            FluidTankNTM tank = findInputTank(need.type);
            if (tank == null || tank.getFill() < need.fill) return false;
        }
        return true;
    }

    private boolean hasOutputSpace(ChemPlantRecipe recipe) {
        for (int i = 0; i < recipe.outputItems.length && i < 3; i++) {
            if (!inventory.insertItem(ITEM_OUT_START + i, recipe.outputItems[i], true).isEmpty()) return false;
        }
        if (recipe.outputFluid != null) {
            boolean placed = false;
            for (FluidTankNTM tank : outputTanks) {
                if ((tank.getTankType() == recipe.outputFluid.type || tank.getTankType() == Fluids.NONE)
                        && tank.getFill() + recipe.outputFluid.fill <= tank.getMaxFill()) {
                    placed = true;
                    break;
                }
            }
            if (!placed) return false;
        }
        return true;
    }

    private void process(ChemPlantRecipe recipe) {
        for (int i = 0; i < recipe.inputItems.length && i < 3; i++) {
            inventory.extractItem(ITEM_IN_START + i, recipe.inputItems[i].count(), false);
        }
        for (FluidStack need : recipe.inputFluids) {
            FluidTankNTM tank = findInputTank(need.type);
            if (tank != null) tank.setFill(tank.getFill() - need.fill);
        }
        for (int i = 0; i < recipe.outputItems.length && i < 3; i++) {
            inventory.insertItem(ITEM_OUT_START + i, recipe.outputItems[i].copy(), false);
        }
        if (recipe.outputFluid != null) {
            for (FluidTankNTM tank : outputTanks) {
                if (tank.getTankType() == recipe.outputFluid.type || tank.getTankType() == Fluids.NONE) {
                    tank.setTankType(recipe.outputFluid.type);
                    tank.setFill(tank.getFill() + recipe.outputFluid.fill);
                    break;
                }
            }
        }
    }

    public DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX() + 2, p.getY(), p.getZ(), Direction.EAST),
                new DirPos(p.getX() - 2, p.getY(), p.getZ(), Direction.WEST),
                new DirPos(p.getX(), p.getY(), p.getZ() + 2, Direction.SOUTH),
                new DirPos(p.getX(), p.getY(), p.getZ() - 2, Direction.NORTH)
        };
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, maxPower);

        for (DirPos dp : getConPos()) {
            trySubscribe(level, dp);
            for (FluidTankNTM tank : inputTanks) if (tank.getTankType() != Fluids.NONE) trySubscribe(tank.getTankType(), level, dp);
            for (FluidTankNTM tank : outputTanks) if (tank.getFill() > 0) tryProvide(tank, level, dp);
        }

        ChemPlantRecipe recipe = findRecipe();
        if (recipe == null) {
            progress = 0;
            isProcessing = false;
            activeRecipeName = null;
            maxPower = Math.max(power, MIN_MAX_POWER);
        } else {
            activeRecipeName = recipe.name;
            maxPower = Math.max(Math.max(power, recipe.power * 100), MIN_MAX_POWER);

            if (power >= recipe.power && hasOutputSpace(recipe)) {
                isProcessing = true;
                progress++;
                power -= recipe.power;

                if (progress >= recipe.duration) {
                    process(recipe);
                    progress = 0;
                }
            } else {
                isProcessing = false;
            }
        }

        // CE: TileEntityMachineChemicalPlant.getLoopedSound() - a continuous AudioWrapper loop
        // (HBMSoundHandler.chemicalPlant, 20-tick keepAlive) while processing. This port has no
        // looped-block-audio bridge yet (documented gap, see MachineRefineryBlockEntity's javadoc);
        // substituted with a periodic broadcast every 20 ticks while active, matching EntityMeteor's
        // established stand-in pattern for the same missing infra.
        if (isProcessing && level.getGameTime() % 20 == 0) {
            level.playSound(null, worldPosition, HBMSoundHandler.chemicalPlant.get(), SoundSource.BLOCKS, 1F, 1.0F);
        }

        dataChanged();
        networkPackMK2(100);
    }

    public String getActiveRecipeName() {
        return activeRecipeName;
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
        return maxPower;
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(inputTanks);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(outputTanks);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(inputTanks[0], inputTanks[1], inputTanks[2], outputTanks[0], outputTanks[1], outputTanks[2]);
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putLong("maxPower", maxPower);
        tag.putInt("progress", progress);
        for (int i = 0; i < 3; i++) {
            inputTanks[i].writeToNBT(tag, "i" + i);
            outputTanks[i].writeToNBT(tag, "o" + i);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        maxPower = Math.max(tag.getLong("maxPower"), MIN_MAX_POWER);
        progress = tag.getInt("progress");
        for (int i = 0; i < 3; i++) {
            inputTanks[i].readFromNBT(tag, "i" + i);
            outputTanks[i].readFromNBT(tag, "o" + i);
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.serialize(buf);
        for (FluidTankNTM tank : outputTanks) tank.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeBoolean(isProcessing);
        buf.writeInt(progress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.deserialize(buf);
        for (FluidTankNTM tank : outputTanks) tank.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        isProcessing = buf.readBoolean();
        progress = buf.readInt();
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        for (int i = 0; i < 3; i++) {
            inputTanks[i].writeToNBT(nbt, "ni" + i);
            outputTanks[i].writeToNBT(nbt, "no" + i);
        }
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        for (int i = 0; i < 3; i++) {
            inputTanks[i].readFromNBT(nbt, "ni" + i);
            outputTanks[i].readFromNBT(nbt, "no" + i);
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ChemPlantMenu(containerId, playerInventory, this);
    }
}
