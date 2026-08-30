package com.hbm.blockentity.machine.oil;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.oil.MachineRefineryMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.RefineryRecipes;
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
 * <h2>Scope trims vs. CE</h2>
 * <ul>
 *   <li><b>No item-container fluid loading</b> - same pre-existing gap as every other machine in this
 *   area, see {@link OilDrillBaseBlockEntity}'s javadoc. Inventory renumbered to battery (0) + sulfur
 *   output (1) only (CE: 13 slots - battery, 5 canister in/out pairs, sulfur out, fluid-ID slot).</li>
 *   <li><b>No {@code IOverpressurable}/{@code IRepairable} explosion-and-repair state machine</b>
 *   (CE's {@code hasExploded}/{@code onFire}/{@code explode}/{@code tryExtinguish}/{@code repair}) -
 *   neither interface is ported (no explosion-system package exists in this port yet, and this is a
 *   Phase 3 "weapons & destruction" concern per this project's own phase list, not Phase 2 machines).
 *   The refinery therefore never explodes and never needs repairing; it just keeps refining.</li>
 *   <li><b>No pollution bookkeeping</b> ({@code PollutionHandler.incrementPollution}) - Phase 4 scope,
 *   same precedent as {@code MachineCombustionEngineBlockEntity}.</li>
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

    private static final int BATTERY_SLOT = 0;
    private static final int SULFUR_SLOT = 1;

    public final List<FluidTankNTM> tanks = new ArrayList<>();
    public long power;
    public int sulfur;
    public boolean isOn;

    public MachineRefineryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, true, true);
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
        return i == BATTERY_SLOT && Library.isBattery(stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return slot == SULFUR_SLOT;
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

        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, MAX_POWER);

        refine();

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
                ItemStack current = inventory.getStackInSlot(SULFUR_SLOT);
                if (current.isEmpty()) {
                    inventory.setStackInSlot(SULFUR_SLOT, out.copy());
                } else if (ItemStack.isSameItemSameComponents(current, out)
                        && current.getCount() + out.getCount() <= current.getMaxStackSize()) {
                    ItemStack grown = current.copy();
                    grown.grow(out.getCount());
                    inventory.setStackInSlot(SULFUR_SLOT, grown);
                }
            }
        }

        this.power -= 5;
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
