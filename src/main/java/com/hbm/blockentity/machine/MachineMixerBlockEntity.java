package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.machine.MachineMixerMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.MixerRecipes.Match;
import com.hbm.inventory.recipes.MixerRecipes.MixerRecipe;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.tileentity.machine.TileEntityMachineMixer} (387 lines, read in
 * full) - see {@code docs/phase2/machines_shredder_assembler_crystallizer_mixer.md}'s per-machine
 * detail for the full slot/power/recipe breakdown.
 * <p>
 * <b>Slot trim vs. CE</b> (documented, matching the precedent every other machine in this pass
 * already sets for the same missing subsystem): CE's slot 2 ({@code tanks[2].setType(2, inventory)},
 * an {@code IItemFluidIdentifier} output-fluid selector) is dropped - see
 * {@link MixerRecipes#findMatch}'s own javadoc for how recipe selection is auto-detected instead.
 * This class's inventory is 4 slots: 0 battery, 1 solid-ingredient input, 2-3 upgrade slots (CE: 5
 * slots, 0 battery/1 solid/2 fluid-id/3-4 upgrades).
 * <p>
 * <b>Recipe selection</b>: CE picks one of possibly several competing recipes for a given output
 * fluid via a player-cyclable {@code recipeIndex} (advanced by {@code receiveControl}'s "toggle"
 * field), then re-derives {@code tanks[0]}/{@code tanks[1]}'s expected type from whichever recipe is
 * "loaded". This class instead scans every registered recipe each tick via {@link MixerRecipes#findMatch}
 * against whatever the two reagent tanks/solid slot already contain - see that method's own javadoc
 * for why (no output-fluid-selector item exists yet to drive CE's manual flow). The output tank's
 * type locks in on first successful process, matching {@link FluidTankNTM#fill}'s own
 * "NONE-typed tank accepts anything, typed tank only matches its own type" contract - once
 * {@code tanks[2]} has a real fluid in it, only recipes producing that exact fluid can match/output
 * further, exactly matching CE's own single-output-type-per-machine-instance behavior.
 * <p>
 * <b>Power/duration formulas</b> (ported from CE exactly, <i>not</i> cached across ticks - CE
 * recomputes both every tick, this class does too): consumption {@code = 50 + speedLevel*150},
 * <i>then</i> {@code -= consumption*powerLevel*0.25} (POWER discount applied after the SPEED
 * surcharge, not just to the base - order matters, see the research report's own flag on this),
 * <i>then</i> {@code *= (overLevel*3 + 1)} (OVERDRIVE multiplies last). {@code processTime}
 * similarly: {@code -= processTime*speedLevel/4}, then {@code /= (overLevel+1)}, floored at 1 tick.
 */
public class MachineMixerBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 10_000L;
    private static final int TANK_REAGENT_CAPACITY = 16_000;
    private static final int TANK_OUTPUT_CAPACITY = 24_000;

    public static final int BATTERY_SLOT = 0;
    public static final int SOLID_INPUT = 1;
    public static final int UPGRADE_START = 2;
    public static final int UPGRADE_END = 3;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);

    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
        VALID_UPGRADES.put(UpgradeType.OVERDRIVE, 3);
    }

    private final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(VALID_UPGRADES);
    /** Index 0/1 = reagent input tanks, index 2 = output tank - matches CE's {@code tanks[]} numbering. */
    public final List<FluidTankNTM> tanks = List.of(
            new FluidTankNTM(Fluids.NONE, TANK_REAGENT_CAPACITY).withOwner(this),
            new FluidTankNTM(Fluids.NONE, TANK_REAGENT_CAPACITY).withOwner(this),
            new FluidTankNTM(Fluids.NONE, TANK_OUTPUT_CAPACITY).withOwner(this)
    );

    private long power;
    public int progress;
    public int processTime; // CE: processTime

    public MachineMixerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 4, true, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineMixer");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) return Library.isBattery(stack);
        if (slot == SOLID_INPUT) return true;
        return slot >= UPGRADE_START && slot <= UPGRADE_END && stack.getItem() instanceof ItemMachineUpgrade;
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

    /** See class javadoc's "Power/duration formulas" - recomputed every tick, not cached, matching CE. */
    private long getConsumption() {
        double consumption = 50.0 + speedLevel() * 150.0;
        consumption -= consumption * powerLevel() * 0.25;
        consumption *= overLevel() * 3 + 1;
        return (long) consumption;
    }

    private int getEffectiveProcessTime(int base) {
        int time = base - base * speedLevel() / 4;
        time /= overLevel() + 1;
        return Math.max(1, time);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            BlockPos target = worldPosition.relative(dir);
            trySubscribe(level, target.getX(), target.getY(), target.getZ(), dir);
            trySubscribe(tanks.get(0).getTankType(), level, target.getX(), target.getY(), target.getZ(), dir);
            trySubscribe(tanks.get(1).getTankType(), level, target.getX(), target.getY(), target.getZ(), dir);
            if (tanks.get(2).getFill() > 0) tryProvide(tanks.get(2), level, target, dir);
        }

        upgradeManager.checkSlots(inventory, UPGRADE_START, UPGRADE_END);
        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, MAX_POWER);

        Match match = MixerRecipes.findMatch(tanks.get(0).getTankType(), tanks.get(0).getFill(),
                tanks.get(1).getTankType(), tanks.get(1).getFill(), inventory.getStackInSlot(SOLID_INPUT));

        long req = getConsumption();
        if (match == null || power < req || !outputAccepts(match.outputType(), match.recipe().output)) {
            progress = 0;
        } else {
            processTime = getEffectiveProcessTime(match.recipe().processTime);
            power -= req;
            progress++;

            if (progress >= processTime) {
                progress = 0;
                MixerRecipe recipe = match.recipe();
                if (recipe.input1 != null) tanks.get(0).setFill(tanks.get(0).getFill() - recipe.input1.fill);
                if (recipe.input2 != null) tanks.get(1).setFill(tanks.get(1).getFill() - recipe.input2.fill);
                if (recipe.solidInput != null) inventory.getStackInSlot(SOLID_INPUT).shrink(recipe.solidInput.count());

                // First successful process locks the output tank's type in (FluidTankNTM.setFill/
                // fill() already refuses a foreign type once typed - setTankType here is a one-time
                // NONE->real-type transition, matching CE's own IItemFluidIdentifier-driven behavior
                // without needing that item - see class javadoc's "Recipe selection".
                if (tanks.get(2).getTankType() == Fluids.NONE) tanks.get(2).setTankType(match.outputType());
                tanks.get(2).setFill(tanks.get(2).getFill() + recipe.output);
            }
        }

        dataChanged();
        networkPackMK2(50);
    }

    private boolean outputAccepts(FluidType outputType, int amount) {
        FluidTankNTM out = tanks.get(2);
        if (out.getTankType() != Fluids.NONE && out.getTankType() != outputType) return false;
        return out.getFill() + amount <= out.getMaxFill();
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
        return MAX_POWER;
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tanks.get(2));
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks.get(0), tanks.get(1));
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return tanks;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("progress", progress);
        tag.putInt("processTime", processTime);
        for (int i = 0; i < tanks.size(); i++) tanks.get(i).writeToNBT(tag, "tank" + i);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progress = tag.getInt("progress");
        processTime = tag.getInt("processTime");
        for (int i = 0; i < tanks.size(); i++) tanks.get(i).readFromNBT(tag, "tank" + i);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        buf.writeInt(processTime);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        processTime = buf.readInt();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineMixerMenu(containerId, playerInventory, this);
    }
}
