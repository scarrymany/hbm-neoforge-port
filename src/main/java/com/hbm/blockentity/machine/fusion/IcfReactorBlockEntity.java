package com.hbm.blockentity.machine.fusion;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.fusion.IcfReactorMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemICFPellet;
import com.hbm.lib.DirPos;
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
 * Ported from CE's {@code TileEntityICF} (the reactor core of the ICF/inertial-confinement-fusion
 * multiblock - {@code docs/phase2/machine_fusion_watz.md}). Reacts {@link ItemICFPellet} fuel under
 * externally-supplied laser power ({@link #receiveLaser}, called each tick by
 * {@link IcfControllerBlockEntity} once it resolves a line of sight to this reactor) to produce
 * heat, exchanged into a {@code SODIUM} -&gt; {@code SODIUM_HOT} {@link FluidTankNTM} pair via the
 * {@link FT_Heatable} trait exactly like CE, and slowly filling a {@code STELLAR_FLUX} byproduct
 * tank.
 *
 * <h2>Simplifications versus CE (documented, not accidental)</h2>
 * <ul>
 *   <li><b>Multiblock footprint</b>: CE's real ICF reactor structure is a bespoke hollow lattice of
 *   {@code icf_component} blocks in specific meta-coded rows/columns
 *   ({@code TileEntityICFStruct.cbarp}), validated as pure per-offset data independent of any block
 *   entity. This port's {@link com.hbm.blocks.machine.fusion.IcfReactorBlock} instead uses the
 *   already-shipped {@link com.hbm.blocks.BlockDummyable} box-dimensions contract (a uniform casing
 *   shell, same mechanism {@code CyclotronBlock}/{@code CentrifugeBlock} already use) - a real,
 *   working multiblock with the same reaction/heat/fluid mechanics, just a simpler validated shape.
 *   Encoding CE's exact lattice as {@link com.hbm.handler.MultiblockHandlerXR} data is flagged as a
 *   follow-up, not attempted here.</li>
 *   <li><b>No separate depleted item</b>: Phase 1 ported {@link ItemICFPellet} as a single item with
 *   an in-place depletion data component (no {@code icf_pellet_depleted} registered - confirmed by
 *   grep, see the survey doc), unlike CE's item-swap-on-max-depletion design. This reactor keeps a
 *   spent pellet as the same item once {@link ItemICFPellet#getDepletion} reaches
 *   {@link ItemICFPellet#getMaxDepletion} and simply migrates it from the internal reacting slot to
 *   an output slot, matching CE's slot-shuffle behavior without needing the item swap.</li>
 * </ul>
 *
 * {@code tanks[0].setType(11)} Exact CE {@code TileEntityICF.java:82}. Slot 11 Exact CE
 * {@code ContainerICF.java:23}. Hopper {@code io} excludes 11 — CE {@code :52}.
 */
public class IcfReactorBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, IFluidStandardTransceiverMK2, IPersistentNBT, MenuProvider {

    public static final long MAX_HEAT = 1_000_000_000_000L;

    /** Slots 0-4: fresh pellet input. Slot 5: internal reacting slot. Slots 6-10: spent pellet output. Slot 11: coolant ID. */
    private static final int INPUT_START = 0;
    private static final int INPUT_END = 4;
    private static final int ACTIVE_SLOT = 5;
    private static final int OUTPUT_START = 6;
    private static final int OUTPUT_END = 10;
    private static final int SLOT_ID = 11;
    private static final int[] ACCESSIBLE = new int[]{0, 1, 2, 3, 4, 6, 7, 8, 9, 10};

    public final FluidTankNTM[] tanks = new FluidTankNTM[3];

    /** Laser power accumulated this tick by {@link #receiveLaser}, consumed and reset every {@link #updateEntity}. */
    public long laser;
    public long maxLaser;
    public long heat;
    public long heatup;
    public int consumption;
    public int output;

    public IcfReactorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 12, true, false);
        tanks[0] = new FluidTankNTM(Fluids.SODIUM, 512_000).withOwner(this);
        tanks[1] = new FluidTankNTM(Fluids.SODIUM_HOT, 512_000).withOwner(this);
        tanks[2] = new FluidTankNTM(Fluids.STELLAR_FLUX, 24_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineICF");
    }

    /** Called by {@link IcfControllerBlockEntity} once per tick it has line of sight to this reactor. */
    public void receiveLaser(long power, long maxPower) {
        this.laser += power;
        this.maxLaser += maxPower;
    }

    public DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX(), p.getY() + 3, p.getZ(), Direction.UP),
                new DirPos(p.getX(), p.getY() - 2, p.getZ(), Direction.DOWN),
                new DirPos(p.getX() + 3, p.getY(), p.getZ(), Direction.EAST),
                new DirPos(p.getX() - 3, p.getY(), p.getZ(), Direction.WEST),
                new DirPos(p.getX(), p.getY(), p.getZ() + 3, Direction.SOUTH),
                new DirPos(p.getX(), p.getY(), p.getZ() - 3, Direction.NORTH)
        };
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        // CE TileEntityICF.java:82
        tanks[0].setType(SLOT_ID, inventory);

        for (DirPos dp : getConPos()) {
            trySubscribe(tanks[0].getTankType(), level, dp);
        }

        boolean markDirty = false;

        // migrate a fully-spent pellet out of the active slot into the first free output slot
        ItemStack activeStack = inventory.getStackInSlot(ACTIVE_SLOT);
        if (!activeStack.isEmpty() && activeStack.getItem() instanceof ItemICFPellet
                && ItemICFPellet.getDepletion(activeStack) >= ItemICFPellet.getMaxDepletion(activeStack)) {
            for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
                if (inventory.getStackInSlot(i).isEmpty()) {
                    inventory.setStackInSlot(i, activeStack.copy());
                    inventory.setStackInSlot(ACTIVE_SLOT, ItemStack.EMPTY);
                    markDirty = true;
                    break;
                }
            }
        }

        // pull a fresh pellet into the active slot
        if (inventory.getStackInSlot(ACTIVE_SLOT).isEmpty()) {
            for (int i = INPUT_START; i <= INPUT_END; i++) {
                ItemStack candidate = inventory.getStackInSlot(i);
                if (!candidate.isEmpty() && candidate.getItem() instanceof ItemICFPellet) {
                    inventory.setStackInSlot(ACTIVE_SLOT, candidate.copy());
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                    markDirty = true;
                    break;
                }
            }
        }

        this.heatup = 0;

        ItemStack reacting = inventory.getStackInSlot(ACTIVE_SLOT);
        if (!reacting.isEmpty() && reacting.getItem() instanceof ItemICFPellet
                && ItemICFPellet.getFusingDifficulty(reacting) <= this.laser) {
            ItemStack copy = reacting.copy();
            this.heatup = ItemICFPellet.react(copy, this.laser);
            inventory.setStackInSlot(ACTIVE_SLOT, copy);
            this.heat += heatup;

            // stellar-flux byproduct only accrues on an actual reaction tick, matching CE
            tanks[2].setFill(tanks[2].getFill() + (int) Math.ceil(this.heat * 10.0D / MAX_HEAT));
            // CE TileEntityICF.java:130-131
            if (tanks[2].getFill() > tanks[2].getMaxFill()) tanks[2].setFill(tanks[2].getMaxFill());
        }

        if (heatup == 0) {
            this.heat += (long) (this.laser * 0.25D);
        }

        this.consumption = 0;
        this.output = 0;

        if (tanks[0].getTankType().hasTrait(FT_Heatable.class)) {
            FT_Heatable trait = tanks[0].getTankType().getTrait(FT_Heatable.class);
            HeatingStep step = trait.getFirstStep();
            tanks[1].setTankType(step.typeProduced);

            int coolingCycles = tanks[0].getFill() / step.amountReq;
            int heatingCycles = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.amountProduced;
            int heatCycles = (int) Math.min(this.heat / 4D / step.heatReq * trait.getEfficiency(HeatingType.ICF),
                    (double) this.heat / step.heatReq);
            int cycles = Math.min(coolingCycles, Math.min(heatingCycles, heatCycles));

            tanks[0].setFill(tanks[0].getFill() - step.amountReq * cycles);
            tanks[1].setFill(tanks[1].getFill() + step.amountProduced * cycles);
            this.heat -= (long) step.heatReq * cycles;

            this.consumption = step.amountReq * cycles;
            this.output = step.amountProduced * cycles;
        }

        for (DirPos dp : getConPos()) {
            tryProvide(tanks[1], level, dp);
            tryProvide(tanks[2], level, dp);
        }

        this.heat = (long) (this.heat * 0.999D);
        if (this.heat > MAX_HEAT) this.heat = MAX_HEAT;
        if (markDirty) setChanged();

        // laser is a per-tick accumulator, reset for the next tick's receiveLaser calls
        this.laser = 0;
        this.maxLaser = 0;

        dataChanged();
        networkPackMK2(150);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        // CE :210-211 is pellet-only (slot < 5). MenuBase.tile is getCheckedInventory(),
        // so ID GUI insert dies without this — same as IcfPress / SILEX.
        if (slot == SLOT_ID) return stack.getItem() instanceof IItemFluidIdentifier;
        return slot >= INPUT_START && slot <= INPUT_END && stack.getItem() instanceof ItemICFPellet;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= OUTPUT_START && slot <= OUTPUT_END;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return ACCESSIBLE;
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tanks);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tanks[1], tanks[2]);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks[0]);
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < 3; i++) tanks[i].writeToNBT(tag, "tank" + i);
        tag.putLong("heat", heat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < 3; i++) tanks[i].readFromNBT(tag, "tank" + i);
        this.heat = tag.getLong("heat");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(laser);
        buf.writeLong(maxLaser);
        buf.writeLong(heat);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.laser = buf.readLong();
        this.maxLaser = buf.readLong();
        this.heat = buf.readLong();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        for (int i = 0; i < 3; i++) tanks[i].writeToNBT(nbt, "t" + i);
        nbt.putLong("heat", heat);
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        for (int i = 0; i < 3; i++) tanks[i].readFromNBT(nbt, "t" + i);
        this.heat = nbt.getLong("heat");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new IcfReactorMenu(containerId, playerInventory, this);
    }
}
