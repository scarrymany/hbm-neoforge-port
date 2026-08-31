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

/**
 * Real render for CE's {@code com.hbm.particle.ParticleDigammaSmoke}
 * ({@code upstream/hbm-ce/.../particle/ParticleDigammaSmoke.java}, read in full) - a single large,
 * red-tinted, camera-facing translucent quad, backing the {@code Smoke_RadialDigamma}
 * {@code HbmEffectNT} constant ({@code HbmEffectNT.java:205-218}). Texture: CE's
 * {@code NTMClientRegistry.particle_base} sprite, resolving to
 * {@code textures/particle/particle_base.png} ({@code NTMClientRegistry.java:342}).
 * <p>
 * CE numbers (verbatim): {@code particleScale = 5}, {@code maxAge = 100 + rand(40)},
 * {@code red = 0.5 + rand.nextFloat()*0.2}, {@code green = blue = 0}, {@code 0.99} per-axis friction,
 * {@code alpha = 1 - age/maxAge}, full brightness. No committed call site exists for
 * {@code Smoke_RadialDigamma} in this port yet (not in {@code com.hbm.particle.HbmEffect}'s current
 * constant set) - the {@code (dx,dy,dz)} passed to {@link Provider#createParticle} is threaded through
 * as CE's own per-spawn {@code .motion(x,y,z)} call, matching {@link ExSmokeParticle}'s identical
 * convention for the same reason.
 */
public class DigammaSmokeParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/particle/particle_base.png");

    /**
     * Billboard quad half-size. Vanilla {@link Particle} has no such field - only its
     * {@link net.minecraft.client.particle.TextureSheetParticle} subclass declares one - so, like this
     * port's own {@code com.hbm.particle.engine.ParticleNT#quadSize} and CE's real
     * {@code particleScale}, it's declared locally here.
     */
    private float quadSize;

    public DigammaSmokeParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
        super(level, x, y, z, dx, dy, dz);
        this.lifetime = 100 + this.random.nextInt(40);
        this.quadSize = 5.0F;
        this.rCol = 0.5F + this.random.nextFloat() * 0.2F;
        this.gCol = 0F;
        this.bCol = 0F;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.alpha = 1F - ((float) this.age / (float) this.lifetime);
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.xd *= 0.99D;
        this.yd *= 0.99D;
        this.zd *= 0.99D;
        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        Vector3f l = new Vector3f(camera.getLeftVector()).mul(this.quadSize);
        Vector3f u = new Vector3f(camera.getUpVector()).mul(this.quadSize);

        addVertex(consumer, pX - l.x - u.x, pY - l.y - u.y, pZ - l.z - u.z, 1, 1);
        addVertex(consumer, pX - l.x + u.x, pY - l.y + u.y, pZ - l.z + u.z, 1, 0);
        addVertex(consumer, pX + l.x + u.x, pY + l.y + u.y, pZ + l.z + u.z, 0, 0);
        addVertex(consumer, pX + l.x - u.x, pY + l.y - u.y, pZ + l.z - u.z, 0, 1);
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
        return HbmParticleRenderTypes.translucent(TEXTURE);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            return new DigammaSmokeParticle(level, x, y, z, dx, dy, dz);
        }
    }
}
