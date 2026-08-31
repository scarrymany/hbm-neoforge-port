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

/**
 * Real render for CE's {@code com.hbm.particle.bullet_hit.ParticleBulletImpact}
 * ({@code upstream/hbm-ce/.../particle/bullet_hit/ParticleBulletImpact.java}, read in full) - the
 * bullet-hole decal quad backing {@code BulletImpact}'s block-hit branch
 * ({@code HbmEffectNT.java:1220-1225}). CE's real class orients the decal flush against the struck
 * surface via a captured normal vector and (optionally, {@code GeneralConfig.bulletHoleNormalMapping})
 * a full normal-mapped VBO mesh - neither the surface-fitted mesh generation
 * ({@code BakedModelUtil.generateDecalMesh}) nor the normal-map shader pass has any modern equivalent
 * built anywhere in this port, and are out of this task's scope (rendering substrate work, not
 * per-particle content). Texture: CE's {@code ResourceManager.bullet_impact} =
 * {@code textures/misc/impact.png}.
 * <p>
 * {@link ModParticleTypes#BULLET_IMPACT} is a plain {@link SimpleParticleType} (no data channel), so
 * this class cannot receive CE's real per-spawn normal/color - it renders CE's generic-material
 * default (scale 0.1F, {@code lifetime = 60+rand(20)}, white tint, {@code HbmEffectNT.java:1220}) as a
 * flat quad facing straight up ({@code (0,1,0)} normal), fading over its last 10 ticks exactly like
 * CE's own {@code renderParticle}. CE never overrides brightness for this class either - ambient light,
 * matching {@link CoolingTowerParticle}/{@link HitDebrisParticle}.
 */
public class BulletImpactParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/misc/impact.png");

    private final float roll;

    public BulletImpactParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.quadSize = 0.1F;
        this.lifetime = 60 + this.random.nextInt(20);
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.rCol = this.gCol = this.bCol = 1.0F;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float pX = (float) (this.x - camPos.x);
        float pY = (float) (this.y - camPos.y);
        float pZ = (float) (this.z - camPos.z);

        this.alpha = 1F - Mth.clamp((this.age + partialTicks) - (this.lifetime - 10), 0, 10) * 0.1F;
        // CE's fallback (non-VBO) path builds a screen/world-fixed quad rotated by a random roll about
        // the surface normal - approximated here as a flat, upward-facing decal (see class javadoc for
        // why the real per-spawn normal isn't available yet), rolled the same random amount CE applies.
        float scale = this.quadSize * 2F; // CE: scale *= 2 in the non-VBO render branch.
        float cos = Mth.cos(this.roll);
        float sin = Mth.sin(this.roll);

        addVertex(consumer, pX + (-cos + sin) * scale, pY, pZ + (-sin - cos) * scale, 0, 0);
        addVertex(consumer, pX + (cos + sin) * scale, pY, pZ + (sin - cos) * scale, 1, 0);
        addVertex(consumer, pX + (cos - sin) * scale, pY, pZ + (sin + cos) * scale, 1, 1);
        addVertex(consumer, pX + (-cos - sin) * scale, pY, pZ + (-sin + cos) * scale, 0, 1);
    }

    private void addVertex(VertexConsumer consumer, float x, float y, float z, float u, float v) {
        BlockPos lightPos = BlockPos.containing(this.x, this.y, this.z);
        int light = this.level.hasChunkAt(lightPos) ? LevelRenderer.getLightColor(this.level, lightPos) : 0;
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
            return new BulletImpactParticle(level, x, y, z);
        }
    }
}
