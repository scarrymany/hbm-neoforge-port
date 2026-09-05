package com.hbm.blockentity.machine.oil;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.capability.NTMFluidCapabilityHandler;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.container.machine.oil.MachineRefineryMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.util.Tuple;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code TileEntityMachineRefinery} (469 lines, read in full) - an independent
 * multiblock (unrelated to {@link OilDrillBaseBlockEntity}, grouped with the extractors only because
 * it consumes their output fluids downstream, per {@code docs/phase2/oil_production_chain.md}): a
 * 5-tank chain ({@code HOTOIL} in -&gt; {@code HEAVYOIL}/{@code NAPHTHA}/{@code LIGHTOIL}/
 * {@code PETROLEUM} out) driven by {@link RefineryRecipes} (the cracking recipe data this task calls
 * fully in-scope), plus a sulfur item byproduct every {@link #MAX_SULFUR} successful refine cycles.
 *
 * Inventory is 13 slots Exact CE {@code TileEntityMachineRefinery.java:69}. Canister trim Exact CE
 * {@code :136-145}: {@code tanks[0].setType(12)} / {@code loadTank(1, 2)} then four
 * {@code unloadTank} pairs after {@code refine()}. Sulfur byproduct is slot 11 ({@code :303-309}).
 * Hopper Exact CE {@code :323-329}: accessible {@code 0..11}, extract {@code 2,4,6,8,10,11}.
 * {@code refine()} {@code incrementPollution(SOOT, SOOT_PER_SECOND*5)} every 20t Exact CE {@code :318}.
 * {@code onFire} {@code SOOT*70} stay skipped ({@code IRepairable} not ported).
 *
 * <h2>Scope trims vs. CE</h2>
 * <ul>
 *   <li><b>No {@code IOverpressurable}/{@code IRepairable} explosion-and-repair state machine</b>
 *   (CE's {@code hasExploded}/{@code onFire}/{@code explode}/{@code tryExtinguish}/{@code repair}) -
 *   neither interface is ported (no explosion-system package exists in this port yet, and this is a
 *   Phase 3 "weapons & destruction" concern per this project's own phase list, not Phase 2 machines).
 *   The refinery therefore never explodes and never needs repairing; it just keeps refining.</li>
 *   <li><b>No looped boiler audio</b> ({@code AudioWrapper}/{@code getLoopedSound}) - the research
 *   report flagged this as an unverified dependency (whether {@code AudioWrapper}/looped-sound
 *   support exists in this port's current sound registry was never confirmed); dropped rather than
 *   guessed at.</li>
 *   <li><b>No self-correcting "still in dummy meta range on first tick" placement quirk.</b> CE's own
 *   comment marks this as a deliberate Forge-1.12 workaround, not dead code - but this port's
 *   {@link com.hbm.blocks.BlockDummyable#setPlacedBy} already writes the core's final (&gt;=12)
 *   {@code META} value in the very same {@code setBlock} call that produces the {@link BlockState}
 *   {@code newBlockEntity} is constructed against (see that method: {@code level.setBlock(corePos,
 *   defaultBlockState().setValue(META, meta), 3)} happens before the block entity exists at all) -
 *   there is no confirmed equivalent, in this port's placement flow, of the CE timing gap that quirk
 *   compensated for. Documented deliberate omission, not a silent drop of "real CE behavior".</li>
 * </ul>
 */
public class MachineRefineryBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, IPersistentNBT, MenuProvider {

    public static final int MAX_SULFUR = 100;
    public static final long MAX_POWER = 1000L;

    private static final int SLOT_BATTERY = 0;
    private static final int SLOT_LOAD = 1;
    private static final int SLOT_LOAD_OUT = 2;
    private static final int SLOT_HEAVY = 3;
    private static final int SLOT_HEAVY_OUT = 4;
    private static final int SLOT_NAPHTHA = 5;
    private static final int SLOT_NAPHTHA_OUT = 6;
    private static final int SLOT_LIGHT = 7;
    private static final int SLOT_LIGHT_OUT = 8;
    private static final int SLOT_PETROLEUM = 9;
    private static final int SLOT_PETROLEUM_OUT = 10;
    private static final int SLOT_SULFUR = 11;
    private static final int SLOT_ID = 12;

    public final List<FluidTankNTM> tanks = new ArrayList<>();
    public long power;
    public int sulfur;
    public boolean isOn;

    public MachineRefineryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 13, true, true);
        tanks.add(new FluidTankNTM(Fluids.HOTOIL, 64_000).withOwner(this));
        tanks.add(new FluidTankNTM(Fluids.HEAVYOIL, 24_000).withOwner(this));
        tanks.add(new FluidTankNTM(Fluids.NAPHTHA, 24_000).withOwner(this));
        tanks.add(new FluidTankNTM(Fluids.LIGHTOIL, 24_000).withOwner(this));
        tanks.add(new FluidTankNTM(Fluids.PETROLEUM, 24_000).withOwner(this));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineRefinery");
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        if (stack.isEmpty()) return false;
        // CE has no override (defaults true). MenuBase.tile is getCheckedInventory(),
        // so ID/canister GUI insert dies without this.
        if (i == SLOT_BATTERY) return Library.isBattery(stack);
        if (i == SLOT_LOAD) {
            if (FluidContainerRegistry.getFluidContent(stack, tanks.get(0).getTankType()) > 0) return true;
            // Port ItemCanister is IFillableItem, not in FluidContainerRegistry (CE metadata canisters).
            return stack.getItem() instanceof IFillableItem fill && fill.providesFluid(tanks.get(0).getTankType(), stack);
        }
        if (i == SLOT_HEAVY || i == SLOT_NAPHTHA || i == SLOT_LIGHT || i == SLOT_PETROLEUM) {
            return NTMFluidCapabilityHandler.isEmptyNtmFluidContainer(stack.getItem())
                    || stack.getItem() instanceof IFillableItem;
        }
        if (i == SLOT_ID) return stack.getItem() instanceof IItemFluidIdentifier;
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        // CE TileEntityMachineRefinery.java:328-329
        return slot == SLOT_LOAD_OUT || slot == SLOT_HEAVY_OUT || slot == SLOT_NAPHTHA_OUT
                || slot == SLOT_LIGHT_OUT || slot == SLOT_PETROLEUM_OUT || slot == SLOT_SULFUR;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        // CE :323-324 — ID 12 is GUI-only
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z + 1, Direction.EAST),
                new DirPos(x + 2, y, z - 1, Direction.EAST),
                new DirPos(x - 2, y, z + 1, Direction.WEST),
                new DirPos(x - 2, y, z - 1, Direction.WEST),
                new DirPos(x + 1, y, z + 2, Direction.SOUTH),
                new DirPos(x - 1, y, z + 2, Direction.SOUTH),
                new DirPos(x + 1, y, z - 2, Direction.NORTH),
                new DirPos(x - 1, y, z - 2, Direction.NORTH)
        };
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        isOn = false;

        for (DirPos dp : getConPos()) {
            trySubscribe(level, dp);
            trySubscribe(tanks.get(0).getTankType(), level, dp);
        }

        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, MAX_POWER);
        // CE TileEntityMachineRefinery.java:137-145
        tanks.get(0).setType(SLOT_ID, inventory);
        tanks.get(0).loadTank(SLOT_LOAD, SLOT_LOAD_OUT, inventory);

        refine();

        tanks.get(1).unloadTank(SLOT_HEAVY, SLOT_HEAVY_OUT, inventory);
        tanks.get(2).unloadTank(SLOT_NAPHTHA, SLOT_NAPHTHA_OUT, inventory);
        tanks.get(3).unloadTank(SLOT_LIGHT, SLOT_LIGHT_OUT, inventory);
        tanks.get(4).unloadTank(SLOT_PETROLEUM, SLOT_PETROLEUM_OUT, inventory);

        for (DirPos dp : getConPos()) {
            for (int i = 1; i < 5; i++) {
                if (tanks.get(i).getFill() > 0) tryProvide(tanks.get(i), level, dp);
            }
        }

        dataChanged();
        networkPackMK2(150);
    }

    private void refine() {
        Tuple.Quintet<FluidStack, FluidStack, FluidStack, FluidStack, ItemStack> recipe =
                RefineryRecipes.getRefinery(tanks.get(0).getTankType());

        if (recipe == null) {
            for (int i = 1; i < 5; i++) tanks.get(i).setTankType(Fluids.NONE);
            return;
        }

        FluidStack[] stacks = new FluidStack[]{recipe.getV(), recipe.getW(), recipe.getX(), recipe.getY()};
        for (int i = 0; i < stacks.length; i++) tanks.get(i + 1).setTankType(stacks[i].type);

        if (power < 5 || tanks.get(0).getFill() < 100) return;

        for (int i = 0; i < stacks.length; i++) {
            if (tanks.get(i + 1).getFill() + stacks[i].fill > tanks.get(i + 1).getMaxFill()) return;
        }

        this.isOn = true;
        tanks.get(0).setFill(tanks.get(0).getFill() - 100);
        for (int i = 0; i < stacks.length; i++) tanks.get(i + 1).setFill(tanks.get(i + 1).getFill() + stacks[i].fill);

        this.sulfur++;

        if (this.sulfur >= MAX_SULFUR) {
            this.sulfur -= MAX_SULFUR;
            ItemStack out = recipe.getZ();

            if (out != null && !out.isEmpty()) {
                ItemStack current = inventory.getStackInSlot(SLOT_SULFUR);
                if (current.isEmpty()) {
                    inventory.setStackInSlot(SLOT_SULFUR, out.copy());
                } else if (ItemStack.isSameItemSameComponents(current, out)
                        && current.getCount() + out.getCount() <= current.getMaxStackSize()) {
                    ItemStack grown = current.copy();
                    grown.grow(out.getCount());
                    inventory.setStackInSlot(SLOT_SULFUR, grown);
                }
            }
        }

        // CE TileEntityMachineRefinery.java:318
        if (level != null && level.getGameTime() % 20 == 0) {
            PollutionHandler.incrementPollution(level, worldPosition, PollutionHandler.PollutionType.SOOT,
                    PollutionHandler.SOOT_PER_SECOND * 5);
        }
        this.power -= 5;
    }

    /** Exact CE {@code TileEntityMachineRefinery.getPowerScaled} :332-334. */
    public long getPowerScaled(long i) {
        return (power * i) / MAX_POWER;
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
        return List.of(tanks.get(1), tanks.get(2), tanks.get(3), tanks.get(4));
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks.get(0));
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return tanks;
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != null && dir != Direction.DOWN;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("sulfur", sulfur);
        for (int i = 0; i < tanks.size(); i++) tanks.get(i).writeToNBT(tag, "tank" + i);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        sulfur = tag.getInt("sulfur");
        for (int i = 0; i < tanks.size(); i++) tanks.get(i).readFromNBT(tag, "tank" + i);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
        buf.writeBoolean(isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
        isOn = buf.readBoolean();
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        boolean empty = true;
        for (FluidTankNTM tank : tanks) if (tank.getFill() > 0) empty = false;
        if (empty) return;

        for (int i = 0; i < tanks.size(); i++) tanks.get(i).writeToNBT(nbt, "" + i);
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        for (int i = 0; i < tanks.size(); i++) tanks.get(i).readFromNBT(nbt, "" + i);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineRefineryMenu(containerId, playerInventory, this);
    }
}
