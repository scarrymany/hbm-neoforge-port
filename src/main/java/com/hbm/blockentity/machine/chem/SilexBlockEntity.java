package com.hbm.blockentity.machine.chem;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.container.machine.chem.SilexMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.chem.SILEXRecipes;
import com.hbm.inventory.recipes.chem.SILEXRecipes.SILEXRecipe;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.main.MainRegistry;
import com.hbm.util.WeightedRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
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
 * {@code tank.setType(1, 1)} / {@code tank.loadTank(2, 3)} Exact CE {@code TileEntitySILEX.java:73-74}.
 * Inventory is 11 slots Exact CE {@code :58} (0 input, 1 ID, 2-3 canister, 4 output, 5-10 queue).
 * <p>
 * <b>Scope trims from CE</b> (documented):
 * <ul>
 *   <li>{@link #loadFluid()} is CE {@code TileEntitySILEX.java:169-222}: UF6/PUF6/DEATH
 *   {@code fluidConversion} plus any tank type that has a {@code fluid_icon} SILEX row
 *   (VITRIOL/REDMUD/FULLERENE) convert 50 mB/tick with no peroxide consume. Peroxide + item-slot
 *   path is unchanged.</li>
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

    public static final int INPUT_SLOT = 0;
    public static final int SLOT_ID = 1;
    public static final int SLOT_CANISTER = 2;
    public static final int SLOT_EMPTY = 3;
    public static final int OUTPUT_SLOT = 4;
    public static final int QUEUE_START = 5;
    public static final int QUEUE_END = 10;

    public static final int MAX_FILL = 16000;
    public static final int PROCESS_TIME = 80;

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.PEROXIDE, MAX_FILL).withOwner(this);

    public EnumWavelengths mode = EnumWavelengths.NULL;
    public ComparableStack current;
    public int currentFill;
    public int progress;

    private int loadDelay;

    public SilexBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 11, true, false);
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
        if (itemStack.isEmpty()) return false;
        if (i == INPUT_SLOT) return SILEXRecipes.getOutput(itemStack) != null;
        // CE :289-293 is input-only. MenuBase.tile is getCheckedInventory(),
        // so ID/canister GUI insert dies without this.
        if (i == SLOT_ID) return itemStack.getItem() instanceof IItemFluidIdentifier;
        if (i == SLOT_CANISTER) {
            if (FluidContainerRegistry.getFluidContent(itemStack, tank.getTankType()) > 0) return true;
            return itemStack.getItem() instanceof IFillableItem fill && fill.providesFluid(tank.getTankType(), itemStack);
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        // CE TileEntitySILEX.java:297-298
        return slot >= QUEUE_START;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        // CE :284-285
        return new int[]{INPUT_SLOT, 5, 6, 7, 8, 9, 10};
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

    /** Exact CE {@code TileEntitySILEX.handleButtonPacket} :137-141. */
    public void voidContents() {
        this.currentFill = 0;
        this.current = null;
    }

    /** CE {@code TileEntitySILEX.java:169-222}. */
    private void loadFluid() {
        FluidType type = tank.getTankType();
        ComparableStack conv = conversionFor(type);
        if (conv != null) {
            if (currentFill == 0) current = (ComparableStack) conv.copy();
            if (current != null && current.equals(conv)) {
                int toFill = Math.min(50, Math.min(MAX_FILL - currentFill, tank.getFill()));
                currentFill += toFill;
                tank.setFill(tank.getFill() - toFill);
            }
        } else {
            ComparableStack direct = new ComparableStack(fluidIcon(), 1, type.getID());
            if (SILEXRecipes.getOutput(direct.toStack()) != null) {
                if (currentFill == 0) current = (ComparableStack) direct.copy();
                if (current != null && current.equals(direct)) {
                    int toFill = Math.min(50, Math.min(MAX_FILL - currentFill, tank.getFill()));
                    currentFill += toFill;
                    tank.setFill(tank.getFill() - toFill);
                }
            }
        }

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

    private static Item fluidIcon() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "fluid_icon"));
    }

    private static ComparableStack conversionFor(FluidType type) {
        if (type == Fluids.UF6 || type == Fluids.PUF6 || type == Fluids.DEATH) {
            return new ComparableStack(fluidIcon(), 1, type.getID());
        }
        return null;
    }

    private boolean process() {
        if (current == null || currentFill <= 0) return false;

        SILEXRecipe recipe = SILEXRecipes.getOutput(current.toStack());
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

        // CE TileEntitySILEX.java:73-74
        tank.setType(SLOT_ID, SLOT_ID, inventory);
        tank.loadTank(SLOT_CANISTER, SLOT_EMPTY, inventory);

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(dir);
            trySubscribe(tank.getTankType(), level, neighbor, dir);
        }

        EnumWavelengths fromFel = com.hbm.blockentity.machine.accel.FelBlockEntity.laserHitting(level, worldPosition);
        if (fromFel != EnumWavelengths.NULL) mode = fromFel;

        loadFluid();

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
            tag.putString("currentItem", BuiltInRegistries.ITEM.getKey(current.item).toString());
            tag.putInt("currentMeta", current.meta);
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
            current = new ComparableStack(item, 1, tag.getInt("currentMeta"));
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
        // CE TileEntitySILEX.java:116-119
        if (this.current != null && this.current.item != null) {
            buf.writeInt(BuiltInRegistries.ITEM.getId(this.current.item));
            buf.writeInt(this.current.meta);
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        currentFill = buf.readInt();
        progress = buf.readInt();
        mode = EnumWavelengths.valueOf(buf.readUtf());
        tank.deserialize(buf);
        // CE TileEntitySILEX.java:131-134
        if (currentFill > 0) {
            current = new ComparableStack(BuiltInRegistries.ITEM.byId(buf.readInt()), 1, buf.readInt());
        } else {
            current = null;
        }
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
