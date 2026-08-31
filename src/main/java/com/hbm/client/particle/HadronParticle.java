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
 * Real render for CE's {@code com.hbm.particle.ParticleHadron}
 * ({@code upstream/hbm-ce/.../particle/ParticleHadron.java}, read in full) - a short-lived (10-tick),
 * camera-facing, additively-blended flash quad that grows linearly and fades linearly over its whole
 * life, backing the {@code Tau} {@code HbmEffectNT} constant's beam-core flash
 * ({@code HbmEffectNT.java:1435-1441}, alongside CE's {@code ParticleHbmSpark} - folded into
 * {@link SparkParticle} instead of a separate entry, see {@code ModParticleTypes}'s class javadoc for
 * that decision). Texture: CE's own hardcoded {@code textures/particle/hadron.png}
 * ({@code ParticleHadron.java:26}).
 * <p>
 * CE's render math (verbatim): {@code alpha = clamp(1 - (age+partialTicks)/maxAge, 0, 1)};
 * {@code quadHalfSize = (age+partialTicks) * 0.15 * particleScale}. CE's own {@code makeSmall(boolean)}
 * variant (scale 0.5, maxAge 5, used by {@code Tau} when {@code data.getBoolean("small")}) is not
 * reproduced here - {@link ModParticleTypes#HADRON} is a plain
 * {@link net.minecraft.core.particles.SimpleParticleType} with no data channel for it (same reasoning
 * as {@link SparkParticle}'s own javadoc) - this class always renders CE's default (non-small) variant.
 */
public class HadronParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/particle/hadron.png");

    public HadronParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 10;
        this.quadSize = 1.0F; // CE particleScale = 1F (default, non-"small" variant)
        this.rCol = this.gCol = this.bCol = 1.0F;
        this.hasPhysics = false;
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        this.alpha = Mth.clamp(1F - ((this.age + partialTicks) / (float) this.lifetime), 0F, 1F);
        float halfSize = (this.age + partialTicks) * 0.15F * this.quadSize;

        Vector3f l = new Vector3f(camera.getLeftVector()).mul(halfSize);
        Vector3f u = new Vector3f(camera.getUpVector()).mul(halfSize);

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
        return HbmParticleRenderTypes.additive(TEXTURE);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            return new HadronParticle(level, x, y, z);
        }
    }
}
