package com.hbm.entity.mob;

import com.hbm.main.MainRegistry;
import net.minecraft.world.entity.monster.Creeper;

import java.lang.reflect.Field;

/**
 * Small shared support helper for CE's 5 {@code EntityCreeper*} variants (see
 * {@code docs/phase4/entities_creeper_variants.md}).
 * <p>
 * <b>{@code Creeper#explodeCreeper()} is {@code private}, not an override point.</b> Confirmed against
 * real NeoForge/NeoForm 1.21.1 mapped source (a targeted decompile check during this port's review
 * pass, resolving the research report's own explicitly-flagged "verify against a real jar" open
 * question): {@code Creeper.java} declares {@code private void explodeCreeper()}, called only from
 * {@code Creeper#tick()}'s own swell-countdown check. A subclass method with that same name/signature
 * does <b>not</b> override it (private methods are not inherited for override purposes) - tagging one
 * {@code @Override} is a hard compile error, and even without the annotation such a method would simply
 * never be invoked by vanilla's own countdown. Each of the 5 leaf classes therefore does <b>not</b>
 * override {@code explodeCreeper()} at all any more; instead they override the real, {@code public},
 * non-final {@code Creeper#tick()} and use {@link #isAboutToExplode} (below) to detect, one tick early
 * and without ever calling {@code super.tick()} on the exploding tick, exactly the tick vanilla's own
 * private method would have fired on - substituting each leaf's own CE-faithful explosion for vanilla's
 * generic {@code Level.ExplosionInteraction.MOB} blast, then discarding the entity themselves so
 * vanilla's private method never runs at all.
 * <p>
 * CE's {@code EntityCreeper#fuseTime} (a 1.12.2 field vanilla itself exposed) sets each variant's fuse
 * length (used verbatim by {@link EntityCreeperPhosgene} at {@code 20} and {@link EntityCreeperNuclear}
 * at {@code 75}). Modern Mojang mappings renamed this same field to {@code Creeper#maxSwell} - confirmed
 * {@code private int maxSwell = 30;} by the same decompile check above (the field-name guess this
 * class's prior revision had already made turned out correct). This port has no access-transformer
 * infrastructure set up anywhere yet (no {@code [[accessTransformers]]} entry in
 * {@code neoforge.mods.toml}, no {@code accesstransformer.cfg} resource) to widen either private member
 * properly, so reflection is used for both {@code maxSwell} and its sibling countdown field
 * {@code swell}: it fails safe (logging one warning and disabling the affected feature - vanilla's
 * 30-tick fuse for {@link #setFuseTime}, deferring to vanilla's own generic explosion for
 * {@link #isAboutToExplode}) rather than crashing if either field name assumption is ever invalidated by
 * a future Minecraft version.
 */
final class CreeperVariantSupport {

    private static final Field MAX_SWELL_FIELD = resolveCreeperField("maxSwell");
    private static final Field SWELL_FIELD = resolveCreeperField("swell");
    private static boolean warnedOnSet = false;
    private static boolean warnedOnRead = false;

    private CreeperVariantSupport() {
    }

    private static Field resolveCreeperField(String name) {
        try {
            Field field = Creeper.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | SecurityException e) {
            MainRegistry.logger.warn("CreeperVariantSupport: could not reflectively resolve Creeper#" + name
                    + " (name unverified against the real jar this build actually compiles against) - "
                    + "features depending on it will fail safe instead of crashing.", e);
            return null;
        }
    }

    /**
     * Sets {@code creeper}'s fuse length (CE's {@code fuseTime}). No-ops (leaving vanilla's 30-tick
     * default) if the underlying field could not be resolved - see class javadoc.
     */
    static void setFuseTime(Creeper creeper, int ticks) {
        if (MAX_SWELL_FIELD == null) return;
        try {
            MAX_SWELL_FIELD.setInt(creeper, ticks);
        } catch (ReflectiveOperationException e) {
            if (!warnedOnSet) {
                warnedOnSet = true;
                MainRegistry.logger.warn("CreeperVariantSupport: failed to write Creeper#maxSwell via reflection.", e);
            }
        }
    }

    /**
     * True if vanilla's own private countdown (replicated here: {@code swell + (isIgnited() ? 1 :
     * getSwellDir())}, floored at 0, exactly mirroring {@code Creeper#tick()}'s own arithmetic) would
     * reach {@code maxSwell} <b>this</b> tick - i.e. the caller's own {@code tick()} override is about
     * to observe the one tick vanilla's private {@code explodeCreeper()} would otherwise fire on.
     * Callers must check this <em>before</em> calling {@code super.tick()} and, if {@code true}, run
     * their own explosion logic and {@code return} instead of delegating to {@code super.tick()} at all
     * - see each leaf class's own {@code tick()} override. Always {@code false} (deferring entirely to
     * vanilla's own generic explosion) if either backing field failed to resolve - see class javadoc.
     */
    static boolean isAboutToExplode(Creeper creeper) {
        if (MAX_SWELL_FIELD == null || SWELL_FIELD == null) return false;
        try {
            int swell = SWELL_FIELD.getInt(creeper);
            int maxSwell = MAX_SWELL_FIELD.getInt(creeper);
            int dir = creeper.isIgnited() ? 1 : creeper.getSwellDir();
            int nextSwell = Math.max(0, swell + dir);
            return nextSwell >= maxSwell;
        } catch (ReflectiveOperationException e) {
            if (!warnedOnRead) {
                warnedOnRead = true;
                MainRegistry.logger.warn("CreeperVariantSupport: failed to read Creeper#swell/maxSwell via "
                        + "reflection - falling back to vanilla's own generic explosion for this creeper variant.", e);
            }
            return false;
        }
    }
}
