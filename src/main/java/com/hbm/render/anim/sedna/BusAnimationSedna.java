package com.hbm.render.anim.sedna;

import java.util.HashMap;
import java.util.Map;

/**
 * Port of CE's {@code com.hbm.render.anim.sedna.BusAnimationSedna} (100 lines) - one complete named
 * animation (e.g. {@code "Fire"}, {@code "Reload"}, {@code "EQUIP"} - the top-level keys under a
 * JSON file's {@code "anim"} object, or a key handed straight to
 * {@link BusAnimationSedna#addBus(String, BusAnimationSequenceSedna)} by a hand-authored,
 * programmatic lambda such as CE's {@code XFactory9mm.LAMBDA_UZI_ANIMS}), holding one
 * {@link BusAnimationSequenceSedna} per named "bus" (model part / transform channel). Read in full
 * directly from CE for this port; ported verbatim (CE's own class javadoc comment - "this duplicate
 * abomination was done solely for sedna guns" - preserved below for provenance/context).
 *
 * <p>CE's own comment: "'buses' with one S since it's not a vehicle" - multiple buses exist
 * simultaneously and start at 0. A bus has one authority, i.e. the translation of a single part of
 * a model or the rotation of the entire thing. Imagine the buses being film strips that hang from
 * the ceiling, with the tape player rolling down, picking up images from all tapes and combining
 * them into a movie.
 */
public class BusAnimationSedna {

    private final HashMap<String, BusAnimationSequenceSedna> animationBuses = new HashMap<>();

    /** 0 by default, will always equal the duration of the longest {@link BusAnimationSequenceSedna}. */
    private int totalTime = 0;

    /**
     * Adds a bus to the animation. If an object has several moving parts, each transformation type
     * of each separate bus should have its own bus - unless you use one bus for several things
     * because the animation is identical, that's ok too (CE's own comment, preserved).
     */
    public BusAnimationSedna addBus(String name, BusAnimationSequenceSedna bus) {
        animationBuses.put(name, bus);

        int duration = bus.getTotalTime();

        if (duration > totalTime)
            totalTime = duration;

        return this;
    }

    /** In case there is keyframes being added to sequences in post, this method allows the totalTime to be updated. */
    public void updateTime() {
        for (Map.Entry<String, BusAnimationSequenceSedna> sequence : animationBuses.entrySet()) {
            int time = sequence.getValue().getTotalTime();

            if (time > totalTime)
                totalTime = time;
        }
    }

    /** Gets a bus from the specified name. Usually not something you want to do (CE's own comment). */
    public BusAnimationSequenceSedna getBus(String name) {
        return animationBuses.get(name);
    }

    /** Multiplies all keyframe durations by the supplied double. Numbers below 1 make the animation play faster. */
    public void setTimeMult(double mult) {
        for (Map.Entry<String, BusAnimationSequenceSedna> sequence : animationBuses.entrySet()) {
            sequence.getValue().multiplyTime(mult);
        }
    }

    /**
     * Gets the state of a bus at a specified time.
     *
     * @param name   the name of the bus in question
     * @param millis the elapsed time since the animation started in milliseconds
     */
    public double[] getTimedTransformation(String name, int millis) {
        if (this.animationBuses.containsKey(name))
            return animationBuses.get(name).getTransformation(millis);

        return null;
    }

    public int getDuration() {
        return totalTime;
    }
}
