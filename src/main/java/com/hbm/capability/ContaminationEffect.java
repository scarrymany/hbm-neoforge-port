package com.hbm.capability;

import net.minecraft.nbt.CompoundTag;

/**
 * A timed radiation-contamination effect (e.g. contaminated food, fallout exposure). Ported from
 * CE's {@code HbmLivingProps.ContaminationEffect}.
 *
 * <p>Mutable by design: {@link #time} counts down every tick as the effect wears off, so this is
 * a plain value holder rather than a record. Serialization piggybacks on the parent
 * {@link HbmLivingAttachment}'s single NBT blob (via {@link #save} / {@link #load}) instead of
 * carrying its own {@code Codec}/{@code StreamCodec} - the attachment already round-trips whole
 * through NBT for both disk and network, so a second serialization path here would be redundant.
 */
public final class ContaminationEffect {

    public double maxRad;
    public int maxTime;
    public int time;
    public boolean ignoreArmor;

    public ContaminationEffect(double maxRad, int time, boolean ignoreArmor) {
        this.maxRad = maxRad;
        this.maxTime = this.time = time;
        this.ignoreArmor = ignoreArmor;
    }

    private ContaminationEffect(double maxRad, int maxTime, int time, boolean ignoreArmor) {
        this.maxRad = maxRad;
        this.maxTime = maxTime;
        this.time = time;
        this.ignoreArmor = ignoreArmor;
    }

    public double getRad() {
        return maxRad * ((double) time / (double) maxTime);
    }

    public void save(CompoundTag tag, int index) {
        CompoundTag me = new CompoundTag();
        me.putDouble("maxRad", this.maxRad);
        me.putInt("maxTime", this.maxTime);
        me.putInt("time", this.time);
        me.putBoolean("ignoreArmor", this.ignoreArmor);
        tag.put("cont_" + index, me);
    }

    public static ContaminationEffect load(CompoundTag tag, int index) {
        // getCompound() never returns null - it falls back to an empty tag when the key is
        // absent, matching CE's null-safe NBTTagCompound#getCompoundTag behavior.
        CompoundTag me = tag.getCompound("cont_" + index);
        double maxRad = me.getDouble("maxRad");
        int maxTime = me.getInt("maxTime");
        int time = me.getInt("time");
        boolean ignoreArmor = me.getBoolean("ignoreArmor");
        return new ContaminationEffect(maxRad, maxTime, time, ignoreArmor);
    }
}
