package com.hbm.render.anim;

import com.hbm.render.anim.BusAnimationKeyframe.IType;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of CE's {@code com.hbm.render.anim.BusAnimationSequence} (169 lines) - one named "bus"
 * worth of keyframe data across all 9 {@link Dimension} channels (3 translate + 3 rotate + 3
 * scale), plus the model-space {@link #offset} and {@link #rotMode} axis-swizzle. Read in full
 * directly from CE for this port; ported verbatim, no behavioral changes - structurally identical
 * to the already-committed {@link com.hbm.render.anim.sedna.BusAnimationSequenceSedna}, which this
 * class predates in CE (this is the legacy, hand-authored-in-Java engine; Sedna's is the newer,
 * JSON-driven one - see {@link BusAnimation}'s own javadoc for why the two are kept separate here
 * too).
 *
 * <p>{@link #getTransformation(int)} is the one method every renderer/animation-state consumer
 * actually calls per frame - it returns a flat {@code double[15]}: indices 0-2 translate, 3-5
 * rotate (degrees), 6-8 scale, 9-11 the model-space offset, 12-14 the XYZ rotation-order swizzle
 * (as small integer-valued doubles 0/1/2, indexing back into 3-5).
 */
public class BusAnimationSequence {

    public enum Dimension {
        TX,
        TY,
        TZ,
        RX,
        RY,
        RZ,
        SX,
        SY,
        SZ;

        public static final Dimension[] VALUES = values();
    }

    // Storing a matrix of keyframe data, each keyframe stores a SINGLE dimension, so we can stagger frames over each parameter
    private final List<List<BusAnimationKeyframe>> transformKeyframes = new ArrayList<>(9);

    public double[] offset = new double[3];

    // swizzle me timbers (CE's own comment) - default identity order X,Y,Z
    public double[] rotMode = new double[]{0, 1, 2};

    public BusAnimationSequence() {
        // Initialize our keyframe storage, since it's multidimensional
        for (int i = 0; i < 9; i++) {
            transformKeyframes.add(new ArrayList<>());
        }
    }

    /** Adds a keyframe to the given dimension. */
    public BusAnimationSequence addKeyframe(Dimension dimension, BusAnimationKeyframe keyframe) {
        transformKeyframes.get(dimension.ordinal()).add(keyframe);
        return this;
    }

    public BusAnimationSequence addKeyframe(Dimension dimension, double value, int duration) {
        return addKeyframe(dimension, new BusAnimationKeyframe(value, duration));
    }

    /** Adds a position with a duration of 0. */
    public BusAnimationSequence setPos(double x, double y, double z) {
        return addPos(x, y, z, 0, IType.LINEAR);
    }

    /** Adds a position with the desired duration and lininterp. */
    public BusAnimationSequence addPos(double x, double y, double z, int duration) {
        return addPos(x, y, z, duration, IType.LINEAR);
    }

    /** Adds a position with the desired duration and interpolation type. */
    public BusAnimationSequence addPos(double x, double y, double z, int duration, IType type) {
        addKeyframe(Dimension.TX, new BusAnimationKeyframe(x, duration, type));
        addKeyframe(Dimension.TY, new BusAnimationKeyframe(y, duration, type));
        addKeyframe(Dimension.TZ, new BusAnimationKeyframe(z, duration, type));
        return this;
    }

    public BusAnimationSequence addRot(double x, double y, double z, int duration) {
        addKeyframe(Dimension.RX, new BusAnimationKeyframe(x, duration));
        addKeyframe(Dimension.RY, new BusAnimationKeyframe(y, duration));
        addKeyframe(Dimension.RZ, new BusAnimationKeyframe(z, duration));
        return this;
    }

    /** Repeats the previous keyframe with the same values using lininterp. Effectively makes the animation frame pause for the desired amount of milliseconds. */
    public BusAnimationSequence hold(int duration) {
        addKeyframe(Dimension.TX, new BusAnimationKeyframe(getLast(Dimension.TX), duration));
        addKeyframe(Dimension.TY, new BusAnimationKeyframe(getLast(Dimension.TY), duration));
        addKeyframe(Dimension.TZ, new BusAnimationKeyframe(getLast(Dimension.TZ), duration));
        return this;
    }

    /** Repeats the previous keyframe for a duration depending on the previous keyframes. Useful for getting different buses to sync up. */
    public BusAnimationSequence holdUntil(int end) {
        int duration = end - getTotalTime();
        // FIXME (CE's own comment, preserved): holdUntil breaks as soon as the animation speed is not 1
        return hold(duration);
    }

    public BusAnimationSequence multiplyTime(double mult) {
        for (Dimension dim : Dimension.VALUES) {
            List<BusAnimationKeyframe> keyframes = transformKeyframes.get(dim.ordinal());
            for (BusAnimationKeyframe keyframe : keyframes) keyframe.duration = (int) (keyframe.originalDuration * mult);
        }
        return this;
    }

    /** Legacy triplet-keyframe entry point: an XYZ-combined {@link BusAnimationKeyframe} expands into three per-axis TX/TY/TZ keyframes; a plain keyframe goes onto TX. */
    public BusAnimationSequence addKeyframe(BusAnimationKeyframe keyframe) {
        if (keyframe.legacyTriplet) {
            addKeyframe(Dimension.TX, new BusAnimationKeyframe(keyframe.legacyX, keyframe.duration, keyframe.interpolationType));
            addKeyframe(Dimension.TY, new BusAnimationKeyframe(keyframe.legacyY, keyframe.duration, keyframe.interpolationType));
            addKeyframe(Dimension.TZ, new BusAnimationKeyframe(keyframe.legacyZ, keyframe.duration, keyframe.interpolationType));
        } else {
            addKeyframe(Dimension.TX, keyframe);
        }
        return this;
    }

    /** Grabs the numerical value for the most recent keyframe on the given dimension. */
    private double getLast(Dimension dim) {
        BusAnimationKeyframe frame = getLastFrame(dim);
        return frame != null ? frame.value : 0D;
    }

    private BusAnimationKeyframe getLastFrame(Dimension dim) {
        List<BusAnimationKeyframe> keyframes = transformKeyframes.get(dim.ordinal());
        if (keyframes.isEmpty()) return null;
        return keyframes.get(keyframes.size() - 1);
    }

    /**
     * Samples every dimension at {@code millis} and packs the result into the flat 15-element
     * transform array described in this class's own javadoc. All transformation data is absolute,
     * additive transformations have not yet been implemented (CE's own comment/limitation,
     * preserved).
     */
    public double[] getTransformation(int millis) {
        double[] transform = new double[15];

        for (int i = 0; i < 9; i++) {
            List<BusAnimationKeyframe> keyframes = transformKeyframes.get(i);

            BusAnimationKeyframe currentFrame = null;
            BusAnimationKeyframe previousFrame = null;

            int startTime = 0;
            int endTime = 0;
            for (BusAnimationKeyframe keyframe : keyframes) {
                startTime = endTime;
                endTime += keyframe.duration;
                previousFrame = currentFrame;
                currentFrame = keyframe;
                if (millis < endTime) break;
            }

            if (currentFrame == null) {
                // Scale defaults to 1, others are 0
                transform[i] = i >= 6 ? 1 : 0;
                continue;
            }

            if (millis >= endTime || currentFrame.duration == 0) {
                transform[i] = currentFrame.value;
                continue;
            }

            if (previousFrame != null && previousFrame.interpolationType == IType.CONSTANT) {
                transform[i] = previousFrame.value;
                continue;
            }

            transform[i] = currentFrame.interpolate(startTime, millis, previousFrame);
        }

        transform[9] = offset[0];
        transform[10] = offset[1];
        transform[11] = offset[2];

        transform[12] = rotMode[0];
        transform[13] = rotMode[1];
        transform[14] = rotMode[2];

        return transform;
    }

    public int getTotalTime() {
        int highestTime = 0;

        for (List<BusAnimationKeyframe> keyframes : transformKeyframes) {
            int time = 0;
            for (BusAnimationKeyframe frame : keyframes) {
                time += frame.duration;
            }

            highestTime = Math.max(time, highestTime);
        }

        return highestTime;
    }
}
