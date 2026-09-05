package com.hbm.blockentity.machine.chem;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.machine.chem.ChemPlantMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.chem.ChemPlantRecipes;
import com.hbm.inventory.recipes.chem.ChemPlantRecipes.ChemPlantRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code TileEntityMachineChemicalPlant} (single-module, distinct from the
 * 4-lane factory). Delegates recipe data to {@link ChemPlantRecipes}.
 * {@code recipe=="null"} keeps first-match; GUI/ROR lock a name.
 * <p>
 * Exact CE {@code TileEntityMachineChemicalPlant} 22-slot layout: battery 0 / blueprint 1 /
 * upgrades 2-3 / item in 4-6 / item out 7-9 / canisters 10-21
 * ({@code ContainerMachineChemicalPlant.java:36-52}). Slots 2-3 SPEED/POWER/OVERDRIVE cap 3
 * Exact CE {@code :121}/{@code :142-147}/{@code :349-354}. {@code upgradePlug} on insert
 * {@code :83-84}. {@code ModuleMachineBase.process} speed/pow + {@code restrictedMode*0.25}
 * {@code :135-148}. IUpgradeInfoProvider / looped audio stay skipped.
 * Factory leftover {@code loadTank(10,13)} is not copied.
 */
public class ChemPlantBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, IPersistentNBT,
        MenuProvider, IControlReceiver, IRORValueProvider, IRORInteractive {

    public static final int BATTERY_SLOT = 0;
    public static final int BLUEPRINT_SLOT = 1;
    public static final int SLOT_UPGRADE_A = 2;
    public static final int SLOT_UPGRADE_B = 3;
    public static final int ITEM_IN_START = 4;
    public static final int ITEM_OUT_START = 7;
    /** Exact CE fluid-in load / empty-out. */
    private static final int SLOT_FLUID_IN = 10;
    private static final int SLOT_FLUID_IN_EMPTY = 13;
    /** Exact CE fluid-out empty / filled. */
    private static final int SLOT_FLUID_OUT = 16;
    private static final int SLOT_FLUID_OUT_FULL = 19;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);

    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
        VALID_UPGRADES.put(UpgradeType.OVERDRIVE, 3);
    }

    public static final long MIN_MAX_POWER = 100_000L;
    public static final int TANK_CAPACITY = 24_000;

    public final FluidTankNTM[] inputTanks = new FluidTankNTM[3];
    public final FluidTankNTM[] outputTanks = new FluidTankNTM[3];

    public long power;
    public long maxPower = MIN_MAX_POWER;
    public double progress;
    public boolean isProcessing;
    public boolean didProcess;
    public boolean restrictedMode;
    public String recipe = "null";
    private String activeRecipeName;
    private final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(VALID_UPGRADES);

    public ChemPlantBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 22, true, true);
        for (int i = 0; i < 3; i++) {
            inputTanks[i] = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);
            outputTanks[i] = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);
        }
    }

    @Override
    protected ItemStackHandler getNewInventory(int scount, int slotlimit) {
        return new ItemStackHandler(scount) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                setChanged();
            }

            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                super.setStackInSlot(slot, stack);
                // CE TileEntityMachineChemicalPlant.java:83-84
                if (!stack.isEmpty() && slot >= SLOT_UPGRADE_A && slot <= SLOT_UPGRADE_B
                        && stack.getItem() instanceof ItemMachineUpgrade && level != null && !level.isClientSide) {
                    level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                            HBMSoundHandler.upgradePlug.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }

            @Override
            public int getSlotLimit(int slot) {
                return slotlimit;
            }
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineChemicalPlant");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        // CE TileEntityMachineChemicalPlant.java:270-276
        if (slot == BATTERY_SLOT) return true;
        if (slot == BLUEPRINT_SLOT) return stack.getItem() instanceof ItemBlueprints;
        if (slot >= SLOT_UPGRADE_A && slot <= SLOT_UPGRADE_B) return stack.getItem() instanceof ItemMachineUpgrade;
        if (slot >= SLOT_FLUID_IN && slot <= SLOT_FLUID_IN + 2) return true;
        if (slot >= SLOT_FLUID_OUT && slot <= SLOT_FLUID_OUT + 2) return true;
        return slot >= ITEM_IN_START && slot < ITEM_IN_START + 3;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        // CE :280-281 is solid-out only. Menu takeOnly still lets the player pull empties/filled.
        return slot >= ITEM_OUT_START && slot < ITEM_OUT_START + 3;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{4, 5, 6, 7, 8, 9};
    }

    public int getProgressScaled(int i) {
        return (int) Math.round(progress * i);
    }

    private ChemPlantRecipe findRecipe() {
        ChemPlantRecipe named = ChemicalPlantRecipes.byName(recipe);
        if (named != null) {
            return matchesItems(named) && matchesFluids(named) ? named : null;
        }
        if (recipe != null && !recipe.isEmpty() && !"null".equals(recipe)) return null;
        for (ChemPlantRecipe found : ChemPlantRecipes.RECIPES) {
            if (matchesItems(found) && matchesFluids(found)) return found;
        }
        return null;
    }

    @Nullable
    private ChemPlantRecipe findItemOnlyRecipe() {
        for (ChemPlantRecipe recipe : ChemPlantRecipes.RECIPES) {
            if (matchesItems(recipe)) return recipe;
        }
        return null;
    }

    private boolean matchesItems(ChemPlantRecipe recipe) {
        boolean[] used = new boolean[3];
        for (AStack key : recipe.inputItems) {
            boolean found = false;
            for (int i = 0; i < 3; i++) {
                if (used[i]) continue;
                if (key.matchesRecipe(inventory.getStackInSlot(ITEM_IN_START + i), false)) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
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
        for (FluidStack out : recipe.outputFluids) {
            boolean placed = false;
            for (FluidTankNTM tank : outputTanks) {
                if ((tank.getTankType() == out.type || tank.getTankType() == Fluids.NONE)
                        && tank.getFill() + out.fill <= tank.getMaxFill()) {
                    placed = true;
                    break;
                }
            }
            if (!placed) return false;
        }
        return true;
    }

    private void process(ChemPlantRecipe recipe) {
        boolean[] used = new boolean[3];
        for (AStack key : recipe.inputItems) {
            for (int i = 0; i < 3; i++) {
                if (used[i]) continue;
                if (key.matchesRecipe(inventory.getStackInSlot(ITEM_IN_START + i), false)) {
                    inventory.extractItem(ITEM_IN_START + i, key.count(), false);
                    used[i] = true;
                    break;
                }
            }
        }
        for (FluidStack need : recipe.inputFluids) {
            FluidTankNTM tank = findInputTank(need.type);
            if (tank != null) tank.setFill(tank.getFill() - need.fill);
        }
        for (int i = 0; i < recipe.outputItems.length && i < 3; i++) {
            inventory.insertItem(ITEM_OUT_START + i, recipe.outputItems[i].copy(), false);
        }
        for (FluidStack out : recipe.outputFluids) {
            for (FluidTankNTM tank : outputTanks) {
                if (tank.getTankType() == out.type || tank.getTankType() == Fluids.NONE) {
                    tank.setTankType(out.type);
                    tank.setFill(tank.getFill() + out.fill);
                    break;
                }
            }
        }

    }

    private void retargetEmptyTanks(ChemPlantRecipe recipe) {
        for (FluidStack need : recipe.inputFluids) {
            if (findInputTank(need.type) != null) continue;
            for (FluidTankNTM tank : inputTanks) {
                if (tank.getFill() == 0) {
                    tank.setTankType(need.type);
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
        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_A, SLOT_UPGRADE_B);

        ChemPlantRecipe itemOnly = findItemOnlyRecipe();
        if (itemOnly != null) retargetEmptyTanks(itemOnly);

        // CE TileEntityMachineChemicalPlant.java:114-131
        ChemPlantRecipe named = ChemicalPlantRecipes.byName(recipe);
        if (named != null) {
            maxPower = named.power * 100L;
        }
        maxPower = Math.max(Math.max(power, maxPower), MIN_MAX_POWER);

        ChemPlantRecipe selected = named;
        if (selected == null && (recipe == null || recipe.isEmpty() || "null".equals(recipe))) {
            selected = itemOnly;
        }
        if (selected != null && selected.inputFluids.length > 0) {
            for (int i = 0; i < Math.min(3, selected.inputFluids.length); i++) {
                inputTanks[i].loadTank(SLOT_FLUID_IN + i, SLOT_FLUID_IN_EMPTY + i, inventory);
            }
        }
        outputTanks[0].unloadTank(SLOT_FLUID_OUT, SLOT_FLUID_OUT_FULL, inventory);
        outputTanks[1].unloadTank(SLOT_FLUID_OUT + 1, SLOT_FLUID_OUT_FULL + 1, inventory);
        outputTanks[2].unloadTank(SLOT_FLUID_OUT + 2, SLOT_FLUID_OUT_FULL + 2, inventory);

        for (DirPos dp : getConPos()) {
            trySubscribe(level, dp);
            for (FluidTankNTM tank : inputTanks) if (tank.getTankType() != Fluids.NONE) trySubscribe(tank.getTankType(), level, dp);
            for (FluidTankNTM tank : outputTanks) if (tank.getFill() > 0) tryProvide(tank, level, dp);
        }

        // CE TileEntityMachineChemicalPlant.java:139-147 + ModuleMachineBase.java:135-148
        double speed = 1D;
        double pow = 1D;
        speed += Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3) / 3D;
        speed += Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);
        pow -= Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3) * 0.25D;
        pow += Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
        pow += Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3) * 10D / 3D;

        didProcess = false;
        ChemPlantRecipe matched = findRecipe();
        if (matched == null) {
            progress = 0;
            isProcessing = false;
            activeRecipeName = null;
        } else {
            activeRecipeName = matched.name;
            long cost = pow == 1 ? matched.power : (long) (matched.power * pow);
            if (power >= cost && hasOutputSpace(matched)) {
                double stepSpeed = restrictedMode ? speed * 0.25D : speed;
                isProcessing = true;
                didProcess = true;
                power -= cost;
                progress += Math.min(stepSpeed / Math.max(1, matched.duration), 1D);
                if (progress >= 1D) {
                    process(matched);
                    if (findRecipe() != null && power >= cost && hasOutputSpace(matched)) {
                        progress -= 1D;
                    } else {
                        progress = 0;
                    }
                }
            } else {
                progress = 0;
                isProcessing = false;
            }
        }

        // CE :153-155 — every processing tick, not only recipe complete
        if (didProcess) {
            ItemStack battery = inventory.getStackInSlot(BATTERY_SLOT);
            if (!battery.isEmpty() && battery.getItem() == com.hbm.items.weapon.WeaponMeleeItems.METEORITE_SWORD_MACHINED.get()) {
                inventory.setStackInSlot(BATTERY_SLOT, new ItemStack(com.hbm.items.weapon.WeaponMeleeItems.METEORITE_SWORD_TREATED.get()));
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
    public long getDemand(FluidType type, int pressure) {
        long amount = 0;
        for (FluidTankNTM tank : getReceivingTanks()) {
            if (tank.getPressure() != pressure) continue;
            if (tank.getTankType() == type || (tank.getTankType() == Fluids.NONE && tank.getFill() == 0)) {
                amount += tank.getMaxFill() - tank.getFill();
            }
        }
        return amount;
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        for (FluidTankNTM tank : getReceivingTanks()) {
            if (tank.getPressure() != pressure) continue;
            if (tank.getTankType() == Fluids.NONE && tank.getFill() == 0) tank.setTankType(type);
            if (tank.getTankType() != type) continue;
            int toAdd = (int) Math.min(amount, tank.getMaxFill() - tank.getFill());
            tank.setFill(tank.getFill() + toAdd);
            amount -= toAdd;
            if (amount <= 0) break;
        }
        return amount;
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
        tag.putDouble("chemProg", progress);
        tag.putString("recipe0", recipe);
        tag.putBoolean("restrictedMode0", restrictedMode);
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
        progress = tag.contains("chemProg") ? tag.getDouble("chemProg") : 0D;
        recipe = tag.contains("recipe0") ? tag.getString("recipe0") : "null";
        restrictedMode = tag.getBoolean("restrictedMode0");
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
        buf.writeBoolean(didProcess);
        buf.writeBoolean(restrictedMode);
        buf.writeDouble(progress);
        buf.writeUtf(recipe);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.deserialize(buf);
        for (FluidTankNTM tank : outputTanks) tank.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        isProcessing = buf.readBoolean();
        didProcess = buf.readBoolean();
        restrictedMode = buf.readBoolean();
        progress = buf.readDouble();
        recipe = buf.readUtf();
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

    @Override
    public boolean hasPermission(Player player) {
        return isUseableByPlayer(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        // CE :303-311
        if (data.contains("index") && data.contains("selection")) {
            int index = data.getInt("index");
            String selection = data.getString("selection");
            if (index == 0) {
                this.recipe = selection;
                this.restrictedMode = false;
                setChanged();
            }
        }
    }

    @Override
    public String[] getFunctionInfo() {
        // CE :358-364
        return new String[]{
                PREFIX_VALUE + "progress",
                PREFIX_VALUE + "recipe",
                PREFIX_VALUE + "active",
                PREFIX_FUNCTION + "setrecipe" + NAME_SEPARATOR + "name"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :368-372
        if ((PREFIX_VALUE + "progress").equals(name)) return "" + getProgressScaled(100);
        if ((PREFIX_VALUE + "recipe").equals(name)) return this.recipe;
        if ((PREFIX_VALUE + "active").equals(name)) return "" + (this.didProcess ? 1 : 0);
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // CE :376-382
        if ((PREFIX_FUNCTION + "setrecipe").equals(name) && params.length == 1) {
            this.recipe = params[0];
            this.restrictedMode = true;
            setChanged();
            return null;
        }
        return null;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ChemPlantMenu(containerId, playerInventory, this);
    }
}
