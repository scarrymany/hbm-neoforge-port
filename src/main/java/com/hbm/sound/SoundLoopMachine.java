package com.hbm.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Generic block-entity-bound ambient loop, ported from CE's {@code SoundLoopMachine}
 * ({@code PositionedSound implements ITickableSound} became {@link AbstractTickableSoundInstance};
 * {@code update()}/{@code donePlaying}/{@code isDonePlaying()} became {@code tick()} calling the
 * base class's inherited {@code stop()}, which flips {@code isStopped()} for the sound engine to
 * notice on its next poll). Self-stops once its block entity is removed; base class for the
 * per-machine loop sounds (centrifuge, broadcaster, boiler, turbofan, siren).
 */
@OnlyIn(Dist.CLIENT)
public class SoundLoopMachine extends AbstractTickableSoundInstance {

    private final BlockEntity blockEntity;

    public SoundLoopMachine(SoundEvent event, BlockEntity blockEntity) {
        super(event, SoundSource.BLOCKS, RandomSource.create());
        this.looping = true;
        this.volume = 1;
        this.pitch = 1;
        this.x = blockEntity.getBlockPos().getX();
        this.y = blockEntity.getBlockPos().getY();
        this.z = blockEntity.getBlockPos().getZ();
        this.blockEntity = blockEntity;
    }

    @Override
    public void tick() {
        if (blockEntity == null || blockEntity.isRemoved())
            this.stop();
    }

    public void setVolume(float f) {
        volume = f;
    }

    public void setPitch(float f) {
        pitch = f;
    }

    // named requestStop, not stop, because AbstractTickableSoundInstance.stop() is protected
    // final - this exposes the same "mark done, engine reaps it next poll" behavior publicly
    // for callers outside this package (e.g. the TileEntity that owns this loop)
    public void requestStop() {
        this.stop();
    }

    public BlockEntity getBlockEntity() {
        return blockEntity;
    }
}
