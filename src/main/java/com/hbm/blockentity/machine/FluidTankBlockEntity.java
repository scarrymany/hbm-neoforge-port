package com.hbm.blockentity.machine;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;

/**
 * Single-block TE-backed fluid storage tank, backed by the just-landed
 * {@link com.hbm.inventory.fluid.tank.FluidTankNTM}/{@code com.hbm.api.fluidmk2} network trio.
 *
 * <h2>Why this is a new block, not a port of CE's {@code machine_fluidtank}</h2>
 * CE's only mass-fluid-storage block, {@code TileEntityMachineFluidTank} (732 lines, read in full -
 * see {@code docs/phase2/machines_storage.md}), is a 5x5 {@code BlockDummyable} multiblock wired into
 * the control-panel event system (remote mode-switching), a fluid hazard/trait system
 * ({@code FT_Corrosive}/{@code FT_Flammable}/antimatter explosions), item-canister auto-fill/drain
 * slots gated on {@code IItemFluidIdentifier} items, a player-climbable ladder hitbox, and
 * OpenComputers callbacks. Only the fluid-tank data class and its network API
 * (this port's own recent-landing {@code FluidTankNTM}/{@code fluidmk2} trio) are actually needed by
 * this storage-machines pass; the rest are large, separate cross-cutting systems (multiblock casing,
 * control panels, hazard/fluid-trait interactions, canister-identifier items) this package was not
 * asked to build and that do not otherwise gate "a working TE-backed fluid storage block existing."
 * <p>
 * So rather than stub out a {@code machine_fluidtank} that only superficially resembles CE's real
 * multiblock (and would need renaming or reworking the moment that real multiblock lands), this class
 * is registered under a distinct id ({@code machine_fluidtank_basic}) as a deliberately-scoped-down,
 * single-block predecessor: a plain 1x1x1 fluid buffer, transceiving fluid through
 * {@link IFluidStandardTransceiverMK2} on all six faces, with a mode toggle (receive-only / both /
 * send-only / disabled - CE's own four tank modes) but none of CE's multiblock-only behavior. CE's
 * real {@code machine_fluidtank} id is intentionally left unclaimed for whoever ports the full
 * multiblock later (needs: the control-panel event system, the fluid hazard/trait interaction system,
 * canister-identifier items, and the multiblock casing framework applied to an 8-port 5x5 footprint -
 * none of which this pass was asked to build).
 *
 * <h2>Mode toggle without GUI-button packet infrastructure</h2>
 * Same cross-cutting gap as {@link BatteryBlockEntity}'s javadoc (no server-bound GUI-button packet
 * exists yet). Unlike the battery, this block's {@link FluidTankBlock} instead cycles the mode via a
 * plain sneak-right-click-with-empty-hand interaction (server-side field mutation, no new packet
 * needed) - a reasonable, self-contained substitute for a GUI button that doesn't require inventing
 * shared packet infrastructure this single block doesn't otherwise need.
 */
public class FluidTankBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, IFluidStandardTransceiverMK2, IPersistentNBT {

    public static final int MODE_RECEIVE = 0;
    public static final int MODE_BOTH = 1;
    public static final int MODE_SEND = 2;
    public static final int MODE_DISABLED = 3;
    private static final int MODE_COUNT = 4;

    public static final int CAPACITY_MB = 256_000;

    private final FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, CAPACITY_MB).withOwner(this);
    private short mode = MODE_BOTH;
    private byte lastRedstone = 0;

    public FluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0, true, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fluidtank");
    }

    public FluidTankNTM getTank() {
        return tank;
    }

    public short getMode() {
        return mode;
    }

    /** Cycles receive-only -> both -> send-only -> disabled -> receive-only, matching CE's four tank modes. */
    public void cycleMode() {
        mode = (short) ((mode + 1) % MODE_COUNT);
        setChanged();
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (mode != MODE_DISABLED) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = worldPosition.relative(dir);
                if (mode == MODE_SEND || mode == MODE_BOTH) {
                    tryProvide(tank, level, neighbor, dir);
                }
                if (mode == MODE_RECEIVE || mode == MODE_BOTH) {
                    trySubscribe(tank.getTankType(), level, neighbor, dir);
                }
            }
        }

        byte comp = tank.getRedstoneComparatorPower();
        if (comp != lastRedstone) {
            setChanged();
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        lastRedstone = comp;

        networkPackNT(64);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return Collections.singletonList(tank);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return (mode == MODE_SEND || mode == MODE_BOTH) ? Collections.singletonList(tank) : Collections.emptyList();
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return (mode == MODE_RECEIVE || mode == MODE_BOTH) ? Collections.singletonList(tank) : Collections.emptyList();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(mode);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        mode = buf.readShort();
        tank.deserialize(buf);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.writeToNBT(tag, "tank");
        tag.putShort("mode", mode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(tag, "tank");
        mode = tag.getShort("mode");
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        if (tank.getFill() <= 0) return;
        CompoundTag data = new CompoundTag();
        tank.writeToNBT(data, "tank");
        data.putShort("mode", mode);
        nbt.put(NBT_PERSISTENT_KEY, data);
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        if (!nbt.contains(NBT_PERSISTENT_KEY)) return;
        CompoundTag data = nbt.getCompound(NBT_PERSISTENT_KEY);
        tank.readFromNBT(data, "tank");
        mode = data.getShort("mode");
    }

    // setDestroyedByCreativePlayer()/isDestroyedByCreativePlayer() are not overridden here -
    // MachineBaseBlockEntity already implements both concretely, satisfying IPersistentNBT's two
    // abstract methods without a second, shadowing flag on this subclass.
}
