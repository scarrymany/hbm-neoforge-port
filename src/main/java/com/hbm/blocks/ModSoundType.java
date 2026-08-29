package com.hbm.blocks;

import com.hbm.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SoundType;

/**
 * Ported from CE's {@code ModSoundType}. The 1.12 three-slot {@code SoundType} constructor
 * (place/break/step, with hit and fall implicitly reusing step) is gone; modern {@link SoundType}
 * takes all five sounds explicitly, confirmed against the Neo Edition reference's own
 * {@code ModSoundType} port. Hit and fall default to the step sound here, matching CE's original
 * behavior of not distinguishing them.
 */
public class ModSoundType extends SoundType {

    protected ModSoundType(SoundEvent placeSound, SoundEvent breakSound, SoundEvent stepSound,
                            SoundEvent hitSound, SoundEvent fallSound, float volume, float pitch) {
        super(volume, pitch, breakSound, stepSound, placeSound, hitSound, fallSound);
    }

    protected ModSoundType(SoundEvent placeSound, SoundEvent breakSound, SoundEvent stepSound, float volume, float pitch) {
        this(placeSound, breakSound, stepSound, stepSound, stepSound, volume, pitch);
    }

    public ModEnvelopedSoundType enveloped() {
        return new ModEnvelopedSoundType(getPlaceSound(), getBreakSound(), getStepSound(), getHitSound(), getFallSound(), volume, pitch);
    }

    public ModEnvelopedSoundType enveloped(RandomSource random) {
        return new ModEnvelopedSoundType(getPlaceSound(), getBreakSound(), getStepSound(), getHitSound(), getFallSound(), volume, pitch, random);
    }

    // creates a sound type with vanilla-like sound paths name-spaced to the mod
    public static ModSoundType mod(String soundName, float volume, float pitch) {
        return new ModSoundType(modDig(soundName), modDig(soundName), modStep(soundName), volume, pitch);
    }

    // these permutations allow creating a sound type with one of the three sounds being custom
    // and the other ones defaulting to vanilla-like sound paths name-spaced to the mod

    public static ModSoundType customPlace(String soundName, SoundEvent placeSound, float volume, float pitch) {
        return new ModSoundType(placeSound, modDig(soundName), modStep(soundName), volume, pitch);
    }

    public static ModSoundType customBreak(String soundName, SoundEvent breakSound, float volume, float pitch) {
        return new ModSoundType(modDig(soundName), breakSound, modStep(soundName), volume, pitch);
    }

    public static ModSoundType customStep(String soundName, SoundEvent stepSound, float volume, float pitch) {
        return new ModSoundType(modDig(soundName), modDig(soundName), stepSound, volume, pitch);
    }

    public static ModSoundType customDig(String soundName, SoundEvent digSound, float volume, float pitch) {
        return new ModSoundType(digSound, digSound, modStep(soundName), volume, pitch);
    }

    // these permutations copy sounds from an existing sound type and modify one of the sounds

    public static ModSoundType customPlace(SoundType from, SoundEvent placeSound, float volume, float pitch) {
        return new ModSoundType(placeSound, from.getBreakSound(), from.getStepSound(), from.getHitSound(), from.getFallSound(), volume, pitch);
    }

    public static ModSoundType customBreak(SoundType from, SoundEvent breakSound, float volume, float pitch) {
        return new ModSoundType(from.getPlaceSound(), breakSound, from.getStepSound(), from.getHitSound(), from.getFallSound(), volume, pitch);
    }

    public static ModSoundType customStep(SoundType from, SoundEvent stepSound, float volume, float pitch) {
        return new ModSoundType(from.getPlaceSound(), from.getBreakSound(), stepSound, from.getHitSound(), from.getFallSound(), volume, pitch);
    }

    public static ModSoundType customDig(SoundType from, SoundEvent dig, float volume, float pitch) {
        return new ModSoundType(dig, dig, from.getStepSound(), from.getHitSound(), from.getFallSound(), volume, pitch);
    }

    // customizes all sounds
    public static ModSoundType placeBreakStep(SoundEvent placeSound, SoundEvent breakSound, SoundEvent stepSound, float volume, float pitch) {
        return new ModSoundType(placeSound, breakSound, stepSound, volume, pitch);
    }

    private static SoundEvent modDig(String soundName) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "dig." + soundName));
    }

    private static SoundEvent modStep(String soundName) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "step." + soundName));
    }

    public static class ModEnvelopedSoundType extends ModSoundType {
        private final RandomSource random;

        ModEnvelopedSoundType(SoundEvent placeSound, SoundEvent breakSound, SoundEvent stepSound,
                               SoundEvent hitSound, SoundEvent fallSound, float volume, float pitch, RandomSource random) {
            super(placeSound, breakSound, stepSound, hitSound, fallSound, volume, pitch);
            this.random = random;
        }

        ModEnvelopedSoundType(SoundEvent placeSound, SoundEvent breakSound, SoundEvent stepSound,
                              SoundEvent hitSound, SoundEvent fallSound, float volume, float pitch) {
            this(placeSound, breakSound, stepSound, hitSound, fallSound, volume, pitch, RandomSource.create());
        }

        // a bit of a hack, but most of the time, the sound path is queried first, then volume and pitch
        private SubType probableSubType = SubType.PLACE;

        @Override
        public SoundEvent getPlaceSound() {
            probableSubType = SubType.PLACE;
            return super.getPlaceSound();
        }

        @Override
        public SoundEvent getBreakSound() {
            probableSubType = SubType.BREAK;
            return super.getBreakSound();
        }

        @Override
        public SoundEvent getStepSound() {
            probableSubType = SubType.STEP;
            return super.getStepSound();
        }

        private Envelope volumeEnvelope = null;
        private Envelope pitchEnvelope = null;

        public ModEnvelopedSoundType volumeFunction(Envelope volumeEnvelope) {
            this.volumeEnvelope = volumeEnvelope;
            return this;
        }

        public ModEnvelopedSoundType pitchFunction(Envelope pitchEnvelope) {
            this.pitchEnvelope = pitchEnvelope;
            return this;
        }

        @Override
        public float getVolume() {
            return volumeEnvelope == null ? super.getVolume() : volumeEnvelope.compute(super.getVolume(), random, probableSubType);
        }

        @Override
        public float getPitch() {
            return pitchEnvelope == null ? super.getPitch() : pitchEnvelope.compute(super.getPitch(), random, probableSubType);
        }

        @FunctionalInterface
        public interface Envelope {
            float compute(float in, RandomSource rand, SubType type);
        }
    }

    public enum SubType {
        PLACE, BREAK, STEP
    }
}
