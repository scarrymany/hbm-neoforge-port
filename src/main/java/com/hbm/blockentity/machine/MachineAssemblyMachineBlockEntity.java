package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.machine.MachineAssemblyMachineMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.AssemblerRecipe;
import com.hbm.inventory.recipes.ProcessingRecipes;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.items.machine.MachineItems;
import com.hbm.lib.Library;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine} (544 lines,
 * read in full) - see {@code docs/phase2/machines_shredder_assembler_crystallizer_mixer.md}'s
 * per-machine detail for the full slot/power/recipe breakdown.
 * <p>
 * <b>Slots</b> (17 total, unchanged from CE): 0 battery; 1 blueprint item slot (accepted but not
 * functionally wired yet - see {@link AssemblerRecipe}'s own "Recipe selection" javadoc for why); 2-3
 * upgrade slots; 4-15 twelve recipe-input slots; 16 recipe-output slot.
 * <p>
 * <b>Recipe shape</b>: {@link ProcessingRecipes#ASSEMBLER_TYPE} ({@link AssemblerRecipe}, JSON-backed),
 * replacing CE's {@code GenericRecipe}/{@code AssemblyMachineRecipes.INSTANCE} pool system (that whole
 * hardcoded-Java-loader + blueprint-pool + dropdown-selection stack is out of this task's scope - see
 * {@link AssemblerRecipe}'s own javadoc). {@link #findMatchingRecipe()} auto-detects whichever
 * registered recipe the current 12 input slots satisfy, first match wins.
 * <p>
 * <b>Power</b>: recipe-driven, matching CE exactly - {@link #getMaxPower()} returns
 * {@code recipe.power * 100} (floored at the CE-documented {@code 100_000} baseline) once a recipe is
 * selected, {@code 100_000} with none selected. Upgrade scaling ({@code SPEED}/{@code POWER}/
 * {@code OVERDRIVE}, capped at level 3 each) ported from CE's {@code ModuleMachineBase} formula: speed
 * +33%/level (up to +100%), power -25%/level (POWER upgrade discount), overdrive stacks +1x
 * speed/+10/3x power per level.
 * <p>
 * <b>Not ported</b> (documented, matching the research report's own "safe to defer" framing): the two
 * cosmetic {@code AssemblerArm} animation objects and their striker-sound timing - zero gameplay
 * effect, tied to a custom OBJ-model rig this port has no renderer for yet. The blueprint-pool
 * recipe-selection UI (CE's {@code receiveControl}/GUI dropdown) - see {@link AssemblerRecipe}'s
 * "Recipe selection" javadoc. Two {@link FluidTankNTM} input/output buffers are kept for structural
 * parity (CE's assembler is {@code IFluidStandardTransceiverMK2}) even though no recipe in this pass's
 * ported data set consumes/produces fluid yet - a future fluid-bearing {@code AssemblerRecipe} variant
 * can wire them in without touching this class's slot layout.
 */
public class MachineAssemblyMachineBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final long BASE_MAX_POWER = 100_000L;
    public static final int TANK_CAPACITY = 4_000;

    public static final int BATTERY_SLOT = 0;
    public static final int BLUEPRINT_SLOT = 1;
    public static final int UPGRADE_START = 2;
    public static final int UPGRADE_END = 3;
    public static final int INPUT_START = 4;
    public static final int INPUT_END = 15;
    public static final int OUTPUT_SLOT = 16;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);

    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
        VALID_UPGRADES.put(UpgradeType.OVERDRIVE, 3);
    }

    private final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(VALID_UPGRADES);
    public final FluidTankNTM inputTank = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);
    public final FluidTankNTM outputTank = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);

    private long power;
    private long maxPower = BASE_MAX_POWER;
    private int progress;
    private int maxProgress;

    public MachineAssemblyMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 17, true, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.assemblyMachine");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) return Library.isBattery(stack);
        if (slot == BLUEPRINT_SLOT) return stack.getItem() == MachineItems.BLUEPRINTS.get();
        if (slot >= UPGRADE_START && slot <= UPGRADE_END) return stack.getItem() instanceof ItemMachineUpgrade;
        return slot >= INPUT_START && slot <= INPUT_END;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return slot == OUTPUT_SLOT;
    }

    private List<ItemStack> inputSnapshot() {
        ItemStack[] items = new ItemStack[INPUT_END - INPUT_START + 1];
        for (int i = 0; i < items.length; i++) items[i] = inventory.getStackInSlot(INPUT_START + i);
        return List.of(items);
    }

    /** First-match auto-detection over every registered {@link AssemblerRecipe} - see class javadoc. */
    @Nullable
    private AssemblerRecipe findMatchingRecipe() {
        if (level == null) return null;
        AssemblerRecipe.Input input = AssemblerRecipe.Input.of(inputSnapshot());
        for (RecipeHolder<AssemblerRecipe> holder : level.getRecipeManager().getAllRecipesFor(ProcessingRecipes.ASSEMBLER_TYPE.get())) {
            AssemblerRecipe recipe = holder.value();
            if (!recipe.matches(input, level)) continue;
            if (!matchesFluids(recipe)) continue;
            return recipe;
        }
        return null;
    }

    private boolean matchesFluids(AssemblerRecipe recipe) {
        for (FluidStack need : recipe.getInputFluids()) {
            if (need == null || need.type == null || need.type == Fluids.NONE) continue;
            if (inputTank.getTankType() != need.type || inputTank.getFill() < need.fill) return false;
        }
        for (FluidStack out : recipe.getOutputFluids()) {
            if (out == null || out.type == null || out.type == Fluids.NONE) continue;
            if (outputTank.getTankType() != Fluids.NONE && outputTank.getTankType() != out.type) return false;
            if (outputTank.getFill() + out.fill > outputTank.getMaxFill()) return false;
        }
        return true;
    }

    private void retargetInputTank(AssemblerRecipe recipe) {
        if (inputTank.getFill() > 0) return;
        for (FluidStack need : recipe.getInputFluids()) {
            if (need != null && need.type != null && need.type != Fluids.NONE) {
                inputTank.setTankType(need.type);
                return;
            }
        }
    }

    private int speedLevel() {
        return Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
    }

    private int powerLevel() {
        return Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3);
    }

    private int overLevel() {
        return Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);
    }

    /** CE's {@code ModuleMachineBase} duration formula: speed upgrade shrinks duration up to 66% at level 3, overdrive divides further. */
    private int effectiveDuration(int base) {
        double speedMult = 1.0 - 0.33 * speedLevel();
        int duration = (int) Math.max(1, base * Math.max(speedMult, 0.25));
        return Math.max(1, duration / (overLevel() + 1));
    }

    /** CE's {@code ModuleMachineBase} power formula: -25%/level POWER discount, then OVERDRIVE multiplies (+10/3x per level). */
    private long effectivePower(long base) {
        double powerMult = 1.0 - 0.25 * powerLevel();
        double result = base * Math.max(powerMult, 0.25);
        result *= 1.0 + (10.0 / 3.0) * overLevel();
        return (long) result;
    }

    private void consumeInputs(AssemblerRecipe recipe) {
        for (AssemblerRecipe.Entry entry : recipe.getInputEntries()) {
            int needed = entry.count();
            for (int slot = INPUT_START; slot <= INPUT_END && needed > 0; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty() || !entry.ingredient().test(stack)) continue;
                int take = Math.min(needed, stack.getCount());
                stack.shrink(take);
                needed -= take;
            }
        }
        for (FluidStack need : recipe.getInputFluids()) {
            if (need == null || need.type == Fluids.NONE) continue;
            if (inputTank.getTankType() == need.type) {
                inputTank.setFill(Math.max(0, inputTank.getFill() - need.fill));
            }
        }
        for (FluidStack out : recipe.getOutputFluids()) {
            if (out == null || out.type == Fluids.NONE) continue;
            if (outputTank.getTankType() == Fluids.NONE || outputTank.getTankType() == out.type) {
                outputTank.setTankType(out.type);
                outputTank.setFill(outputTank.getFill() + out.fill);
            }
        }
    }

    private boolean canFitOutput(ItemStack output) {
        ItemStack current = inventory.getStackInSlot(OUTPUT_SLOT);
        if (current.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(current, output) && current.getCount() + output.getCount() <= current.getMaxStackSize();
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            BlockPos target = worldPosition.relative(dir);
            trySubscribe(level, target.getX(), target.getY(), target.getZ(), dir);
            trySubscribe(inputTank.getTankType(), level, target.getX(), target.getY(), target.getZ(), dir);
            if (outputTank.getFill() > 0) tryProvide(outputTank, level, target, dir);
        }

        upgradeManager.checkSlots(inventory, UPGRADE_START, UPGRADE_END);
        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, maxPower);

        AssemblerRecipe itemMatch = findItemOnlyRecipe();
        if (itemMatch != null) retargetInputTank(itemMatch);

        AssemblerRecipe recipe = findMatchingRecipe();
        if (recipe == null) {
            progress = 0;
            maxPower = BASE_MAX_POWER;
        } else {
            maxPower = Math.max(BASE_MAX_POWER, recipe.getPower() * 100);
            maxProgress = effectiveDuration(recipe.getDuration());
            long req = effectivePower(recipe.getPower());

            if (power >= req && canFitOutput(recipe.getResultItem(level.registryAccess()))) {
                power -= req;
                progress++;
                if (progress >= maxProgress) {
                    progress = 0;
                    ItemStack output = recipe.getResultItem(level.registryAccess()).copy();
                    consumeInputs(recipe);
                    ItemStack current = inventory.getStackInSlot(OUTPUT_SLOT);
                    if (current.isEmpty()) {
                        inventory.setStackInSlot(OUTPUT_SLOT, output);
                    } else {
                        current.grow(output.getCount());
                    }

                    // CE TileEntityMachineAssemblyMachine.java:273-275: battery-slot sword upgrade
                    // (alloyed → machined) when any recipe completes successfully.
                    ItemStack battery = inventory.getStackInSlot(BATTERY_SLOT);
                    if (!battery.isEmpty() && battery.getItem() == com.hbm.items.weapon.WeaponMeleeItems.METEORITE_SWORD_ALLOYED.get()) {
                        inventory.setStackInSlot(BATTERY_SLOT, new ItemStack(com.hbm.items.weapon.WeaponMeleeItems.METEORITE_SWORD_MACHINED.get()));
                    }
                }
            }
        }

        dataChanged();
        networkPackMK2(50);
    }

    public int getProgressScaled(int scale) {
        if (maxProgress <= 0) return 0;
        return (progress * scale) / maxProgress;
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
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(outputTank);
    }

    @Nullable
    private AssemblerRecipe findItemOnlyRecipe() {
        if (level == null) return null;
        AssemblerRecipe.Input input = AssemblerRecipe.Input.of(inputSnapshot());
        for (RecipeHolder<AssemblerRecipe> holder : level.getRecipeManager().getAllRecipesFor(ProcessingRecipes.ASSEMBLER_TYPE.get())) {
            if (holder.value().matches(input, level)) return holder.value();
        }
        return null;
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
        return List.of(inputTank);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(inputTank, outputTank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putLong("maxPower", maxPower);
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        inputTank.writeToNBT(tag, "tankIn");
        outputTank.writeToNBT(tag, "tankOut");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        maxPower = tag.contains("maxPower") ? tag.getLong("maxPower") : BASE_MAX_POWER;
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
        inputTank.readFromNBT(tag, "tankIn");
        outputTank.readFromNBT(tag, "tankOut");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeInt(progress);
        buf.writeInt(maxProgress);
        inputTank.serialize(buf);
        outputTank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        progress = buf.readInt();
        maxProgress = buf.readInt();
        inputTank.deserialize(buf);
        outputTank.deserialize(buf);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineAssemblyMachineMenu(containerId, playerInventory, this);
    }
}
