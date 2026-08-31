package com.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Foundation-wave placeholder {@link Particle} shared by every {@link com.hbm.particle.ModParticleTypes}
 * entry that is registered as a plain {@link SimpleParticleType} (15 of the 17 entries - see
 * {@code ModParticleTypes}'s class javadoc for the full table mapping each entry to its real CE
 * source class/line). This task ({@code f4-particle-registry-and-events}) owns the registry and
 * NeoForge event plumbing, not each particle's final render behavior - per that task's own scope note,
 * a genuinely custom per-instance visual (CE's real texture-sheet animation, color, motion trail,
 * etc; see each CE class named in {@code ModParticleTypes}'s javadoc) is left for the Content wave's
 * {@code c14} task to fill in, once real texture assets exist under {@code assets/hbm/textures/particle}
 * (confirmed absent today - {@code docs/phase5/custom_particle_types_registry.md}'s own asset-catalog
 * check found zero files there).
 * <p>
 * <b>Why this renders nothing (deliberately, not a bug)</b>: extends vanilla {@link Particle} directly
 * (not {@link net.minecraft.client.particle.TextureSheetParticle}, which needs a real
 * {@link net.minecraft.client.particle.TextureAtlasSprite} to draw). {@link Particle#render} has an
 * empty no-op default body in vanilla (only overridden by subclasses that actually draw something,
 * e.g. {@code TextureSheetParticle}) - this class does not override it, so nothing is drawn regardless
 * of {@link #getRenderType()}'s bucket; {@link #getRenderType()} still must return a real, valid
 * constant (it is {@code abstract} on {@link Particle}), so {@link ParticleRenderType#PARTICLE_SHEET_TRANSLUCENT}
 * is used here as a safe, extremely well-established vanilla constant with no missing-asset risk,
 * rather than risking an unverified {@code NO_RENDER}-style constant name this sandbox cannot
 * compile-check (ground rule: this port cannot run {@code ./gradlew} or launch a client). The particle
 * still ticks, moves, and expires using vanilla {@link Particle#tick()}'s default gravity+lifetime
 * physics (not overridden here either), so spawning one is a real, harmless, non-crashing no-visual
 * placeholder - not a TODO stub that does nothing at all.
 */
public class StubHbmParticle extends Particle {

    public StubHbmParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.lifetime = 20 + this.random.nextInt(10);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /**
     * Shared stateless {@link ParticleProvider} for every {@link SimpleParticleType}-registered entry
     * in {@link com.hbm.particle.ModParticleTypes} - one instance, reused across all 15 registrations
     * in {@code com.hbm.client.particle.ModParticleProviders}, matching the
     * {@code new SomeParticle.Provider()} nested-class convention Neo Edition's own (real, compiling)
     * particle classes use (cross-checked for API shape only, e.g. {@code SparkParticle.Provider}).
     */
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            return new StubHbmParticle(level, x, y, z, dx, dy, dz);
        }
    }
}
