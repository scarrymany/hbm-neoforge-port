package com.hbm.client.particle;

import com.hbm.main.MainRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;

/**
 * Real render for CE's {@code com.hbm.particle.ParticleMukeCloudBF}
 * ({@code upstream/hbm-ce/.../particle/ParticleMukeCloudBF.java}, 16 lines, read in full) - CE's own
 * subclass is a texture-swap-only override of {@link com.hbm.particle.helper.HbmEffectNT}'s parent
 * class ({@code ParticleMukeCloud}), so this port mirrors that exact relationship rather than
 * duplicating {@link MukeCloudParticle}'s physics/animation logic. Backs the {@code BF}
 * {@code HbmEffectNT} constant ({@code HbmEffectNT.java:1120-1123}, dispatched by
 * {@code com.hbm.particle.HbmEffect.BF} - {@code EntityQuackos}'s 150-particle despawn burst). Texture:
 * {@code textures/particle/explosion_bf.png} (CE's own hardcoded path,
 * {@code ParticleMukeCloudBF.java:9}).
 */
public class MukeCloudBfParticle extends MukeCloudParticle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/particle/explosion_bf.png");

    public MukeCloudBfParticle(ClientLevel level, double x, double y, double z, double mx, double my, double mz) {
        super(level, x, y, z, mx, my, mz);
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            return new MukeCloudBfParticle(level, x, y, z, dx, dy, dz);
        }
    }
}
