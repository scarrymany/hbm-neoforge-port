package com.hbm.client.particle;

import com.hbm.main.MainRegistry;
import com.hbm.particle.HbmParticleOptions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Real render for CE's {@code com.hbm.particle.ParticlePlasmaBlast}
 * ({@code upstream/hbm-ce/.../particle/ParticlePlasmaBlast.java}, read in full) - a flat, ground-plane
 * (not camera-billboarded - CE rotates it by explicit pitch/yaw, not toward the camera), color-tinted,
 * additively-blended quad that grows in with an exponential-decay curve and fades linearly over its
 * 20-tick life, backing the {@code PlasmaBlast} {@code HbmEffectNT} constant
 * ({@code HbmEffectNT.java:449-454}). Texture: CE binds {@code textures/particle/shockwave.png} for
 * this class (same PNG {@link MukeWaveParticle} uses for an unrelated effect - a real CE texture-reuse
 * choice, not a mistake carried over from this port).
 * <p>
 * CE's render math (verbatim, {@code ParticlePlasmaBlast.renderParticle}):
 * {@code alpha = 1 - (age+partialTicks)/maxAge}; {@code scale = (1 - e^((age+partialTicks)*-0.125)) * particleScale};
 * a flat quad from {@code (-scale,-0.25,-scale)} to {@code (scale,-0.25,scale)} in a
 * translate(pos)+rotateY(yaw)+rotateX(pitch) local frame. Ported onto {@link PoseStack} +
 * {@link Axis#YP}/{@link Axis#XP} matching {@code upstream/neo-edition/.../particle/
 * PlasmaBlastParticle.java}'s confirmed-compiling API shape for this exact matrix chain (cross-checked
 * for API shape only, per this project's standing rule - the color/scale/alpha numbers above are
 * transcribed from CE directly, not from Neo Edition).
 */
public class PlasmaBlastParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/particle/shockwave.png");

    /** CE's own 0-1 float color channels, {@code HbmEffectNT.java:450-451}. */
    public final float colorR, colorG, colorB;
    /** CE's own billboard orientation in degrees, {@code HbmEffectNT.java:451}. */
    public final float pitch, yaw;
    /** CE's own {@code cloud.setScale(...)} call, {@code HbmEffectNT.java:452}. */
    public final float scale;

    public PlasmaBlastParticle(ClientLevel level, double x, double y, double z,
                                float r, float g, float b, float pitch, float yaw, float scale) {
        super(level, x, y, z);
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.pitch = pitch;
        this.yaw = yaw;
        this.scale = scale;
        this.lifetime = 20; // CE: particleMaxAge = 20 (ParticlePlasmaBlast constructor)
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.hasPhysics = false;
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        this.alpha = Mth.clamp(1F - ((this.age + partialTicks) / (float) this.lifetime), 0F, 1F);
        float drawScale = (1F - (float) Math.pow(Math.E, (this.age + partialTicks) * -0.125F)) * this.scale;

        PoseStack pose = new PoseStack();
        pose.translate(pX, pY, pZ);
        pose.mulPose(Axis.YP.rotationDegrees(this.yaw));
        pose.mulPose(Axis.XP.rotationDegrees(this.pitch));
        Matrix4f matrix = pose.last().pose();

        // CE disables face culling for this quad (GlStateManager.disableCull()) since it's a flat
        // ground-plane quad, not a camera billboard - a VertexConsumer here has no per-draw cull
        // toggle without risking un-paired global RenderSystem state (see HbmParticleRenderTypes'
        // class javadoc), so both winding orders are emitted instead to stay visible from any angle.
        addVertex(consumer, matrix, -drawScale, -0.25F, -drawScale, 1, 1);
        addVertex(consumer, matrix, -drawScale, -0.25F, drawScale, 1, 0);
        addVertex(consumer, matrix, drawScale, -0.25F, drawScale, 0, 0);
        addVertex(consumer, matrix, drawScale, -0.25F, -drawScale, 0, 1);

        addVertex(consumer, matrix, drawScale, -0.25F, -drawScale, 0, 1);
        addVertex(consumer, matrix, drawScale, -0.25F, drawScale, 0, 0);
        addVertex(consumer, matrix, -drawScale, -0.25F, drawScale, 1, 0);
        addVertex(consumer, matrix, -drawScale, -0.25F, -drawScale, 1, 1);
    }

    private void addVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, float u, float v) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(this.colorR, this.colorG, this.colorB, this.alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT) // CE: OpenGlHelper.setLightmapTextureCoords(lightmapTexUnit, 240, 240)
                .setNormal(0F, 1F, 0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return HbmParticleRenderTypes.additive(TEXTURE);
    }

    public static final class Provider implements ParticleProvider<HbmParticleOptions> {
        @Override
        public Particle createParticle(HbmParticleOptions options, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            var tag = options.tag;
            return new PlasmaBlastParticle(level, x, y, z,
                    tag.getFloat("r"), tag.getFloat("g"), tag.getFloat("b"),
                    tag.getFloat("pitch"), tag.getFloat("yaw"), tag.getFloat("scale"));
        }
    }
}
