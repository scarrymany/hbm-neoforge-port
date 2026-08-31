package com.hbm.client.particle;

import com.hbm.main.MainRegistry;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Real render for CE's {@code com.hbm.particle.ParticleMukeWave}
 * ({@code upstream/hbm-ce/.../particle/ParticleMukeWave.java}, read in full) - the flat, ground-plane,
 * additively-blended expanding shockwave ring that opens every {@code Muke}/{@code TinyTot} nuke-flash
 * effect ({@code HbmEffectNT.java:392-435}, dispatched by {@code com.hbm.particle.HbmEffect}'s
 * {@code MUKE}/{@code TINY_TOT} handlers). Texture: CE's own {@code textures/particle/shockwave.png}
 * ({@code ParticleMukeWave.java:20}) - the same PNG {@link PlasmaBlastParticle} uses for an unrelated
 * effect, a real CE texture-reuse choice.
 * <p>
 * CE numbers (verbatim): {@code maxAge = 25}, {@code waveScale = 45};
 * {@code growth = (1 - e^(-(age+partialTicks)*0.125)) * waveScale};
 * {@code alpha = 1 - (age+partialTicks)/maxAge}; a single flat quad at {@code y = pY - 0.25} spanning
 * {@code [-growth, +growth]} on both X and Z (not a camera billboard - lies flat like
 * {@link PlasmaBlastParticle}, same double-winding no-cull workaround for the same documented reason).
 */
public class MukeWaveParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/particle/shockwave.png");

    public MukeWaveParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, 0, 0, 0);
        this.lifetime = 25;
        this.rCol = this.gCol = this.bCol = 1.0F;
        this.hasPhysics = false;
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y) - 0.25F;
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        this.alpha = 1F - ((this.age + partialTicks) / (float) this.lifetime);
        float growth = (1F - (float) Math.exp((this.age + partialTicks) * -0.125F)) * 45.0F;

        float x0 = pX - growth, x1 = pX + growth;
        float z0 = pZ - growth, z1 = pZ + growth;

        addVertex(consumer, x0, pY, z0, 1, 1);
        addVertex(consumer, x0, pY, z1, 1, 0);
        addVertex(consumer, x1, pY, z1, 0, 0);
        addVertex(consumer, x1, pY, z0, 0, 1);

        addVertex(consumer, x1, pY, z0, 0, 1);
        addVertex(consumer, x1, pY, z1, 0, 0);
        addVertex(consumer, x0, pY, z1, 1, 0);
        addVertex(consumer, x0, pY, z0, 1, 1);
    }

    private void addVertex(VertexConsumer consumer, float x, float y, float z, float u, float v) {
        consumer.addVertex(x, y, z)
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0F, 1F, 0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return HbmParticleRenderTypes.additive(TEXTURE);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            return new MukeWaveParticle(level, x, y, z);
        }
    }
}
