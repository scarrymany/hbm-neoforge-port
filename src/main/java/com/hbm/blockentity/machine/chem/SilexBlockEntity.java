package com.hbm.blockentity.machine.chem;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.container.machine.chem.SilexMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.chem.SILEXRecipes;
import com.hbm.inventory.recipes.chem.SILEXRecipes.SILEXRecipe;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.util.WeightedRandom;
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

import java.util.List;

/**
 * Ported from CE's {@code TileEntitySILEX} - laser-gated, weighted-random single-output isotope/
 * element separation ({@code docs/phase2/machines_chemical_isotope.md}'s SILEX section). The
 * <b>exact</b> isotope-separation formula is preserved verbatim in {@link #process()}:
 * {@code progress += Math.pow(2, mode.ordinal() - recipe.laserStrength.ordinal() + 1) / 2} - every
 * wavelength tier above the recipe's minimum required strength doubles the per-tick progress
 * increment.
 * <p>
 * <b>Scope trims from CE</b> (documented):
 * <ul>
 *   <li>No item-container fluid loading (canister/gas-icon item slots 2-3 in CE) - same pre-existing
 *   gap as every other machine in this area (see {@code MachineRefineryBlockEntity}'s javadoc). The
 *   acid tank fills only through the pipe network.</li>
 *   <li>No direct-fluid-input reprocessing path (CE additionally lets {@code UF6}/{@code PUF6}/
 *   {@code DEATH} fed straight into the tank convert directly to material charge, bypassing the item
 *   slot). Only the solid-feedstock-via-item-slot path (the mechanic actually described in the task's
 *   research doc) is ported; none of this pass's ported {@link SILEXRecipes} entries need the
 *   fluid-direct path anyway.</li>
 *   <li>SILEX has no HE power requirement in CE either (it implements no
 *   {@code IEnergyReceiverMK2}) - this is not a simplification, it is preserved.</li>
 *   <li>{@link #mode} is reset to {@link EnumWavelengths#NULL} every tick and must be set from
 *   outside by a Free-Electron Laser block before this tick's {@link #updateEntity()} runs - CE's own
 *   cross-block coupling. {@code TileEntityFEL} is not ported in this pass (soft dependency flagged
 *   in the research doc), so {@link #setLaserMode} has no caller yet; the field/hook is ready for
 *   when it lands.</li>
 * </ul>
 */
public class SilexBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardReceiverMK2, ITickableBE, IPersistentNBT, MenuProvider {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int QUEUE_START = 2;
    private static final int QUEUE_END = 7;

    public static final int MAX_FILL = 16000;
    public static final int PROCESS_TIME = 80;

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.PEROXIDE, MAX_FILL).withOwner(this);

    public EnumWavelengths mode = EnumWavelengths.NULL;
    public ComparableStack current;
    public int currentFill;
    public int progress;

    private int loadDelay;

    public SilexBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 8, true, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineSILEX");
    }

    /** Called by a Free-Electron Laser block aiming a beam at this SILEX this tick (not ported yet - see class javadoc). */
    public void setLaserMode(EnumWavelengths mode) {
        this.mode = mode;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemStack) {
        return i == INPUT_SLOT && SILEXRecipes.getOutput(itemStack) != null;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return slot >= QUEUE_START;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{INPUT_SLOT, QUEUE_START, QUEUE_START + 1, QUEUE_START + 2, QUEUE_START + 3, QUEUE_END - 1, QUEUE_END};
    }

    public int getProgressScaled(int i) {
        return (progress * i) / PROCESS_TIME;
    }

    public int getFluidScaled(int i) {
        return (tank.getFill() * i) / tank.getMaxFill();
    }

    public int getFillScaled(int i) {
        return (currentFill * i) / MAX_FILL;
    }

    private void loadFeedstock() {
        loadDelay++;
        if (loadDelay > 20) loadDelay = 0;
        if (loadDelay != 0) return;

        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || tank.getTankType() != Fluids.PEROXIDE) return;
        if (current != null && !current.equals(new ComparableStack(input).makeSingular())) return;

        SILEXRecipe recipe = SILEXRecipes.getOutput(input);
        if (recipe == null) return;

        int load = recipe.fluidProduced;
        if (load <= MAX_FILL - currentFill && load <= tank.getFill()) {
            currentFill += load;
            current = new ComparableStack(input).makeSingular();
            tank.setFill(tank.getFill() - load);
            input.shrink(1);
        }
    }

    private boolean process() {
        if (current == null || currentFill <= 0) return false;

        SILEXRecipe recipe = SILEXRecipes.getOutput(current.getStack());
        if (recipe == null) return false;
        if (recipe.laserStrength.ordinal() > mode.ordinal()) return false;
        if (currentFill < recipe.fluidConsumed) return false;
        if (!inventory.getStackInSlot(OUTPUT_SLOT).isEmpty()) return false;

        // The exact isotope-separation formula from docs/phase2/machines_chemical_isotope.md - do not
        // simplify: every wavelength tier above the recipe's minimum doubles the progress increment.
        progress += Math.pow(2, mode.ordinal() - recipe.laserStrength.ordinal() + 1) / 2;

        if (progress >= PROCESS_TIME) {
            currentFill -= recipe.fluidConsumed;

            WeightedRandom.Item picked = WeightedRandom.getRandomItem(level.random, recipe.outputs);
            if (picked instanceof com.hbm.util.WeightedRandomObject wro) {
                ItemStack out = wro.asStack();
                if (out != null) inventory.setStackInSlot(OUTPUT_SLOT, out.copy());
            }
            progress = 0;
            setChanged();
        }

        return true;
    }

    private void dequeue() {
        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) return;

        for (int i = QUEUE_START; i <= QUEUE_END; i++) {
            ItemStack queued = inventory.getStackInSlot(i);
            if (!queued.isEmpty() && queued.getCount() < queued.getMaxStackSize()
                    && ItemStack.isSameItemSameComponents(queued, output)) {
                queued.grow(1);
                output.shrink(1);
                return;
            }
        }
        for (int i = QUEUE_START; i <= QUEUE_END; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, output.copy());
                inventory.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
                return;
            }
        }
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(dir);
            trySubscribe(tank.getTankType(), level, neighbor, dir);
        }

        loadFeedstock();

        if (!process()) progress = 0;

        dequeue();

        if (currentFill <= 0) current = null;

        dataChanged();
        networkPackMK2(50);

        this.mode = EnumWavelengths.NULL;
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.writeToNBT(tag, "tank");
        tag.putInt("fill", currentFill);
        tag.putInt("progress", progress);
        if (current != null && current.item != null) {
            tag.putString("currentItem", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(current.item).toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(tag, "tank");
        currentFill = tag.getInt("fill");
        progress = tag.getInt("progress");
        if (tag.contains("currentItem")) {
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.parse(tag.getString("currentItem")));
            current = new ComparableStack(item, 1);
        } else {
            current = null;
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(currentFill);
        buf.writeInt(progress);
        buf.writeUtf(mode.toString());
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        currentFill = buf.readInt();
        progress = buf.readInt();
        mode = EnumWavelengths.valueOf(buf.readUtf());
        tank.deserialize(buf);
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        if (tank.getFill() <= 0) return;
        tank.writeToNBT(nbt, "tank");
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        tank.readFromNBT(nbt, "tank");
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != null;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SilexMenu(containerId, playerInventory, this);
    }
}
