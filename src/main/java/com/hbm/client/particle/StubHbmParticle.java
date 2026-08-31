package com.hbm.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
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
 * {@link net.minecraft.client.particle.TextureAtlasSprite} to draw). {@link Particle#render} is
 * {@code abstract} in real 1.21.1 (there is no no-op default to inherit, unlike this class's original
 * assumption), so this class supplies its own deliberately empty override below - nothing is drawn
 * regardless of {@link #getRenderType()}'s bucket; {@link #getRenderType()} still must return a real, valid
 * constant (it is also {@code abstract} on {@link Particle}), so {@link ParticleRenderType#PARTICLE_SHEET_TRANSLUCENT}
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
     * {@link Particle#render} is {@code abstract} in real 1.21.1 (unlike the assumption this class's
     * own class javadoc made about a no-op vanilla default - there is none), so an override is
     * mandatory. Deliberately empty: this stub has no texture/sprite to draw (see class javadoc) -
     * drawing nothing here is the intended placeholder behavior, not an unimplemented TODO.
     */
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
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
