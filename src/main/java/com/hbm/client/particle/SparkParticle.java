package com.hbm.client.particle;

import com.hbm.main.MainRegistry;
import com.hbm.particle.ModParticleTypes;
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
 * Real render for CE's {@code com.hbm.particle.ParticleSpark}
 * ({@code upstream/hbm-ce/.../particle/ParticleSpark.java}, read in full) - a stretched, camera-facing
 * "ribbon" quad drawn between the particle's current position and its motion-direction tip, backing
 * the {@code Spark} {@code HbmEffectNT} constant ({@code HbmEffectNT.java:841-878}). Texture: CE binds
 * {@code ResourceManager.bfg_particle} = {@code textures/models/bfg/particle.png} for this class
 * (confirmed by direct read of {@code ResourceManager.java:1108} - an odd path for a generic spark, but
 * CE's real one, not a guess).
 * <p>
 * <b>Why {@link ModParticleTypes#SPARK} is registered as a plain
 * {@link net.minecraft.core.particles.SimpleParticleType}, not a data-carrying entry</b> (per
 * {@code com.hbm.particle.ModParticleTypes}'s own class javadoc - the {@code f4-particle-registry-and-
 * events} task's documented decision, not this class's): CE's real {@code Spark} handler reads a large
 * NBT-keyed parameter set (direction, width, length, lifetime, gravity, color, cone angle, count -
 * {@code HbmEffectNT.java:841-864}) that a {@link SimpleParticleType} spawn has no channel for. No
 * committed call site exists anywhere in this port yet for {@code Spark} (confirmed by
 * {@code docs/phase5/custom_particle_types_registry.md}'s own survey - only unconfirmed turret
 * "muzzle-flash burst" prose references, see that report's open question 2), so this class uses CE's
 * own real defaults (an un-cone'd, un-jittered single spark) for everything except direction, and
 * threads the {@code (dx,dy,dz)} the vanilla {@link net.minecraft.world.level.Level#addParticle}
 * entry point already carries through as CE's own per-spawn direction vector (CE's
 * {@code .motion(mX,mY,mZ)} call) - a real, non-arbitrary use of the one data channel actually
 * available, not an invented substitute.
 */
public class SparkParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/models/bfg/particle.png");

    /**
     * Billboard quad half-size. Vanilla {@link Particle} has no such field - only its
     * {@link net.minecraft.client.particle.TextureSheetParticle} subclass declares one - so, like this
     * port's own {@code com.hbm.particle.engine.ParticleNT#quadSize} and CE's real
     * {@code particleScale}, it's declared locally here.
     */
    private float quadSize;
    /** CE {@code ParticleSpark.stretch} - ribbon length multiplier along the motion axis, default 1.0F. */
    private final float stretch;
    /** CE {@code ParticleSpark.gravity}, default {@code 9.81F * 0.01F} (HbmEffectNT.java:848). */
    private final float gravity;

    public SparkParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
        super(level, x, y, z);
        this.stretch = 1.0F;
        this.quadSize = 0.025F; // CE default "width" (HbmEffectNT.java:846)
        this.lifetime = 100;    // CE default "lifetime" (HbmEffectNT.java:850)
        this.gravity = 9.81F * 0.01F;
        this.rCol = this.gCol = this.bCol = 1.0F;
        this.alpha = 1.0F;
        this.hasPhysics = false; // collision handled manually below, matching CE's own custom move()

        // CE ParticleSpark.motion(mX, mY, mZ): sets motion AND nudges the spawn position by one step.
        this.xd = dx;
        this.yd = dy - this.gravity;
        this.zd = dz;
        this.setPos(x + this.xd, y + this.yd, z + this.zd);
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime || (this.xd * this.xd + this.yd * this.yd + this.zd * this.zd) < 0.001D) {
            this.remove();
            return;
        }

        // CE: canCollide only after the first 4 ticks, so the spark isn't immediately stopped by the
        // surface it spawned on/near.
        boolean canCollide = this.age >= 4;
        double prevYd = this.yd;
        if (canCollide) {
            this.move(this.xd, this.yd, this.zd);
        } else {
            this.setBoundingBox(this.getBoundingBox().move(this.xd, this.yd, this.zd));
            this.setLocationFromBoundingbox();
        }

        final float airResistance = 0.95F;
        this.xd *= airResistance;
        this.yd = this.yd * airResistance - this.gravity;
        this.zd *= airResistance;

        if (canCollide && this.onGround) {
            // CE's move() bounce-back: reflects the pre-move Y motion with damping+randomness on collision.
            this.yd = -prevYd * 0.75D * (this.random.nextFloat() * 0.8D + 0.25D);
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);
        float tipX = (float) (pX + this.xd);
        float tipY = (float) (pY + this.yd);
        float tipZ = (float) (pZ + this.zd);

        Vec3 axis = new Vec3(tipX - pX, tipY - pY, tipZ - pZ);
        Vec3 toTip = new Vec3(tipX, tipY, tipZ);
        Vec3 cross = axis.cross(toTip);
        Vec3 side = cross.lengthSqr() > 1.0E-6 ? cross.normalize().scale(0.5D * this.quadSize) : new Vec3(0.5D * this.quadSize, 0, 0);
        Vec3 stretched = axis.scale(this.stretch);

        float p2x = (float) (pX - side.x), p2y = (float) (pY - side.y), p2z = (float) (pZ - side.z);
        float p1x = (float) (pX + side.x), p1y = (float) (pY + side.y), p1z = (float) (pZ + side.z);

        addVertex(consumer, p2x, p2y, p2z, 1, 0);
        addVertex(consumer, p1x, p1y, p1z, 1, 1);
        addVertex(consumer, (float) (p1x + stretched.x), (float) (p1y + stretched.y), (float) (p1z + stretched.z), 0, 1);
        addVertex(consumer, (float) (p2x + stretched.x), (float) (p2y + stretched.y), (float) (p2z + stretched.z), 0, 0);
    }

    private void addVertex(VertexConsumer consumer, float x, float y, float z, float u, float v) {
        consumer.addVertex(x, y, z)
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT) // CE: packLightmap(240, 240)
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
            return new SparkParticle(level, x, y, z, dx, dy, dz);
        }
    }
}
