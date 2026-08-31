package com.hbm.client.particle;

import com.hbm.main.MainRegistry;
import com.hbm.particle.ModParticleTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Real render for CE's {@code com.hbm.particle.bullet_hit.ParticleHitDebris}
 * ({@code upstream/hbm-ce/.../particle/bullet_hit/ParticleHitDebris.java}, read in full) - a
 * material-tinted, gravity-affected, tumbling debris chip, backing {@code BulletImpact}'s block-hit
 * branch ({@code HbmEffectNT.java:1152-1224}). CE's real class takes a per-spawn
 * {@link ResourceLocation} constructor argument (a different sheet per hit material - rock, wood,
 * leaves) - {@link ModParticleTypes#HIT_DEBRIS} is registered as a plain
 * {@link SimpleParticleType} (no data channel) so this class cannot vary that per spawn yet; it uses
 * CE's own generic-material default ({@code textures/misc/rock_fragments.png}, CE's
 * {@code ResourceManager.rock_fragments} - the {@code else}/rock-and-ground fallback branch,
 * {@code HbmEffectNT.java:1174-1198}) rather than inventing a texture. A future pass wiring a real gun-
 * impact call site should either add a typed {@code ParticleType<HbmParticleOptions>} entry (matching
 * {@link ModParticleTypes#PLASMA_BLAST}'s shape) carrying the material texture, or dispatch a small
 * fixed set of material-specific {@code SimpleParticleType} variants instead - noted here rather than
 * decided, since no real call site exists yet to design against.
 * <p>
 * CE numbers (verbatim, generic/rock branch): {@code scale = (0.5+rand)*1F}, {@code texIdx = rand(16)}
 * (this port's 4x4 grid), {@code lifetime = 40+rand(20)}, {@code gravity = 1F},
 * {@code rotationOverLifetime = rand*3-1.5F} decaying {@code 0.95}/tick ({@code 0.7} once grounded),
 * fade over the last 10 ticks ({@code alpha = 1 - clamp(age-(maxAge-10),0,10)*0.1}). CE never overrides
 * {@code getBrightnessForRender} for this class - ambient world light, not full bright, matching
 * {@link CoolingTowerParticle}'s same documented choice.
 */
public class HitDebrisParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/misc/rock_fragments.png");
    private static final int GRID = 4;
    private static final float CELL = 1F / GRID;

    private final int texIdx;
    private float rotation;
    private float prevRotation;
    private float rotationSpeed;

    public HitDebrisParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
        super(level, x, y, z, dx, dy, dz);
        this.texIdx = this.random.nextInt(16);
        this.quadSize = 0.5F + this.random.nextFloat();
        this.lifetime = 40 + this.random.nextInt(20);
        this.rotation = this.random.nextFloat() * Mth.TWO_PI;
        this.prevRotation = this.rotation;
        this.rotationSpeed = this.random.nextFloat() * 3F - 1.5F;
        this.gravity = 1.0F;
        this.rCol = this.gCol = this.bCol = 1.0F;
        this.hasPhysics = true;
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

        this.yd -= 0.04D * this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.prevRotation = this.rotation;
        this.rotation += this.rotationSpeed;
        this.rotationSpeed *= this.onGround ? 0.7F : 0.95F;
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        this.alpha = 1F - Mth.clamp((this.age + partialTicks) - (this.lifetime - 10), 0, 10) * 0.1F;
        float angle = Mth.lerp(partialTicks, this.prevRotation, this.rotation);

        // See BloodParticle's identical rotation approach (left x up view axis, Quaternionf.rotateAxis/
        // .transform) for why this avoids an unconfirmed Camera#getLookVector/Vector3f#rotateAxis call.
        Vector3f left0 = new Vector3f(camera.getLeftVector());
        Vector3f up0 = new Vector3f(camera.getUpVector());
        Vector3f lookAxis = new Vector3f(left0).cross(up0).normalize();
        Quaternionf roll = new Quaternionf().rotateAxis(angle, lookAxis.x(), lookAxis.y(), lookAxis.z());
        Vector3f l = roll.transform(new Vector3f(left0)).mul(this.quadSize * 0.1F);
        Vector3f u = roll.transform(new Vector3f(up0)).mul(this.quadSize * 0.1F);

        float u0 = (this.texIdx % GRID) * CELL;
        float v0 = (this.texIdx / GRID) * CELL;
        BlockPos lightPos = BlockPos.containing(this.x, this.y, this.z);
        int light = this.level.hasChunkAt(lightPos) ? LevelRenderer.getLightColor(this.level, lightPos) : 0;

        addVertex(consumer, pX - l.x - u.x, pY - l.y - u.y, pZ - l.z - u.z, u0 + CELL, v0 + CELL, light);
        addVertex(consumer, pX - l.x + u.x, pY - l.y + u.y, pZ - l.z + u.z, u0 + CELL, v0, light);
        addVertex(consumer, pX + l.x + u.x, pY + l.y + u.y, pZ + l.z + u.z, u0, v0, light);
        addVertex(consumer, pX + l.x - u.x, pY + l.y - u.y, pZ + l.z - u.z, u0, v0 + CELL, light);
    }

    private void addVertex(VertexConsumer consumer, float x, float y, float z, float u, float v, int light) {
        consumer.addVertex(x, y, z)
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
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
            return new HitDebrisParticle(level, x, y, z, dx, dy, dz);
        }
    }
}
