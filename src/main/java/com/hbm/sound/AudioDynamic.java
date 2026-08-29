package com.hbm.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Core looping/attenuating client sound instance, ported from CE's {@code AudioDynamic}
 * (which extended vanilla {@code MovingSound}; the 1.21.1 equivalent base is
 * {@link AbstractTickableSoundInstance}, {@code update()} became {@code tick()}).
 * <p>
 * CE carried two distance-attenuation modes selected by the {@code nonLegacy} constructor flag:
 * older call sites (mostly weapon/tool loops predating {@code AudioWrapper.getLoopedSound})
 * pass {@code false} and get the original {@code intendedVolume}-relative falloff driven by
 * {@link #setAttenuation}, while newer call sites pass {@code true} and get straightforward
 * range-relative falloff. Both are preserved here exactly as CE had them.
 */
@OnlyIn(Dist.CLIENT)
public class AudioDynamic extends AbstractTickableSoundInstance {

    public float maxVolume = 1;
    public float range;
    public float intendedVolume;
    public int keepAlive;
    public int timeSinceKA;
    public boolean shouldExpire = false;
    private final boolean nonLegacy;
    // shitty addition that should make looped sounds on tools and guns work right
    // position updates happen automatically and if the parent is the client player, volume is always on max
    public Entity parentEntity = null;

    protected AudioDynamic(SoundEvent event, SoundSource source, boolean useNewSystem) {
        super(event, source, RandomSource.create());
        this.looping = true;
        this.attenuation = Attenuation.NONE;
        this.intendedVolume = 10;
        this.range = 10;
        this.nonLegacy = useNewSystem;
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void attachTo(Entity e) {
        this.parentEntity = e;
    }

    public void setAttenuation(Attenuation type) {
        this.attenuation = type;
        volume = intendedVolume;
    }

    @Override
    public void tick() {
        Player player = Minecraft.getInstance().player;
        float f;

        if (parentEntity != null && player != parentEntity) {
            this.setPosition((float) parentEntity.getX(), (float) parentEntity.getY(), (float) parentEntity.getZ());
        }

        // only adjust volume over distance if the sound isn't attached to this entity
        if (nonLegacy) {
            if (player != null && player != parentEntity) {
                f = (float) Math.sqrt(Math.pow(x - player.getX(), 2)
                        + Math.pow(y - player.getY(), 2)
                        + Math.pow(z - player.getZ(), 2));
                volume = func(f);
            } else {
                // shitty hack that prevents stereo weirdness when using 0 0 0
                if (player == parentEntity) {
                    this.setPosition((float) parentEntity.getX(), (float) parentEntity.getY() + 10, (float) parentEntity.getZ());
                }
                volume = maxVolume;
            }

            if (this.shouldExpire) {
                if (this.timeSinceKA > this.keepAlive) {
                    this.stop();
                }
                this.timeSinceKA++;
            }
        } else {
            if (player != null && player != parentEntity) {
                f = (float) Math.sqrt(Math.pow(x - player.getX(), 2)
                        + Math.pow(y - player.getY(), 2)
                        + Math.pow(z - player.getZ(), 2));

                if (attenuation == Attenuation.LINEAR) {
                    volume = func(f);
                } else {
                    volume = func(f, intendedVolume);
                }
            } else {
                if (player == parentEntity) {
                    this.setPosition((float) parentEntity.getX(), (float) parentEntity.getY() + 10, (float) parentEntity.getZ());
                }
                volume = intendedVolume;
            }
        }
    }

    public void start() {
        Minecraft.getInstance().getSoundManager().play(this);
    }

    // named doneStopWhatever, not stop, because AbstractTickableSoundInstance.stop() is final
    // (it just sets the stopped/looping flags for the engine to notice on its next poll) - this
    // one force-stops the sound immediately, matching CE's original AudioDynamic.stop() behavior
    public void doneStopWhatever() {
        Minecraft.getInstance().getSoundManager().stop(this);
    }

    public void setVolume(float volume) {
        this.maxVolume = volume;
    }

    public void setRange(float range) {
        this.range = range;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
        this.shouldExpire = true;
    }

    public void keepAlive() {
        this.timeSinceKA = 0;
    }

    public float func(float f, float v) {
        return (f / v) * -2 + 2;
    }

    public float func(float dist) {
        return (dist / range) * -maxVolume + maxVolume;
    }

    public boolean isPlaying() {
        return Minecraft.getInstance().getSoundManager().isActive(this);
    }
}
