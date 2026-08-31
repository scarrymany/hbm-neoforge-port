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
import org.joml.Vector3f;

/**
 * Real render for CE's {@code com.hbm.particle.bullet_hit.ParticleSmokeAnim}
 * ({@code upstream/hbm-ce/.../particle/bullet_hit/ParticleSmokeAnim.java}, read in full) - an animated
 * (9x9, 81-frame), growing, log-fading smoke puff, backing {@code BulletImpact}'s smoke plume on both
 * its block-hit and entity-hit branches ({@code HbmEffectNT.java:1244-1251,1287-1294}). Texture: CE's
 * {@code ResourceManager.smoke_anim0} = {@code textures/misc/smo0_blur4.png}.
 * <p>
 * {@link ModParticleTypes#SMOKE_ANIM} is a plain {@link SimpleParticleType} (no data channel), so this
 * class uses CE's own generic-material block-hit default rather than inventing one
 * ({@code HbmEffectNT.java:1244-1251}: {@code speed=0.1F}, {@code scale=5+rand*5},
 * {@code scaleOverLife=1}, {@code lifetime=15} [CE doubles this internally,
 * {@code particleMaxAge=lifetime*2=30}], tinted grey {@code color(0.5F,0.5F,0.5F)}).
 * <p>
 * CE numbers (verbatim): per-tick {@code 0.9} motion friction plus {@code motionY -= 0.04F} constant
 * fall, {@code particleScale += scaleOverLife} ({@code scaleOverLife *= 0.95} decay);
 * frame {@code index = (int)((age+partialTicks)*25*speed + offset) % 81} on a 9-column grid
 * ({@code offset} a fixed per-particle random 0-80 phase); alpha
 * {@code = (1 - log10(clamp(age/maxAge,0,1)*9+1)) * 0.3}.
 */
public class SmokeAnimParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/misc/smo0_blur4.png");
    private static final int GRID = 9;
    private static final int FRAMES = GRID * GRID;
    private static final float CELL = 1F / GRID;

    /**
     * Billboard quad half-size. Vanilla {@link Particle} has no such field - only its
     * {@link net.minecraft.client.particle.TextureSheetParticle} subclass declares one - so, like this
     * port's own {@code com.hbm.particle.engine.ParticleNT#quadSize} and CE's real
     * {@code particleScale}, it's declared locally here.
     */
    private float quadSize;
    private final float speed;
    private final int offset;
    private float scaleOverLife;
    private float prevScale;

    public SmokeAnimParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
        super(level, x, y, z, dx, dy, dz);
        this.speed = 0.1F;
        this.quadSize = 5F + this.random.nextFloat() * 5F;
        this.prevScale = this.quadSize;
        this.scaleOverLife = 1.0F;
        this.lifetime = 15 * 2; // CE: particleMaxAge = lifetime * 2
        this.offset = this.random.nextInt(81);
        this.rCol = this.gCol = this.bCol = 0.5F; // CE generic block-hit tint: color(r*0.5, g*0.5, b*0.5)
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.xd *= 0.9D;
        this.yd *= 0.9D;
        this.zd *= 0.9D;
        this.yd -= 0.04D;
        this.move(this.xd, this.yd, this.zd);

        this.prevScale = this.quadSize;
        this.quadSize += this.scaleOverLife;
        this.scaleOverLife *= 0.95F;
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        float progress = Mth.clamp((this.age + partialTicks) / (float) this.lifetime, 0F, 1F);
        this.alpha = (1F - (float) Math.log10(progress * 9F + 1F)) * 0.3F;

        int index = (int) ((this.age + partialTicks) * 25F * this.speed + this.offset) % FRAMES;
        float u0 = (index % GRID) * CELL;
        float v0 = (index / GRID) * CELL;

        float scale = Mth.lerp(partialTicks, this.prevScale, this.quadSize) * 0.1F;
        Vector3f l = new Vector3f(camera.getLeftVector()).mul(scale);
        Vector3f u = new Vector3f(camera.getUpVector()).mul(scale);

        addVertex(consumer, pX - l.x - u.x, pY - l.y - u.y, pZ - l.z - u.z, u0 + CELL, v0 + CELL);
        addVertex(consumer, pX - l.x + u.x, pY - l.y + u.y, pZ - l.z + u.z, u0 + CELL, v0);
        addVertex(consumer, pX + l.x + u.x, pY + l.y + u.y, pZ + l.z + u.z, u0, v0);
        addVertex(consumer, pX + l.x - u.x, pY + l.y - u.y, pZ + l.z - u.z, u0, v0 + CELL);
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
            return new SmokeAnimParticle(level, x, y, z, dx, dy, dz);
        }
    }
}
