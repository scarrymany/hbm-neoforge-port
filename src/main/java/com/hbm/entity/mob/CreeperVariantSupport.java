package com.hbm.entity.mob;

import com.hbm.main.MainRegistry;
import net.minecraft.world.entity.monster.Creeper;

import java.lang.reflect.Field;

/**
 * Small shared support helper for CE's 5 {@code EntityCreeper*} variants (see
 * {@code docs/phase4/entities_creeper_variants.md}).
 * <p>
 * CE's {@code EntityCreeper#fuseTime} (a 1.12.2 field vanilla itself exposed) sets each variant's
 * fuse length (used verbatim by {@link EntityCreeperPhosgene} at {@code 20} and
 * {@link EntityCreeperNuclear} at {@code 75}). Modern Mojang mappings renamed this same field to
 * {@code Creeper#maxSwell} and it is <b>not</b> exposed publicly - vanilla's {@code tick()} reads it
 * directly, not through {@link Creeper#getMaxSwell()} (a getter added later purely for renderer/AI
 * consumers, which would have no effect on the actual countdown if merely overridden). This port has
 * no access-transformer infrastructure set up anywhere yet (no {@code [[accessTransformers]]} entry in
 * {@code neoforge.mods.toml}, no {@code accesstransformer.cfg} resource) to widen it properly, and this
 * sandbox has no compiled 1.21.1 jar/decompile available to verify the exact field name against - see
 * the research report's own "Open questions" section. Best-effort reflection is used instead: it fails
 * safe (silently keeping vanilla's default 30-tick fuse and logging one warning) rather than crashing
 * entity construction if the field name assumption above turns out wrong in a real build.
 */
final class CreeperVariantSupport {

    private static final Field MAX_SWELL_FIELD = resolveMaxSwellField();
    private static boolean warnedOnSet = false;

    private CreeperVariantSupport() {
    }

    private static Field resolveMaxSwellField() {
        try {
            Field field = Creeper.class.getDeclaredField("maxSwell");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | SecurityException e) {
            MainRegistry.logger.warn("CreeperVariantSupport: could not reflectively resolve "
                    + "Creeper#maxSwell (name unverified against a real 1.21.1 jar in this port's build "
                    + "environment) - CE-tuned fuse-time overrides for the creeper variants will silently "
                    + "fall back to vanilla's default 30-tick fuse.", e);
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
}
