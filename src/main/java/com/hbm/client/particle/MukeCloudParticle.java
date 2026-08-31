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
 * Real render for CE's {@code com.hbm.particle.ParticleMukeCloud}
 * ({@code upstream/hbm-ce/.../particle/ParticleMukeCloud.java}, read in full) - a single, fixed-size
 * (no growth/fade), camera-facing billboard playing a baked-in 5x5 (25-frame) fireball animation off
 * its own texture sheet, driven purely by {@code age/maxAge} progress through the grid - one instance
 * of the ~193-particle toroidal swarm every {@code Muke}/{@code TinyTot}/{@code BF} mushroom-cloud
 * effect spawns ({@code HbmEffectNT.java:392-435,1120-1123}, dispatched by
 * {@code com.hbm.particle.HbmEffect}'s {@code MUKE}/{@code TINY_TOT}/{@code BF} handlers, and by
 * {@link MukeFlashParticle}'s own self-triggered swarm - see that class). Texture:
 * {@code textures/particle/explosion.png} (CE's own hardcoded path, {@code ParticleMukeCloud.java:20}).
 * {@link MukeCloudBfParticle} overrides only {@link #getTexture()} for {@code explosion_bf.png},
 * matching CE's real {@code ParticleMukeCloudBF extends ParticleMukeCloud}, texture-swap-only subclass.
 * <p>
 * CE physics (verbatim, constructor + {@code onUpdate}): friction/lifetime branch on the sign of the
 * initial {@code my} velocity - {@code my > 0.1}: friction 0.9, life {@code 92+rand(11)+(int)(my*20)};
 * {@code 0 < my <= 0.1}: friction 0.9, life {@code 72+rand(11)}; {@code my == 0}: friction 0.95, life
 * {@code 52+rand(11)}; {@code my < 0}: friction 0.85, life {@code 122+rand(31)}, and age starts
 * pre-advanced to 80 (so it begins ~2/3 through its own animation, matching CE exactly - a real CE
 * quirk, not a bug this port introduces). {@code particleGravity = 0} (no extra fall acceleration -
 * CE's cloud drifts purely on its own initial velocity decayed by friction). {@code onGround}: motionX/
 * Z additionally {@code *= 0.7}. Render (verbatim): {@code texIndex = clamp(age,0,maxAge) * 25 / maxAge},
 * a fixed 5-column grid; {@code particleAlpha} and {@code particleScale} are unconditionally reset to
 * {@code 1F}/{@code 3F} every frame in CE's own {@code renderParticle} (no fade, no growth - the
 * "growth" look comes entirely from the baked animation frames).
 */
public class MukeCloudParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/particle/explosion.png");
    private static final int GRID = 5;
    private static final int FRAMES = GRID * GRID;
    private static final float QUAD_SIZE = 3.0F;

    private final float friction;

    public MukeCloudParticle(ClientLevel level, double x, double y, double z, double mx, double my, double mz) {
        super(level, x, y, z, mx, my, mz);
        this.setSize(0.2F, 0.2F);
        this.rCol = this.gCol = this.bCol = 1.0F;
        this.alpha = 1.0F;
        this.hasPhysics = true;

        if (my > 0D) {
            this.friction = 0.9F;
            this.lifetime = my > 0.1D
                    ? 92 + this.random.nextInt(11) + (int) (my * 20D)
                    : 72 + this.random.nextInt(11);
        } else if (my == 0D) {
            this.friction = 0.95F;
            this.lifetime = 52 + this.random.nextInt(11);
        } else {
            this.friction = 0.85F;
            this.lifetime = 122 + this.random.nextInt(31);
            this.age = 80; // CE: particleAge = 80 for the my < 0 branch, verbatim.
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime - 2) {
            this.remove();
            return;
        }

        this.move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;

        if (this.onGround) {
            this.xd *= 0.7D;
            this.zd *= 0.7D;
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        int clampedAge = Mth.clamp(this.age, 0, this.lifetime);
        int texIndex = this.lifetime <= 0 ? 0 : Mth.clamp(clampedAge * FRAMES / this.lifetime, 0, FRAMES - 1);
        float cell = 1F / GRID;
        float uMin = (texIndex % GRID) * cell;
        float vMin = (texIndex / GRID) * cell;

        Vector3f l = new Vector3f(camera.getLeftVector()).mul(QUAD_SIZE);
        Vector3f u = new Vector3f(camera.getUpVector()).mul(QUAD_SIZE);

        addVertex(consumer, pX - l.x - u.x, pY - l.y - u.y, pZ - l.z - u.z, uMin + cell, vMin + cell);
        addVertex(consumer, pX - l.x + u.x, pY - l.y + u.y, pZ - l.z + u.z, uMin + cell, vMin);
        addVertex(consumer, pX + l.x + u.x, pY + l.y + u.y, pZ + l.z + u.z, uMin, vMin);
        addVertex(consumer, pX + l.x - u.x, pY + l.y - u.y, pZ + l.z - u.z, uMin, vMin + cell);
    }

    private void addVertex(VertexConsumer consumer, float x, float y, float z, float u, float v) {
        consumer.addVertex(x, y, z)
                .setColor(1F, 1F, 1F, 1F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0F, 1F, 0F);
    }

    protected ResourceLocation getTexture() {
        return TEXTURE;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return HbmParticleRenderTypes.translucent(getTexture());
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            return new MukeCloudParticle(level, x, y, z, dx, dy, dz);
        }
    }
}
