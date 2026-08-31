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
import org.joml.Vector3f;

import java.util.Random;

/**
 * Real render for CE's {@code com.hbm.particle.ParticleRadiationFog}
 * ({@code upstream/hbm-ce/.../particle/ParticleRadiationFog.java}, read in full) - a static (never
 * moves - CE's own {@code onUpdate} never calls {@code move()}), 25-quad yellow-green haze cluster
 * that slowly pulses in and out of visibility over a fixed 400-tick life, backing the {@code RadFog}
 * {@code HbmEffectNT} constant ({@code HbmEffectNT.java:141-148}). Texture:
 * {@code textures/particle/fog.png} ({@code ParticleRadiationFog.java:23}).
 * <p>
 * Not yet wired to any spawn call site - {@code packet/toclient/RadFogPayload.java} still ships CE's
 * own documented vanilla-{@link net.minecraft.core.particles.ParticleTypes#CLOUD} stand-in; swapping
 * that payload's handler to this real type is a small, independent follow-up left to the coordinator
 * (see this task's own notes) rather than done inline here, since {@code RadFogPayload} is a shared
 * packet file another Phase 4 task's own javadoc already documents owning that swap.
 * <p>
 * CE's static per-quad layout (verbatim - a 25-entry deterministic random walk seeded {@code 50L},
 * computed once and shared by every instance, not per-particle-random): offsets accumulate
 * {@code (gaussian-1)*2.5} on X/Z and {@code (gaussian-1)*0.15} on Y per step; each quad additionally
 * gets a fixed {@code gaussian*0.5} jitter and a {@code [0,1)} size multiplier. Alpha follows a
 * precomputed {@code sin(age*PI/400)*0.125} envelope (peaks near half life, silent at both ends) -
 * ported as a lookup table exactly like CE's own {@code ALPHA_LUT}. Color is fixed
 * {@code (0.85, 0.9, 0.5)} regardless of any constructor color argument - CE's own class sets
 * {@code particleRed/Green/Blue} in its constructor but then never reads them in
 * {@code renderParticle}, which packs the color from its own separate {@code COLOR_RED/GREEN/BLUE}
 * constants instead; a real CE dead-field quirk, preserved here rather than "fixed", since this port's
 * mandate is CE parity, not a CE bugfix. {@code particleScale} default 7.5F (the no-color constructor
 * CE's real {@code RadFog} handler actually calls).
 */
public class RadiationFogParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/particle/fog.png");

    private static final int MIN_MAX_AGE = 400;
    private static final int QUAD_COUNT = 25;
    private static final double[] OFF_X = new double[QUAD_COUNT];
    private static final double[] OFF_Y = new double[QUAD_COUNT];
    private static final double[] OFF_Z = new double[QUAD_COUNT];
    private static final double[] JIT_X = new double[QUAD_COUNT];
    private static final double[] JIT_Y = new double[QUAD_COUNT];
    private static final double[] JIT_Z = new double[QUAD_COUNT];
    private static final double[] SIZE_MUL = new double[QUAD_COUNT];
    private static final float[] ALPHA_LUT = new float[MIN_MAX_AGE + 1];
    private static final float COLOR_RED = 0.85F;
    private static final float COLOR_GREEN = 0.9F;
    private static final float COLOR_BLUE = 0.5F;

    static {
        Random random = new Random(50L);
        double offX = 0D, offY = 0D, offZ = 0D;
        for (int i = 0; i < QUAD_COUNT; i++) {
            offX += (random.nextGaussian() - 1D) * 2.5D;
            offY += (random.nextGaussian() - 1D) * 0.15D;
            offZ += (random.nextGaussian() - 1D) * 2.5D;
            OFF_X[i] = offX;
            OFF_Y[i] = offY;
            OFF_Z[i] = offZ;
            SIZE_MUL[i] = random.nextDouble();
            JIT_X[i] = random.nextGaussian() * 0.5D;
            JIT_Y[i] = random.nextGaussian() * 0.5D;
            JIT_Z[i] = random.nextGaussian() * 0.5D;
        }
        for (int age = 0; age <= MIN_MAX_AGE; age++) {
            ALPHA_LUT[age] = (float) (Math.sin(age * Math.PI / (double) MIN_MAX_AGE) * 0.125D);
        }
    }

    private final float scale;

    public RadiationFogParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = MIN_MAX_AGE; // CE forces particleMaxAge up to MIN_MAX_AGE on its first tick anyway.
        this.scale = 7.5F;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (++this.age >= this.lifetime) {
            this.remove();
            return;
        }

        // CE: motion decays but is never applied via move() - this fog cluster is stationary by design.
        this.xd *= 0.9599999785423279D;
        this.yd *= 0.9599999785423279D;
        this.zd *= 0.9599999785423279D;
        if (this.onGround) {
            this.xd *= 0.699999988079071D;
            this.zd *= 0.699999988079071D;
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        double baseX = Mth.lerp(partialTicks, this.xo, this.x) - camPos.x;
        double baseY = Mth.lerp(partialTicks, this.yo, this.y) - camPos.y;
        double baseZ = Mth.lerp(partialTicks, this.zo, this.z) - camPos.z;

        int clampedAge = Mth.clamp(this.age, 0, MIN_MAX_AGE);
        float alpha = ALPHA_LUT[clampedAge];

        Vector3f left = new Vector3f(camera.getLeftVector());
        Vector3f up = new Vector3f(camera.getUpVector());

        for (int i = 0; i < QUAD_COUNT; i++) {
            float size = (float) (SIZE_MUL[i] * this.scale);
            float pX = (float) (baseX + OFF_X[i] + JIT_X[i]);
            float pY = (float) (baseY + OFF_Y[i] + JIT_Y[i]);
            float pZ = (float) (baseZ + OFF_Z[i] + JIT_Z[i]);

            Vector3f l = new Vector3f(left).mul(size);
            Vector3f u = new Vector3f(up).mul(size);

            addVertex(consumer, pX - l.x - u.x, pY - l.y - u.y, pZ - l.z - u.z, 1, 1, alpha);
            addVertex(consumer, pX - l.x + u.x, pY - l.y + u.y, pZ - l.z + u.z, 1, 0, alpha);
            addVertex(consumer, pX + l.x + u.x, pY + l.y + u.y, pZ + l.z + u.z, 0, 0, alpha);
            addVertex(consumer, pX + l.x - u.x, pY + l.y - u.y, pZ + l.z - u.z, 0, 1, alpha);
        }
    }

    private void addVertex(VertexConsumer consumer, float x, float y, float z, float u, float v, float alpha) {
        consumer.addVertex(x, y, z)
                .setColor(COLOR_RED, COLOR_GREEN, COLOR_BLUE, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0F, 1F, 0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return HbmParticleRenderTypes.translucent(TEXTURE);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            return new RadiationFogParticle(level, x, y, z);
        }
    }
}
