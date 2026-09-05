package com.hbm.blockentity.machine;

import com.hbm.api.block.ILockable;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IAnimatedDoor;
import com.hbm.items.tool.ItemKey;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code TileEntitySlidingBlastDoor}. TESR skins leftover
 * TODO(CE: TileEntitySlidingBlastDoor.java:360-376): texture 0/1/2 OBJ.
 */
public class SlidingBlastDoorBlockEntity extends LoadedBaseBlockEntity implements ITickableBE, IAnimatedDoor, ILockable {

    public DoorState state = DoorState.CLOSED;
    public byte texture;
    public long sysTime;
    public boolean shouldUseBB = true;
    public boolean keypadLocked;
    private int timer;
    private boolean wasPowered;
    private boolean redstoneOnly;
    private boolean locked;
    private int pins;
    private double mod = 1.0D;

    public SlidingBlastDoorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (!isLocked() && !keypadLocked) {
            boolean isPowered = level.hasNeighborSignal(worldPosition);
            if (isPowered && !wasPowered) tryToggle();
            wasPowered = isPowered;
        }

        DoorState oldState = state;
        if (state.isStationaryState()) {
            timer = 0;
        } else {
            timer++;
            if (state == DoorState.CLOSING) {
                if (timer == 2) {
                    placeDummy(-2);
                    placeDummy(2);
                } else if (timer == 6) {
                    placeDummy(-1);
                    placeDummy(1);
                } else if (timer == 12) {
                    placeDummy(0);
                }
                if (timer > 24) {
                    state = DoorState.CLOSED;
                    if (state != oldState) {
                        RadiationSystemNT.markSectionsForRebuild(level, occupiedBlocks());
                    }
                }
            } else if (state == DoorState.OPENING) {
                if (timer == 12) removeDummy(0);
                else if (timer == 16) {
                    removeDummy(-1);
                    removeDummy(1);
                } else if (timer == 20) {
                    removeDummy(-2);
                    removeDummy(2);
                }
                if (timer > 24) state = DoorState.OPEN;
            }
        }
        networkPackNT(100);
    }

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

    public boolean tryToggle(Player player) {
        if (state == DoorState.CLOSED) return tryOpen(player);
        if (state == DoorState.OPEN) return tryClose(player);
        return false;
    }

    public boolean tryOpen(Player player) {
        if (state != DoorState.CLOSED) return false;
        if (level != null && !level.isClientSide && canAccess(player)) open();
        return true;
    }

    public boolean tryClose(Player player) {
        if (state != DoorState.OPEN) return false;
        if (level != null && !level.isClientSide && canAccess(player)) close();
        return true;
    }

    public boolean canAccess(Player player) {
        if (keypadLocked && player != null) return false;
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

    private Direction facing() {
        return Direction.from3DDataValue(getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset);
    }

    private BlockPos offsetPos(int offset) {
        return switch (facing()) {
            case SOUTH -> worldPosition.offset(offset, 0, 0);
            case NORTH -> worldPosition.offset(-offset, 0, 0);
            case EAST -> worldPosition.offset(0, 0, offset);
            case WEST -> worldPosition.offset(0, 0, -offset);
            default -> worldPosition;
        };
    }

    /** CE name: closing restores collision ({@code removeExtra}). */
    private void placeDummy(int offset) {
        if (!(getBlockState().getBlock() instanceof BlockDummyable dummyable)) return;
        BlockPos p = offsetPos(offset);
        if (offset == 0) shouldUseBB = true;
        else dummyable.removeExtra(level, p);
        dummyable.removeExtra(level, p.above(1));
        dummyable.removeExtra(level, p.above(2));
        dummyable.removeExtra(level, p.above(3));
    }

    /** CE name: opening punches holes ({@code makeExtra}). */
    private void removeDummy(int offset) {
        if (!(getBlockState().getBlock() instanceof BlockDummyable dummyable)) return;
        BlockPos p = offsetPos(offset);
        BlockDummyable.safeRem = true;
        if (offset == 0) shouldUseBB = false;
        else dummyable.makeExtra(level, p);
        dummyable.makeExtra(level, p.above(1));
        dummyable.makeExtra(level, p.above(2));
        dummyable.makeExtra(level, p.above(3));
        BlockDummyable.safeRem = false;
    }

    private List<BlockPos> occupiedBlocks() {
        List<BlockPos> out = new ArrayList<>(20);
        for (int offset = -2; offset <= 2; offset++) {
            BlockPos p = offsetPos(offset);
            for (int y = 0; y <= 3; y++) out.add(p.above(y));
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
    public void setTextureState(byte tex) {
        this.texture = tex;
    }

    @Override
    public boolean setTexture(String tex) {
        if ("sliding_blast_door".equals(tex)) {
            texture = 0;
            return true;
        }
        if ("sliding_blast_door_variant1".equals(tex)) {
            texture = 1;
            return true;
        }
        if ("sliding_blast_door_variant2".equals(tex)) {
            texture = 2;
            return true;
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleNewState(DoorState newState) {
        if (this.state != newState && level != null) {
            if (this.state == DoorState.CLOSED && newState == DoorState.OPENING) {
                level.playSound(null, worldPosition, HBMSoundHandler.qe_sliding_opening.get(), SoundSource.BLOCKS, 2F, 1F);
            }
            if (this.state == DoorState.OPEN && newState == DoorState.CLOSING) {
                level.playSound(null, worldPosition, HBMSoundHandler.qe_sliding_opening.get(), SoundSource.BLOCKS, 2F, 1F);
            }
            if (this.state.isMovingState() && newState.isStationaryState()) {
                level.playSound(null, worldPosition,
                        newState == DoorState.OPEN ? HBMSoundHandler.qe_sliding_opened.get() : HBMSoundHandler.qe_sliding_shut.get(),
                        SoundSource.BLOCKS, 2F, 1F);
            }
            if (this.state.isStationaryState() && newState.isMovingState()) {
                sysTime = System.currentTimeMillis();
            }
            this.state = newState;
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
        buf.writeBoolean(shouldUseBB);
        buf.writeByte(state.ordinal());
        buf.writeByte(texture);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        shouldUseBB = buf.readBoolean();
        DoorState newState = DoorState.VALUES[buf.readByte()];
        texture = buf.readByte();
        if (level != null && level.isClientSide) handleNewState(newState);
        else this.state = newState;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        state = DoorState.VALUES[tag.getByte("state")];
        sysTime = tag.getLong("sysTime");
        timer = tag.getInt("timer");
        wasPowered = tag.getBoolean("wasPowered");
        keypadLocked = tag.getBoolean("keypadLocked");
        shouldUseBB = tag.getBoolean("shouldUseBB");
        redstoneOnly = tag.getBoolean("redstoneOnly");
        texture = tag.getByte("texture");
        locked = tag.getBoolean("locked");
        pins = tag.getInt("pins");
        mod = tag.getDouble("mod");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("state", (byte) state.ordinal());
        tag.putLong("sysTime", sysTime);
        tag.putInt("timer", timer);
        tag.putBoolean("wasPowered", wasPowered);
        tag.putBoolean("keypadLocked", keypadLocked);
        tag.putBoolean("shouldUseBB", shouldUseBB);
        tag.putBoolean("redstoneOnly", redstoneOnly);
        tag.putByte("texture", texture);
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
