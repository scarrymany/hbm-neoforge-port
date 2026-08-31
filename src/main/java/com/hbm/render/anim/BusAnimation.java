package com.hbm.render.anim;

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Port of CE's {@code com.hbm.render.anim.BusAnimation} (92 lines) - one complete named animation,
 * holding one {@link BusAnimationSequence} per named "bus" (model part / transform channel). This
 * is the older, simpler, hand-authored-in-Java animation engine CE uses for a small set of items -
 * {@link com.hbm.items.IAnimatedItem#getAnimation}'s return type. It is <b>not</b> the same system
 * as the already-committed {@link com.hbm.render.anim.sedna.BusAnimationSedna} (CE's newer,
 * JSON-driven, more sophisticated gun-animation engine, ported in Phase 5): CE genuinely maintains
 * both side by side (Sedna's own class javadoc, preserved on that class: "this duplicate
 * abomination was done solely for sedna guns"; the file header on file predates it: "Th3_Sl1ze: for
 * now I'll leave it because of good old Drillgon guns"). Do not merge the two - they are read from
 * separate CE source files, structurally similar but independently maintained, and this port keeps
 * that separation.
 *
 * <p>CE's own class comment, preserved for provenance: "a &quot;&quot;&quot;simple&quot;&quot;&quot;
 * implementation of an animation system - it's the first thing i came up with and i suppose it's
 * relatively simple but it's probably not since i suck at everything - i could have just used
 * collada XML animations but where's the fun in that?"
 *
 * <p>CE's own comment on the "bus" naming, preserved: "&quot;buses&quot; with one S since it's not
 * a vehicle" - multiple buses exist simultaneously and start at 0. A bus has one authority, i.e.
 * the translation of a single part of a model or the rotation of the entire thing. Imagine the
 * buses being film strips that hang from the ceiling, with the tape player rolling down, picking up
 * images from all tapes and combining them into a movie.
 *
 * <p><b>Not ported:</b> CE's {@code HbmAnimations} (the per-hotbar-slot animation-timer array,
 * {@code getRelevantAnim}/{@code getRelevantTransformation}, and the sound-cue scheduling
 * {@link #playPendingSounds} is a stub for) is a separate, larger render-loop-integration class
 * outside this task's scope (porting only the missing {@code BusAnimation} data class that
 * {@code IAnimatedItem} names) - not committed anywhere in this tree. A future consumer that wants
 * to actually play back a {@link BusAnimation} against the render loop will need to port that piece
 * too; this class alone is a complete, correct, self-contained data/sampling model matching CE.
 */
public class BusAnimation {

    // "buses" with one S since it's not a vehicle
    private final HashMap<String, BusAnimationSequence> animationBuses = new HashMap<>();
    // multiple buses exist simultaneously and start with 0.
    // a bus has one authority, i.e. the translation of a single part of a model or the rotation of the entire thing.
    // imagine the buses being film strips that hang from the ceiling, with the tape player
    // rolling down, picking up images from all tapes and combining them into a movie.

    // 0 by default, will always equal the duration of the longest BusAnimationSequence
    private int totalTime = 0;

    /**
     * Adds a bus to the animation. If an object has several moving parts, each transformation type
     * of each separate bus should have its own bus - unless you use one bus for several things
     * because the animation is identical, that's ok too (CE's own comment, preserved).
     *
     * @param name of the bus being added
     * @param bus the bus in question
     */
    public BusAnimation addBus(String name, BusAnimationSequence bus) {
        animationBuses.put(name, bus);

        int duration = bus.getTotalTime();

        if (duration > totalTime)
            totalTime = duration;
        return this;
    }

    /**
     * In case there is keyframes being added to sequences in post, this method allows the
     * totalTime to be updated (CE's own comment, preserved).
     */
    public void updateTime() {
        for (Entry<String, BusAnimationSequence> sequence : animationBuses.entrySet()) {
            int time = sequence.getValue().getTotalTime();

            if (time > totalTime)
                totalTime = time;
        }
    }

    /**
     * Gets a bus from the specified name. Usually not something you want to do (CE's own comment,
     * preserved).
     */
    public BusAnimationSequence getBus(String name) {
        return animationBuses.get(name);
    }

    /**
     * Gets the state of a bus at a specified time.
     *
     * @param name the name of the bus in question
     * @param millis the elapsed time since the animation started in milliseconds
     */
    public double[] getTimedTransformation(String name, int millis) {
        if (this.animationBuses.containsKey(name))
            return animationBuses.get(name).getTransformation(millis);

        return null;
    }

    /**
     * Reads all buses and checks if inbetween the last invocation and this one, a sound was
     * scheduled (CE's own comment, preserved). CE ships this as an empty {@code //TODO: pending}
     * stub - preserved as such here, not a gap introduced by this port.
     *
     * @param lastMillis the last time the bus was checked
     * @param millis the current time
     */
    public void playPendingSounds(int lastMillis, int millis) {
        // TODO: pending (CE's own stub, preserved)
    }

    public int getDuration() {
        return totalTime;
    }
}
