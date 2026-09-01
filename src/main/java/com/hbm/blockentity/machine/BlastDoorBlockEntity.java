package com.hbm.blockentity.machine;

import com.hbm.api.block.ILockable;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.generic.GenericBlocks;
import com.hbm.blocks.machine.DummyBlockBlast;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IAnimatedDoor;
import com.hbm.items.tool.ItemKey;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code TileEntityBlastDoor}. Control-panel leftover
 * TODO(CE: TileEntityBlastDoor.java:359-395): {@code IControllable} door_toggle / door_open_state.
 */
public class BlastDoorBlockEntity extends LoadedBaseBlockEntity implements ITickableBE, IAnimatedDoor, ILockable {

    public DoorState state = DoorState.CLOSED;
    public long sysTime;
    private int timer;
    private boolean wasPowered;
    private boolean redstoneOnly;
    private boolean locked;
    private int pins;
    private double mod = 1.0D;

    public BlastDoorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (!isLocked()) {
            boolean isPowered = level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(worldPosition.above(6));
            if (isPowered && !wasPowered) {
                tryToggle();
            }
            wasPowered = isPowered;
        }

        if (state.isStationaryState()) {
            timer = 0;
        } else {
            timer++;
            if (state == DoorState.OPENING) {
                if (timer >= 0) removeDummy(worldPosition.above(1));
                if (timer >= 20) removeDummy(worldPosition.above(2));
                if (timer >= 40) removeDummy(worldPosition.above(3));
                if (timer >= 60) removeDummy(worldPosition.above(4));
                if (timer >= 80) removeDummy(worldPosition.above(5));
            } else {
                if (timer >= 20) placeDummy(worldPosition.above(5));
                if (timer >= 40) placeDummy(worldPosition.above(4));
                if (timer >= 60) placeDummy(worldPosition.above(3));
                if (timer >= 80) placeDummy(worldPosition.above(2));
                if (timer >= 100) placeDummy(worldPosition.above(1));
            }
            if (timer >= 100) {
                if (state == DoorState.OPENING) {
                    state = DoorState.OPEN;
                } else if (state == DoorState.CLOSING) {
                    state = DoorState.CLOSED;
                    RadiationSystemNT.markSectionsForRebuild(level, occupiedBlocks());
                }
            }
        }
        networkPackNT(150);
    }

    public boolean tryToggle() {
        if (state == DoorState.CLOSED) return tryOpen();
        if (state == DoorState.OPEN) return tryClose();
        return false;
    }

    public boolean tryOpen() {
        if (state == DoorState.CLOSED) {
            if (level != null && !level.isClientSide) {
                open();
                openNeigh();
            }
            return true;
        }
        return false;
    }

    public boolean tryClose() {
        if (state == DoorState.OPEN) {
            if (level != null && !level.isClientSide) {
                close();
                closeNeigh();
            }
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
            level.playSound(null, player.blockPosition(), HBMSoundHandler.lockOpen.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ok;
    }

    public boolean canToggleRedstone(Player player) {
        if (!isLocked()) return true;
        if (player == null) return false;
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof ItemKeyPin && ItemKeyPin.getPins(off) == pins;
    }

    public boolean placeDummy(BlockPos pos) {
        if (level == null || !level.getBlockState(pos).canBeReplaced()) return false;
        level.setBlockAndUpdate(pos, GenericBlocks.DUMMY_BLOCK_BLAST.get().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof DummyBlockEntity dummy) {
            dummy.target = this.worldPosition;
        }
        return true;
    }

    public void removeDummy(BlockPos pos) {
        if (level == null) return;
        if (level.getBlockState(pos).is(GenericBlocks.DUMMY_BLOCK_BLAST.get())) {
            DummyBlockBlast.safeBreak = true;
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            DummyBlockBlast.safeBreak = false;
        }
    }

    private void forNeigh(java.util.function.Consumer<BlastDoorBlockEntity> fn) {
        if (level == null) return;
        for (BlockPos p : new BlockPos[]{
                worldPosition.offset(1, 0, 0), worldPosition.offset(-1, 0, 0),
                worldPosition.offset(0, 0, 1), worldPosition.offset(0, 0, -1)}) {
            BlockEntity te = level.getBlockEntity(p);
            if (te instanceof BlastDoorBlockEntity door) fn.accept(door);
        }
    }

    public void openNeigh() {
        forNeigh(n -> {
            if (n.state == DoorState.CLOSED && (!n.isLocked() || n.pins == pins)) {
                n.open();
                n.openNeigh();
            }
        });
    }

    public void closeNeigh() {
        forNeigh(n -> {
            if (n.state == DoorState.OPEN && (!n.isLocked() || n.pins == pins)) {
                n.close();
                n.closeNeigh();
            }
        });
    }

    public void lockNeigh() {
        forNeigh(n -> {
            if (!n.isLocked()) {
                n.setPins(pins);
                n.lock();
                n.setMod(mod);
            }
        });
    }

    private List<BlockPos> occupiedBlocks() {
        List<BlockPos> out = new ArrayList<>(6);
        for (int i = 0; i <= 5; i++) {
            out.add(worldPosition.above(i));
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
            sysTime = System.currentTimeMillis();
            networkPackNT(150);
            closeNeigh();
            RadiationSystemNT.markSectionsForRebuild(level, occupiedBlocks());
        } else if (state == DoorState.OPEN) {
            state = DoorState.CLOSING;
            sysTime = System.currentTimeMillis();
            networkPackNT(150);
            openNeigh();
            RadiationSystemNT.markSectionsForRebuild(level, occupiedBlocks());
        }
        setChanged();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleNewState(DoorState newState) {
        if (state != newState && level != null) {
            if (state.isStationaryState() && newState.isMovingState()) {
                level.playSound(null, worldPosition, HBMSoundHandler.reactorStart.get(), SoundSource.BLOCKS, 0.5F, 0.75F);
            } else if (state.isMovingState() && newState.isStationaryState()) {
                level.playSound(null, worldPosition, HBMSoundHandler.reactorStop.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
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

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeByte(state.ordinal());
        buf.writeLong(sysTime);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        DoorState newState = DoorState.VALUES[buf.readByte()];
        long synced = buf.readLong();
        if (level != null && level.isClientSide) {
            handleNewState(newState);
            if (!newState.isMovingState()) sysTime = synced;
        } else {
            this.state = newState;
            this.sysTime = synced;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        state = DoorState.VALUES[tag.getInt("state")];
        sysTime = tag.getLong("sysTime");
        timer = tag.getInt("timer");
        wasPowered = tag.getBoolean("wasPowered");
        redstoneOnly = tag.getBoolean("redstoneOnly");
        locked = tag.getBoolean("locked");
        pins = tag.getInt("pins");
        mod = tag.getDouble("mod");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("state", state.ordinal());
        tag.putLong("sysTime", sysTime);
        tag.putInt("timer", timer);
        tag.putBoolean("wasPowered", wasPowered);
        tag.putBoolean("redstoneOnly", redstoneOnly);
        tag.putBoolean("locked", locked);
        tag.putInt("pins", pins);
        tag.putDouble("mod", mod);
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public void lock() {
        locked = true;
        setChanged();
        lockNeigh();
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
