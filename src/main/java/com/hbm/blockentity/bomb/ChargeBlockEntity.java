package com.hbm.blockentity.bomb;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.bomb.BlockChargeBase;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.ModContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Ported from CE's {@code com.hbm.tileentity.bomb.TileEntityCharge} (107 lines, read in full) -
 * {@code docs/phase3/bomb_blocks_and_detonators.md} Section A. Maps onto {@link LoadedBaseBlockEntity}
 * (no inventory) per that report's confirmed base-class mapping. Countdown ticks down once
 * {@link #started}; on reaching zero it resolves the detonating {@link Entity} from
 * {@link ModContext#DETONATOR_CONTEXT} if set (used by call sites this package doesn't otherwise
 * populate - kept for parity with the shared thread-local contract), else looks up
 * {@link #placerID} via the server's player list (the placer may be offline - a {@code null}
 * detonator is a valid, already-handled case downstream in every {@code IBomb} implementor).
 */
public class ChargeBlockEntity extends LoadedBaseBlockEntity implements ITickableBE {

    public boolean started;
    public int timer;
    @Nullable
    public UUID placerID;

    public ChargeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide()) return;

        if (started) {
            timer--;

            if (timer % 20 == 0 && timer > 0) {
                level.playSound(null, worldPosition, HBMSoundHandler.fstbmbPing.get(), SoundSource.BLOCKS, 10.0F, 1.0F);
            }

            if (timer <= 0) {
                Entity detonator = ModContext.DETONATOR_CONTEXT.get();
                if (detonator == null && placerID != null && level instanceof ServerLevel serverLevel) {
                    detonator = serverLevel.getServer().getPlayerList().getPlayer(placerID);
                }

                if (level.getBlockState(worldPosition).getBlock() instanceof BlockChargeBase chargeBlock) {
                    chargeBlock.explode(level, worldPosition, detonator);
                }
            }
        }

        networkPackNT(100);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(this.timer);
        buf.writeBoolean(this.started);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.timer = buf.readInt();
        this.started = buf.readBoolean();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("timer", timer);
        tag.putBoolean("started", started);
        if (placerID != null) tag.putUUID("placer", placerID);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        timer = tag.getInt("timer");
        started = tag.getBoolean("started");
        placerID = tag.hasUUID("placer") ? tag.getUUID("placer") : null;
    }

    public String getMinutes() {
        String mins = "" + (timer / 1200);
        if (mins.length() == 1) mins = "0" + mins;
        return mins;
    }

    public String getSeconds() {
        String secs = "" + ((timer / 20) % 60);
        if (secs.length() == 1) secs = "0" + secs;
        return secs;
    }
}
