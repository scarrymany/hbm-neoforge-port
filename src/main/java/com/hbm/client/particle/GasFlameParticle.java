package com.hbm.client.particle;

import com.hbm.main.MainRegistry;
import com.hbm.particle.HbmParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import org.joml.Vector3f;

import java.awt.Color;

/**
 * Real render for CE's {@code com.hbm.particle.ParticleGasFlame}
 * ({@code upstream/hbm-ce/.../particle/ParticleGasFlame.java}, read in full) - backing the
 * {@code GasFlame} {@code HbmEffectNT} constant ({@code HbmEffectNT.java:466-473}), and wired at
 * {@code explosion/ExplosionLarge.spawnBurst} (see that class's own updated javadoc - CE's real call
 * site, {@code ParticleUtil.spawnGasFlame}, does route through the network/{@code effectNT} path this
 * port's {@link net.minecraft.server.level.ServerLevel#sendParticles} bridge now covers, not a
 * direct-only client call as this port's earlier javadoc pass assumed).
 * <p>
 * CE's real class extends vanilla 1.12's {@code ParticleSmokeNormal} (a billowing grey-smoke puff) and
 * only overrides its color every tick via an HSB "orange-to-black ember" gradient
 * ({@code updateColor()}, transcribed verbatim below) plus a {@code colorMod} per-particle brightness
 * jitter (0.8-1.0) - CE has no dedicated PNG for this class, so this port uses CE's own real
 * generic-smoke sheet ({@code textures/particle/particlesmoke.png}, present in CE's own texture
 * catalog) as the closest faithful stand-in for the vanilla smoke sprite CE's superclass drew,
 * documented rather than silently invented.
 */
public class GasFlameParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/particle/particlesmoke.png");

    /** CE's own per-spawn scale, {@code HbmEffectNT.java:470-471} (falls back to CE's own 6.5F default). */
    public final float scale;
    /** CE {@code ParticleGasFlame.colorMod}, a fixed per-particle brightness jitter (0.8-1.0). */
    private final float colorMod;

    public GasFlameParticle(ClientLevel level, double x, double y, double z, double mx, double my, double mz, float scale) {
        // CE: super(world, x, y, z, mX, mY * 1.5, mZ, scale) - the *1.5 on the initial Y motion only.
        super(level, x, y, z, mx, my * 1.5, mz);
        this.scale = scale;
        this.quadSize = scale;
        this.colorMod = 0.8F + this.random.nextFloat() * 0.2F;
        this.lifetime = 30 + this.random.nextInt(13);
        this.hasPhysics = true;
        updateColor();
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

        // CE: prevMo = motionY; super.onUpdate() [vanilla ParticleSmokeNormal physics]; motionY = prevMo
        // (i.e. CE's real class deliberately undoes whatever vertical drift the vanilla smoke superclass
        // applied, then re-applies its own tiny +0.005 lift below) - ported directly as "move, then apply
        // CE's own explicit motion adjustments", skipping the vanilla-smoke intermediate since this class
        // no longer extends it.
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.75D;
        this.yd += 0.005D;
        this.zd *= 0.75D;

        updateColor();
    }

    /** CE {@code ParticleGasFlame.updateColor()}, transcribed verbatim (orange-to-black ember HSB gradient). */
    private void updateColor() {
        float time = (float) this.age / (float) this.lifetime;
        Color color = Color.getHSBColor(
                Math.max((60F - time * 100F) / 360F, 0.0F),
                1F - time * 0.25F,
                1F - time * 0.5F
        );
        this.rCol = (color.getRed() / 255F) * colorMod;
        this.gCol = (color.getGreen() / 255F) * colorMod;
        this.bCol = (color.getBlue() / 255F) * colorMod;
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
                .setColor(this.rCol, this.gCol, this.bCol, 1F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT) // CE ParticleGasFlame.getBrightnessForRender() override: 15728880 = 0xF000F0
                .setNormal(0F, 1F, 0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return HbmParticleRenderTypes.translucent(TEXTURE);
    }

    public static final class Provider implements ParticleProvider<HbmParticleOptions> {
        @Override
        public Particle createParticle(HbmParticleOptions options, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            var tag = options.tag;
            double mx = tag.getDouble("mX");
            double my = tag.getDouble("mY");
            double mz = tag.getDouble("mZ");
            float scale = tag.getFloat("scale");
            return new GasFlameParticle(level, x, y, z, mx, my, mz, scale > 0 ? scale : 6.5F);
        }
    }
}
