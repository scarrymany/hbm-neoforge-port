package com.hbm.sound;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Extends {@link AudioWrapperClient} with a one-shot start/stop {@link SoundEvent} played
 * around the loop's lifecycle, ported from CE's {@code AudioWrapperClientStartStop}
 * ({@code World.playSound(...)} became {@link Level#playLocalSound}).
 * <p>
 * <b>Deviation from CE:</b> CE's constructor never assigns its {@code cat} field, so CE always
 * calls {@code World.playSound(...)} with a {@code null SoundCategory} for the start/stop cues
 * (a latent upstream bug - {@code cat} is dead and always null there). This port deliberately
 * assigns {@link #cat} from the constructor argument instead of reproducing that bug, because
 * {@link Level#playLocalSound} resolves a real {@link SoundSource} to read its volume slider and
 * would throw a {@link NullPointerException} given {@code null}, whereas CE's equivalent call
 * merely played the sound at the default category's implicit volume.
 */
@OnlyIn(Dist.CLIENT)
public class AudioWrapperClientStartStop extends AudioWrapperClient {

    public SoundEvent start;
    public SoundEvent stop;
    public Level world;
    public SoundSource cat;
    public float ssVol;
    public float x, y, z;

    public AudioWrapperClientStartStop(Level world, SoundEvent source, SoundEvent start, SoundEvent stop, float vol, SoundSource cat) {
        super(source, cat, false);
        if (sound != null) {
            sound.setVolume(vol);
            sound.setAttenuation(SoundInstance.Attenuation.LINEAR);
        }
        this.ssVol = vol;
        this.world = world;
        this.cat = cat;
        this.start = start;
        this.stop = stop;
    }

    @Override
    public void updatePosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        super.updatePosition(x, y, z);
    }

    @Override
    public void startSound() {
        if (start != null) {
            world.playLocalSound(x, y, z, start, cat, ssVol, 1, false);
        }
        super.startSound();
    }

    @Override
    public void stopSound() {
        if (stop != null) {
            world.playLocalSound(x, y, z, stop, cat, ssVol, 1, false);
        }
        super.stopSound();
    }

    @Override
    public float getVolume() {
        return ssVol;
    }
}
