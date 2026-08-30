package com.hbm.blockentity.machine;

import com.hbm.api.block.ILockable;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.machine.BlockSiloHatch;
import com.hbm.blocks.machine.LaunchInfraBlocks;
import com.hbm.interfaces.IAnimatedDoor;
import com.hbm.lib.HBMSoundHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * Ported from CE's {@code com.hbm.tileentity.machine.TileEntitySiloHatch} (256 lines, read in full)
 * - a large sliding blast door: T-flip-flop redstone, lock/key security (implements this port's
 * already-real {@link ILockable} - the first concrete implementor, see that interface's own javadoc
 * noting it had none yet), and a 100-tick open/close animation that places/removes a 3x3
 * {@link DummyBlockEntity} ring 3 blocks in front of itself via {@link BlockSiloHatch#FACING}.
 * <p>
 * <b>Door-state sync, adapted</b>: CE pushes state changes via a dedicated
 * {@code TEDoorAnimationPacket} sent only to nearby trackers. This port has no equivalent
 * per-feature packet infrastructure for this yet, so door state instead rides the same generic
 * {@link #serialize}/{@link #deserialize} sync every {@link LoadedBaseBlockEntity} already has
 * ({@link #networkPackNT} on state-change ticks) - {@link #deserialize} calls
 * {@link #handleNewState} when the synced state actually changes, preserving CE's client-side
 * "play the hydraulics sound only on a stationary-to-moving transition" behavior exactly via
 * {@link IAnimatedDoor#clientAnimStart}.
 * <p>
 * <b>Not ported</b>: {@code RadiationSystemNT.markSectionsForRebuild} calls - that whole radiation
 * simulation system is not ported anywhere in this tree yet (confirmed absent by grep), matching
 * {@link com.hbm.interfaces.IRadResistantBlock}'s own documented gap.
 */
public class SiloHatchBlockEntity extends LoadedBaseBlockEntity implements ITickableBE, IAnimatedDoor, ILockable {

    public DoorState state = DoorState.CLOSED;
    public long sysTime;
    public int timer = -1;
    @Nullable
    public Direction facing = null;
    private boolean wasPowered = false;
    private boolean redstoneOnly = false;

    private boolean locked = false;
    private int pins = 0;
    private double mod = 1.0D;

    public SiloHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        // T-flip-flop redstone behavior
        if (!this.isLocked()) {
            boolean isPowered = level.hasNeighborSignal(worldPosition);
            if (isPowered && !wasPowered) {
                tryToggle();
            }
            wasPowered = isPowered;
        }

        DoorState oldState = timer < 0 ? null : state;

        if (this.state.isStationaryState()) {
            timer = 0;
        } else {
            if (facing == null) {
                facing = level.getBlockState(worldPosition).getValue(BlockSiloHatch.FACING).getOpposite();
            }
            timer++;

            if (state == DoorState.CLOSING) {
                if (timer == 50) {
                    BlockPos mid = worldPosition.relative(facing, 3);
                    for (int i = -1; i <= 1; i++) {
                        for (int j = -1; j <= 1; j++) {
                            placeDummy(mid.offset(i, 0, j));
                        }
                    }
                }
                if (timer > 100) {
                    state = DoorState.CLOSED;
                    // TODO(radiation-system): CE marks the occupied radiation sections for rebuild
                    // here (RadiationSystemNT.markSectionsForRebuild) - not ported yet, see class javadoc.
                }
            } else if (state == DoorState.OPENING) {
                if (timer == 70) {
                    BlockPos mid = worldPosition.relative(facing, 3);
                    for (int i = -1; i <= 1; i++) {
                        for (int j = -1; j <= 1; j++) {
                            removeDummy(mid.offset(i, 0, j));
                        }
                    }
                }
                if (timer > 100) {
                    state = DoorState.OPEN;
                }
            }
        }

        if (oldState != state) {
            networkPackNT(200);
        }
    }

    public void tryToggle() {
        if (state == DoorState.CLOSED) {
            tryOpen();
        } else if (state == DoorState.OPEN) {
            tryClose();
        }
    }

    public void tryOpen() {
        if (this.state == DoorState.CLOSED && level != null && !level.isClientSide) {
            open();
            timer = -1;
        }
    }

    public void tryClose() {
        if (this.state == DoorState.OPEN && level != null && !level.isClientSide) {
            close();
            timer = -1;
        }
    }

    public boolean placeDummy(BlockPos target) {
        if (level == null || !level.getBlockState(target).canBeReplaced()) return false;

        level.setBlockAndUpdate(target, LaunchInfraBlocks.DUMMY_BLOCK_SILO_HATCH.get().defaultBlockState());

        if (level.getBlockEntity(target) instanceof DummyBlockEntity dummy) {
            dummy.target = this.worldPosition;
        }

        return true;
    }

    public void removeDummy(BlockPos target) {
        if (level == null) return;
        if (level.getBlockState(target).is(LaunchInfraBlocks.DUMMY_BLOCK_SILO_HATCH.get())) {
            level.setBlockAndUpdate(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        state = DoorState.VALUES[tag.getByte("state")];
        wasPowered = tag.getBoolean("wasPowered");
        redstoneOnly = tag.getBoolean("redstoneOnly");
        locked = tag.getBoolean("locked");
        pins = tag.getInt("pins");
        mod = tag.getDouble("mod");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("state", (byte) state.ordinal());
        tag.putBoolean("wasPowered", wasPowered);
        tag.putBoolean("redstoneOnly", redstoneOnly);
        tag.putBoolean("locked", locked);
        tag.putInt("pins", pins);
        tag.putDouble("mod", mod);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeByte((byte) state.ordinal());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        DoorState newState = DoorState.VALUES[buf.readByte()];
        if (level != null && level.isClientSide) {
            handleNewState(newState);
        } else {
            this.state = newState;
        }
    }

    public AABB getRenderBoundingBox() {
        if (facing == null && level != null) {
            facing = level.getBlockState(worldPosition).getValue(BlockSiloHatch.FACING).getOpposite();
        }
        if (facing == null) return new AABB(worldPosition);
        BlockPos mid = worldPosition.relative(facing, 3);
        return new AABB(-3.3, 0, -3.3, 4.3, 2, 4.3).move(mid);
    }

    @Override
    public void open() {
        if (state == DoorState.CLOSED) toggle();
    }

    @Override
    public void close() {
        if (state == DoorState.OPEN) toggle();
    }

    @Override
    public DoorState getState() {
        return state;
    }

    @Override
    public void toggle() {
        if (state == DoorState.CLOSED) {
            state = DoorState.OPENING;
        } else if (state == DoorState.OPEN) {
            state = DoorState.CLOSING;
        }
        // TODO(radiation-system): CE marks occupied radiation sections for rebuild on every toggle -
        // not ported yet, see class javadoc.
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleNewState(DoorState newState) {
        if (state != newState) {
            if (state.isStationaryState() && newState.isMovingState() && level != null && facing != null) {
                BlockPos hydraulics = worldPosition.relative(facing, 5);
                boolean opening = newState == DoorState.OPENING;
                level.playSound(null, hydraulics.getX() + 0.5, hydraulics.getY() + 0.5, hydraulics.getZ() + 0.5,
                        opening ? HBMSoundHandler.siloopen.get() : HBMSoundHandler.siloclose.get(),
                        SoundSource.BLOCKS, opening ? 4F : 3F, 1F);
            }
            sysTime = IAnimatedDoor.clientAnimStart(state, newState, sysTime);
            state = newState;
        }
    }

    @Override
    public boolean getRedstoneOnly() {
        return redstoneOnly;
    }

    @Override
    public void setRedstoneOnly(boolean redstoneOnly) {
        this.redstoneOnly = redstoneOnly;
    }

    // --- ILockable -----------------------------------------------------------------------------

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public void lock() {
        this.locked = true;
        setChanged();
    }

    @Override
    public void unlock() {
        this.locked = false;
        setChanged();
    }

    @Override
    public void setPins(int pins) {
        this.pins = pins;
    }

    @Override
    public int getPins() {
        return pins;
    }

    @Override
    public void setMod(double mod) {
        this.mod = mod;
    }

    @Override
    public double getMod() {
        return mod;
    }
}
