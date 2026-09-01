package com.hbm.blockentity.machine;

import com.hbm.api.block.ILockable;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.config.MachineConfig;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IAnimatedDoor;
import com.hbm.interfaces.IDoor;
import com.hbm.items.tool.ItemKey;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.tileentity.DoorDecl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * CE {@code TileEntityDoorGeneric}. Control-panel {@code IControllable} leftover
 * TODO(CE: TileEntityDoorGeneric.java:464-517).
 */
public class DoorGenericBlockEntity extends LoadedBaseBlockEntity implements ITickableBE, IAnimatedDoor, ILockable {

    public DoorState state = DoorState.CLOSED;
    public int openTicks;
    public boolean shouldUseBB;
    public long animStartTime;
    private DoorDecl doorType;
    private int redstonePower;
    private boolean wasPowered;
    private boolean redstoneOnly;
    private byte skinIndex;
    private final Set<BlockPos> activatedBlocks = new HashSet<>();

    private boolean locked;
    private int pins;
    private double mod = 1.0D;

    public DoorGenericBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public DoorDecl getDoorType() {
        if (doorType == null && getBlockState().getBlock() instanceof BlockDoorGeneric door) {
            doorType = door.type;
        }
        return doorType;
    }

    @Override
    public void updateEntity() {
        DoorDecl decl = getDoorType();
        if (decl == null || level == null) return;
        try {
            Consumer<DoorGenericBlockEntity> update = decl.onDoorUpdate();
            if (update != null) update.accept(this);
            if (state == DoorState.OPENING) {
                openTicks++;
                if (openTicks >= decl.timeToOpen()) openTicks = decl.timeToOpen();
            } else if (state == DoorState.CLOSING) {
                openTicks--;
                if (openTicks <= 0) openTicks = 0;
            }
            if (!level.isClientSide) {
                applyOpenRanges(decl);
                if (state == DoorState.OPENING && openTicks == decl.timeToOpen()) {
                    state = DoorState.OPEN;
                }
                if (state == DoorState.CLOSING && openTicks == 0) {
                    state = DoorState.CLOSED;
                    RadiationSystemNT.markSectionsForRebuild(level, occupiedBlocks());
                }
                networkPackNT(100);
                boolean isPowered = redstonePower > 0;
                if (isPowered && !wasPowered) {
                    tryToggle(-1);
                }
                wasPowered = isPowered;
                if (redstonePower == -1) redstonePower = 0;
            }
        } catch (NullPointerException ignored) {
            // CE: structure gen can tick before dummy fill finishes.
        }
    }

    private void applyOpenRanges(DoorDecl decl) {
        if (!(getBlockState().getBlock() instanceof BlockDummyable dummyable)) return;
        int[][] ranges = decl.getDoorOpenRanges();
        Direction dir = facing();
        if (state == DoorState.OPENING) {
            for (int i = 0; i < ranges.length; i++) {
                walkRange(dummyable, ranges[i], decl.getDoorRangeOpenTime(openTicks, i), dir, true);
            }
        } else if (state == DoorState.CLOSING) {
            for (int i = 0; i < ranges.length; i++) {
                walkRange(dummyable, ranges[i], decl.getDoorRangeOpenTime(openTicks, i), dir, false);
            }
        }
    }

    private void walkRange(BlockDummyable dummyable, int[] range, float time, Direction dir, boolean opening) {
        int abs = Math.abs(range[3]);
        int sign = (int) Math.signum(range[3]);
        Rotation rot = rotationFor(dir);
        if (opening) {
            for (int j = 0; j < abs; j++) {
                if (abs > 1 && (float) j / (abs - 1) > time) break;
                stamp(dummyable, range, j, sign, rot, true);
            }
        } else {
            for (int j = abs - 1; j >= 0; j--) {
                if (abs > 1 && (float) j / (abs - 1) < time) break;
                stamp(dummyable, range, j, sign, rot, false);
            }
        }
    }

    private void stamp(BlockDummyable dummyable, int[] range, int j, int sign, Rotation rot, boolean extra) {
        for (int k = 0; k < range[4]; k++) {
            int ax = 0, ay = 0, az = 0;
            switch (range[5]) {
                case 0 -> { ay = k; az = sign * j; }
                case 1 -> { ax = k; ay = sign * j; }
                default -> { ax = sign * j; ay = k; }
            }
            BlockPos local = rotate(new BlockPos(range[0] + ax, range[1] + ay, range[2] + az), rot);
            BlockPos finalPos = worldPosition.offset(local);
            if (finalPos.equals(worldPosition)) {
                shouldUseBB = extra;
            } else if (extra) {
                dummyable.makeExtra(level, finalPos);
            } else {
                dummyable.removeExtra(level, finalPos);
            }
        }
    }

    public Direction facing() {
        return Direction.from3DDataValue(getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset);
    }

    /** CE: dir.getBlockRotation(); if X axis add CLOCKWISE_180. */
    public static Rotation rotationFor(Direction dir) {
        Rotation r = switch (dir) {
            case SOUTH -> Rotation.NONE;
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        if (dir.getAxis() == Direction.Axis.X) r = r.getRotated(Rotation.CLOCKWISE_180);
        return r;
    }

    public static BlockPos rotate(BlockPos p, Rotation r) {
        return switch (r) {
            case CLOCKWISE_90 -> new BlockPos(-p.getZ(), p.getY(), p.getX());
            case CLOCKWISE_180 -> new BlockPos(-p.getX(), p.getY(), -p.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(p.getZ(), p.getY(), -p.getX());
            default -> p;
        };
    }

    /** CE {@code tryToggle()} — no lock check (player already {@code canAccess}'d). */
    public boolean tryToggle() {
        if (state == DoorState.CLOSED) {
            if (level != null && !level.isClientSide) open();
            return true;
        }
        if (state == DoorState.OPEN) {
            if (level != null && !level.isClientSide) close();
            return true;
        }
        return false;
    }

    /** CE {@code tryToggle(int)} — locked && passcode != pins fails. Redstone uses {@code -1}. */
    public boolean tryToggle(int passcode) {
        if (state == DoorState.CLOSED) return tryOpen(passcode);
        if (state == DoorState.OPEN) return tryClose(passcode);
        return false;
    }

    private boolean tryOpen(int passcode) {
        if (isLocked() && passcode != pins) return false;
        if (state == DoorState.CLOSED) {
            if (level != null && !level.isClientSide) open();
            return true;
        }
        return false;
    }

    private boolean tryClose(int passcode) {
        if (isLocked() && passcode != pins) return false;
        if (state == DoorState.OPEN) {
            if (level != null && !level.isClientSide) close();
            return true;
        }
        return false;
    }

    public boolean canAccess(Player player) {
        if (!isLocked()) return true;
        ItemStack held = player.getMainHandItem();
        int heldPins = held.getItem() instanceof ItemKeyPin ? ItemKeyPin.getPins(held) : 0;
        boolean ok = canAccess(heldPins, held.getItem() instanceof ItemKey);
        if (ok && level != null) {
            level.playSound(null, player.blockPosition(), com.hbm.lib.HBMSoundHandler.lockOpen.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ok;
    }

    public boolean canToggleRedstone(Player player) {
        if (!isLocked()) return true;
        if (player == null) return false;
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof ItemKeyPin && ItemKeyPin.getPins(off) == pins;
    }

    public boolean cycleSkinIndex() {
        DoorDecl decl = getDoorType();
        if (decl == null || !decl.hasSkins()) return false;
        int count = Math.max(1, decl.getSkinCount());
        skinIndex = (byte) ((skinIndex + 1) % count);
        setChanged();
        return true;
    }

    public void updateRedstonePower(BlockPos pos) {
        if (level == null) return;
        boolean powered = level.hasNeighborSignal(pos);
        boolean contained = activatedBlocks.contains(pos);
        if (!contained && powered) {
            activatedBlocks.add(pos.immutable());
            if (redstonePower == -1) {
                redstonePower = 0;
                if (MachineConfig.HOLD_DOOR_REDSTONE.get()) tryToggle();
            }
            redstonePower++;
        } else if (contained && !powered) {
            activatedBlocks.remove(pos);
            redstonePower--;
            if (redstonePower == 0) {
                redstonePower = -1;
                if (MachineConfig.HOLD_DOOR_REDSTONE.get()) tryToggle();
            }
        }
    }

    private List<BlockPos> occupiedBlocks() {
        List<BlockPos> out = new ArrayList<>();
        out.add(worldPosition);
        DoorDecl decl = getDoorType();
        if (decl == null) return out;
        Direction dir = facing();
        Rotation rot = rotationFor(dir);
        for (int[] range : decl.getDoorOpenRanges()) {
            int abs = Math.abs(range[3]);
            int sign = (int) Math.signum(range[3]);
            for (int j = 0; j < abs; j++) {
                for (int k = 0; k < range[4]; k++) {
                    int ax = 0, ay = 0, az = 0;
                    switch (range[5]) {
                        case 0 -> { ay = k; az = sign * j; }
                        case 1 -> { ax = k; ay = sign * j; }
                        default -> { ax = sign * j; ay = k; }
                    }
                    out.add(worldPosition.offset(rotate(new BlockPos(range[0] + ax, range[1] + ay, range[2] + az), rot)));
                }
            }
        }
        return out;
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
            RadiationSystemNT.markSectionsForRebuild(level, occupiedBlocks());
        } else if (state == DoorState.OPEN) {
            state = DoorState.CLOSING;
            RadiationSystemNT.markSectionsForRebuild(level, occupiedBlocks());
        }
        setChanged();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleNewState(DoorState newState) {
        if (this.state != newState) {
            DoorDecl decl = getDoorType();
            if (decl != null && level != null) {
                if (this.state == DoorState.CLOSED && newState == DoorState.OPENING) {
                    playOnce(decl.getOpenSoundStart(), decl.getSoundVolume());
                }
                if (this.state == DoorState.OPEN && newState == DoorState.CLOSING) {
                    playOnce(decl.getCloseSoundStart(), decl.getSoundVolume());
                }
                if (this.state.isMovingState() && newState.isStationaryState()) {
                    playOnce(newState == DoorState.OPEN ? decl.getOpenSoundEnd() : decl.getCloseSoundEnd(),
                            decl.getSoundVolume());
                }
            }
            this.state = newState;
            if (state.isMovingState()) animStartTime = System.currentTimeMillis();
        }
    }

    private void playOnce(SoundEvent sound, float vol) {
        if (sound == null || level == null) return;
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, vol, 1F);
    }

    @Override
    public boolean getRedstoneOnly() {
        return redstoneOnly;
    }

    @Override
    public void setRedstoneOnly(boolean redstoneOnly) {
        this.redstoneOnly = redstoneOnly;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeByte(state.ordinal());
        buf.writeByte(skinIndex);
        buf.writeBoolean(shouldUseBB);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        DoorState newState = DoorState.VALUES[buf.readUnsignedByte()];
        skinIndex = buf.readByte();
        shouldUseBB = buf.readBoolean();
        if (level != null && level.isClientSide) handleNewState(newState);
        else this.state = newState;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        state = DoorState.VALUES[tag.getByte("state")];
        openTicks = tag.getInt("openTicks");
        redstonePower = tag.getInt("redstoned");
        shouldUseBB = tag.getBoolean("shouldUseBB");
        wasPowered = tag.getBoolean("wasPowered");
        redstoneOnly = tag.getBoolean("redstoneOnly");
        skinIndex = tag.getByte("skin");
        locked = tag.getBoolean("locked");
        pins = tag.getInt("pins");
        mod = tag.getDouble("mod");
        activatedBlocks.clear();
        CompoundTag act = tag.getCompound("activatedBlocks");
        int n = act.getAllKeys().size() / 3;
        for (int i = 0; i < n; i++) {
            activatedBlocks.add(new BlockPos(act.getInt("x" + i), act.getInt("y" + i), act.getInt("z" + i)));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("state", (byte) state.ordinal());
        tag.putInt("openTicks", openTicks);
        tag.putInt("redstoned", redstonePower);
        tag.putBoolean("shouldUseBB", shouldUseBB);
        tag.putBoolean("wasPowered", wasPowered);
        tag.putBoolean("redstoneOnly", redstoneOnly);
        tag.putByte("skin", skinIndex);
        tag.putBoolean("locked", locked);
        tag.putInt("pins", pins);
        tag.putDouble("mod", mod);
        CompoundTag act = new CompoundTag();
        int i = 0;
        for (BlockPos p : activatedBlocks) {
            act.putInt("x" + i, p.getX());
            act.putInt("y" + i, p.getY());
            act.putInt("z" + i, p.getZ());
            i++;
        }
        tag.put("activatedBlocks", act);
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public void lock() {
        locked = true;
        setChanged();
    }

    @Override
    public void unlock() {
        locked = false;
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
