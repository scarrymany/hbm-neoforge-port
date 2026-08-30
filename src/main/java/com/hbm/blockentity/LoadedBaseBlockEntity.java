package com.hbm.blockentity;

import com.hbm.api.tile.ILoadedTile;
import com.hbm.config.GeneralConfig;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.packet.toclient.BufPacket;
import com.hbm.sound.AudioWrapper;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionType;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * Universal block-entity base, ported from CE's {@code com.hbm.tileentity.TileEntityLoadedBase}
 * (288 lines, read in full). Package root is {@code com.hbm.blockentity} per this port's own
 * package-naming decision (see {@code docs/phase2/multiblock_framework.md} decision 1, already
 * applied to the {@link IPersistentNBT} sibling class in this same package) - CE's {@code TileEntity}
 * suffix/package becomes {@code BlockEntity}, matching both the class this actually extends and Neo
 * Edition's own real (confirmed running) {@code com.hbm.blockentity.LoadedBaseBlockEntity}.
 *
 * <p>Owns: {@link #isLoaded}/{@link #onLoad()}/{@link #onChunkUnloaded()} tracking; {@code muffled}/
 * {@code tilted} fields with NBT round-trip via {@link #saveAdditional}/{@link #loadAdditional}
 * (CE's {@code writeToNBT}/{@code readFromNBT}, renamed and re-parented per Neo Edition's confirmed
 * 1.21.1 shape - {@code protected void saveAdditional(CompoundTag, HolderLookup.Provider)} /
 * {@code protected void loadAdditional(CompoundTag, HolderLookup.Provider)}, both {@code super}-first
 * exactly like CE's own chaining); a {@link RegistryFriendlyByteBuf}-based sync pair
 * ({@link #serialize}/{@link #deserialize}, {@link #serializeInitial}/{@link #deserializeInitial}
 * for the one-time chunk-load payload, carried through {@link #getUpdateTag}/{@link #handleUpdateTag}
 * exactly like CE used {@code getUpdateTag}/{@code handleUpdateTag} over
 * {@code SPacketUpdateTileEntity}); two throttled sync-packet senders ({@link #networkPackNT} and
 * {@link #networkPackMK2}); and the {@link #checkTilt}/{@link TiltType} machine-gravity wobble
 * effect with the three fixed floor-shape helpers.
 *
 * <p><b>Networking note</b>: {@link #getUpdateTag}/{@link #handleUpdateTag}/{@link #getUpdatePacket}
 * are {@code final} exactly like CE's equivalents, on purpose - CE's own javadoc on
 * {@code serialize}/{@code deserialize} says "only call {@code super.serialize()} on noisy
 * machines", i.e. every subclass extension point for syncable data is the ByteBuf pair, never a
 * direct {@code getUpdateTag} override. This differs from Neo Edition's real class (which does not
 * implement chunk-load full sync at all, and separately does not mark these methods {@code final}
 * so at least one of its own subclasses, {@code RadioTorchBaseBlockEntity}, augments the tag
 * directly) - CE remains the source of truth for behavior here, and CE's design is that
 * {@code getUpdateTag} is a sealed implementation detail of the sync pair, not a second extension
 * point. A subclass that needs extra one-time chunk-load data adds it to
 * {@link #serializeInitial}/{@link #deserializeInitial} instead.
 */
public class LoadedBaseBlockEntity extends BlockEntity implements ILoadedTile, IBufPacketReceiver {

    public boolean isLoaded = true;
    public boolean muffled = false;
    public boolean tilted = false;
    public int tiltBlocksChecked = 0;
    public int tiltBlocksValid = 0;

    protected boolean hasDataChanged = true;
    private byte[] lastPackedBufData = null;

    public LoadedBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * @return if the block entity is loaded. Note that even if it's loaded, it may be invalid!
     */
    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        isLoaded = true;
    }

    /**
     * 1.21.1's replacement for CE's {@code onChunkUnload()} - confirmed real override, same name
     * and no-arg shape, cross-checked against Neo Edition's own
     * {@code LoadedBaseBlockEntity}/{@code DoorGenericBlockEntity}.
     */
    @Override
    public void onChunkUnloaded() {
        isLoaded = false;
    }

    /** Vidarin: Remember to override this if you use {@link #rebootAudio(AudioWrapper)}!! */
    public AudioWrapper createAudioLoop() {
        return null;
    }

    public AudioWrapper rebootAudio(AudioWrapper wrapper) {
        wrapper.stopSound();
        AudioWrapper audio = createAudioLoop();
        audio.startSound();
        return audio;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        muffled = tag.getBoolean("muffled");
        tilted = tag.getBoolean("tilted");
        hasDataChanged = true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("muffled", muffled);
        tag.putBoolean("tilted", tilted);
    }

    public float getVolume(float baseVolume) {
        return muffled ? baseVolume * 0.1F : baseVolume;
    }

    public void setMuffled(boolean muffled) {
        this.muffled = muffled;
        dataChanged();
    }

    public void dataChanged() {
        hasDataChanged = true;
    }

    @Override
    public final CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (level == null) return tag;
        RegistryFriendlyByteBuf buf =
                new RegistryFriendlyByteBuf(Unpooled.buffer(64), level.registryAccess(), ConnectionType.OTHER);
        try {
            serializeInitial(buf);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            tag.putByteArray("hbmSync", bytes);
        } finally {
            buf.release();
        }
        return tag;
    }

    @Override
    public final void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (level == null || !tag.contains("hbmSync")) return;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.wrappedBuffer(tag.getByteArray("hbmSync")), level.registryAccess(), ConnectionType.OTHER);
        try {
            deserializeInitial(buf);
        } finally {
            buf.release();
        }
    }

    @Override
    public final Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * {@inheritDoc}
     * only call super.serialize() on noisy machines. It has no effect on others.<br>
     * The final ByteBuf is compared with the previously sent one in order to avoid unnecessary
     * traffic - see {@link #networkPackNT(int)}.
     */
    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(muffled);
        buf.writeBoolean(tilted);
    }

    /**
     * {@inheritDoc}
     * only call super.deserialize() on noisy machines. It has no effect on others.
     */
    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        muffled = buf.readBoolean();
        tilted = buf.readBoolean();
    }

    /**
     * Payload emitted once per chunk-load sync via {@link #getUpdateTag}. Defaults to the per-tick
     * {@link #serialize(RegistryFriendlyByteBuf)} payload so block entities that sync everything
     * per-tick need no extra work.
     */
    public void serializeInitial(RegistryFriendlyByteBuf buf) {
        serialize(buf);
    }

    /**
     * Symmetric counterpart to {@link #serializeInitial(RegistryFriendlyByteBuf)}. Invoked from
     * {@link #handleUpdateTag(CompoundTag, HolderLookup.Provider)} on the main client thread during
     * chunk data resolution, after the standard NBT path has already applied
     * {@link #loadAdditional}, so it must not depend on pre-existing field values.
     */
    public void deserializeInitial(RegistryFriendlyByteBuf buf) {
        deserialize(buf);
    }

    /**
     * Sends a sync packet that uses a ByteBuf for efficient information-cramming. Skips sending
     * when the payload is byte-for-byte identical to the last one sent (CE compared a cheap FNV-1a
     * hash of the compiled buffer instead of the raw bytes - this port has no equivalent hashing
     * utility ported yet, so it compares the byte array directly; behaviorally identical, just a
     * few bytes more work per call for machines with a large per-tick payload).
     */
    public void networkPackNT(int range) {
        if (level == null || level.isClientSide) return;

        RegistryFriendlyByteBuf buf =
                new RegistryFriendlyByteBuf(Unpooled.buffer(64), level.registryAccess(), ConnectionType.OTHER);
        byte[] data;
        try {
            serialize(buf);
            data = new byte[buf.readableBytes()];
            buf.readBytes(data);
        } finally {
            buf.release();
        }

        if (Arrays.equals(data, lastPackedBufData)) return;
        lastPackedBufData = data;

        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersNear(serverLevel, null,
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), range,
                    new BufPacket(worldPosition, data));
        }
    }

    /**
     * Sends a sync packet, skipping compilation entirely when data has not changed.
     * <p>
     * Block entities using this must call {@link #dataChanged()} whenever any synced field
     * changes. Failing to do so will cause clients to never receive the update.
     */
    public void networkPackMK2(int range) {
        if (level == null || level.isClientSide) return;
        if (!hasDataChanged) return;

        RegistryFriendlyByteBuf buf =
                new RegistryFriendlyByteBuf(Unpooled.buffer(64), level.registryAccess(), ConnectionType.OTHER);
        byte[] data;
        try {
            serialize(buf);
            data = new byte[buf.readableBytes()];
            buf.readBytes(data);
        } finally {
            buf.release();
        }

        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersNear(serverLevel, null,
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), range,
                    new BufPacket(worldPosition, data));
        }
        hasDataChanged = false;
    }

    public enum TiltType {
        UNAVOIDABLE, CONFIG
    }

    /**
     * CE's material-based floor-quality checks (sand/cloth/ground {@code Material}, and a named
     * ban on {@code dirt_dead}/{@code dirt_oily}/{@code stone_cracked}) are deferred here: vanilla's
     * {@code Material} enum no longer exists in modern Minecraft, and Neo Edition's own real port of
     * this exact method (cross-checked for API shape) left the same checks as an explicit
     * {@code // todo materials} gap rather than resolving them. Only the structural checks
     * (sturdy-topped, explosion-resistant-enough-for-extra-heavy) are ported for now; a future pass
     * can add a tag- or block-set-based material check once this base package's consumers need one.
     */
    public void checkTilt(TiltType cfg, boolean extraHeavy) {
        if (level == null) return;

        boolean doesTilt = false;
        if (cfg == TiltType.UNAVOIDABLE) doesTilt = true;
        if (cfg == TiltType.CONFIG && GeneralConfig.ENABLE_MACHINE_GRAVITY.get()) doesTilt = true;
        if (cfg == TiltType.CONFIG && GeneralConfig.ENABLE_528.get() && GeneralConfig.X528_ENABLE_MACHINE_GRAVITY.get())
            doesTilt = true;

        if (!doesTilt) { this.tilted = false; return; }
        if (this.getFloorCount() <= 0) { this.tilted = false; return; }
        BlockPos pos = worldPosition;
        if ((level.getGameTime() + (pos.getY() + pos.getZ() * 27644437) * 27644437L + pos.getX()) % 20 != 0) return;

        if (this.tiltBlocksChecked >= this.getFloorCount()) {

            if (this.tiltBlocksValid >= this.tiltBlocksChecked * 0.95) {
                this.tilted = false;
            } else {
                if (!this.tilted) {
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            HBMSoundHandler.metalImpact.get(), SoundSource.BLOCKS, 3F, 1F);
                }
                this.tilted = true;
            }

            this.setChanged();
            this.tiltBlocksChecked = 0;
            this.tiltBlocksValid = 0;
        }

        BlockPos floorPos = getFloorPosFromIndex(this.tiltBlocksChecked);
        if (floorPos == null) return;

        BlockState ground = level.getBlockState(floorPos);
        this.tiltBlocksChecked++;

        // for extra heavy machines, the ground needs to:
        // * have a sturdy top face
        // * have an explosion resistance of stone or greater
        if (extraHeavy) {
            if (!ground.isFaceSturdy(level, floorPos, Direction.UP)) return;
            Block block = ground.getBlock();
            if (block.getExplosionResistance() < Blocks.STONE.getExplosionResistance()) return;
            this.tiltBlocksValid++;
        // for standard machines, the ground just needs a sturdy top face
        } else {
            if (!ground.isFaceSturdy(level, floorPos, Direction.UP)) return;
            this.tiltBlocksValid++;
        }
    }

    public int getFloorCount() { return 0; }

    @Nullable
    public BlockPos getFloorPosFromIndex(int index) { return null; }

    public BlockPos standardFloor3x3(int index) {
        return worldPosition.offset(-1 + (index / 2) * 2, -1, -1 + (index % 2) * 2);
    }

    public BlockPos standardFloor5x5(int index) {
        return worldPosition.offset(-2 + (index / 3) * 2, -1, -2 + (index % 3) * 2);
    }

    public BlockPos standardFloor7x7(int index) {
        return worldPosition.offset(-3 + (index / 4) * 2, -1, -3 + (index % 4) * 2);
    }
}
