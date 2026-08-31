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
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Real render for CE's {@code com.hbm.particle.ParticleExSmoke}
 * ({@code upstream/hbm-ce/.../particle/ParticleExSmoke.java}, read in full) - CE's most-reused generic
 * explosion-smoke plume, backing {@code Smoke_Cloud}/{@code Smoke_Radial}/{@code Smoke_Shock}/
 * {@code Smoke_ShockRand}/{@code Smoke_Wave} ({@code HbmEffectNT.java:165-291}, already dispatched by
 * {@code com.hbm.particle.HbmEffect}'s {@code SMOKE_RADIAL}/{@code SMOKE_CLOUD}/{@code SMOKE_SHOCK}
 * handlers - see that class). Texture: CE's {@code NTMClientRegistry.contrail} sprite, which resolves
 * to {@code textures/particle/contrail.png} (confirmed by its sprite-registration call,
 * {@code NTMClientRegistry.java:341}); this port uses the whole PNG as one quad (CE's own sprite is a
 * single non-atlased region too - {@code particleTexture.getMinU()}..{@code getMaxU()} span the entire
 * bound texture for this sprite).
 * <p>
 * CE draws 6 independent, randomly offset/tinted/scaled sub-quads per particle per frame, reseeding a
 * {@code java.util.Random(randomSeed)} (captured once at spawn) identically every frame so the swarm
 * layout stays stable while only the underlying particle drifts - reproduced verbatim below using
 * {@link RandomSource#create(long)} instead of {@code java.util.Random} (draw order/formulas kept
 * bit-for-bit identical: {@code color = next * 0.5 + 0.4} grey, {@code scale = next + 0.5},
 * {@code offset = (nextGaussian - 1) * 0.75} per axis). Motion/age: {@code maxAge = 100 + rand(40)},
 * {@code alpha = 1 - age/maxAge}, {@code 0.76} friction per axis, full brightness
 * ({@code getBrightnessForRender} override, {@code ParticleExSmoke.java:87-90}).
 */
public class ExSmokeParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/particle/contrail.png");
    private static final int SUB_QUADS = 6;

    private final long randomSeed;

    public ExSmokeParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
        super(level, x, y, z, dx, dy, dz);
        this.lifetime = 100 + this.random.nextInt(40);
        this.randomSeed = level.random.nextInt();
        this.hasPhysics = false; // CE: canCollide never set true (defaults false) - straight-line drift
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

        this.xd *= 0.7599999785423279D;
        this.yd *= 0.7599999785423279D;
        this.zd *= 0.7599999785423279D;
        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        RandomSource seeded = RandomSource.create(this.randomSeed);
        Vector3f left = new Vector3f(camera.getLeftVector());
        Vector3f up = new Vector3f(camera.getUpVector());

        for (int i = 0; i < SUB_QUADS; i++) {
            float grey = seeded.nextFloat() * 0.5F + 0.4F;
            float scale = seeded.nextFloat() + 0.5F;
            float oX = (float) ((seeded.nextGaussian() - 1D) * 0.75F);
            float oY = (float) ((seeded.nextGaussian() - 1D) * 0.75F);
            float oZ = (float) ((seeded.nextGaussian() - 1D) * 0.75F);

            float cx = pX + oX, cy = pY + oY, cz = pZ + oZ;
            Vector3f l = new Vector3f(left).mul(scale);
            Vector3f u = new Vector3f(up).mul(scale);

            addVertex(consumer, cx - l.x - u.x, cy - l.y - u.y, cz - l.z - u.z, 1, 1, grey);
            addVertex(consumer, cx - l.x + u.x, cy - l.y + u.y, cz - l.z + u.z, 1, 0, grey);
            addVertex(consumer, cx + l.x + u.x, cy + l.y + u.y, cz + l.z + u.z, 0, 0, grey);
            addVertex(consumer, cx + l.x - u.x, cy + l.y - u.y, cz + l.z - u.z, 0, 1, grey);
        }
    }

    private void addVertex(VertexConsumer consumer, float x, float y, float z, float u, float v, float grey) {
        consumer.addVertex(x, y, z)
                .setColor(grey, grey, grey, this.alpha)
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
            return new ExSmokeParticle(level, x, y, z, dx, dy, dz);
        }
    }
}
