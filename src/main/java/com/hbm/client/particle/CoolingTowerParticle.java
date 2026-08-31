package com.hbm.client.particle;

import com.hbm.main.MainRegistry;
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
import org.joml.Vector3f;

/**
 * Real render for CE's {@code com.hbm.particle.ParticleCoolingTower}
 * ({@code upstream/hbm-ce/.../particle/ParticleCoolingTower.java}, read in full) - a slow-drifting,
 * growing, fading steam-puff billboard, backing the {@code Tower} {@code HbmEffectNT} constant
 * ({@code HbmEffectNT.java:911-930}, dispatched by {@code com.hbm.particle.HbmEffect.TOWER}). Texture:
 * CE's {@code NTMClientRegistry.particle_base} sprite, resolving to
 * {@code textures/particle/particle_base.png}.
 * <p>
 * CE's real {@code Tower} handler reads {@code lift}/{@code base}/{@code max}/{@code life}/
 * {@code strafe}/{@code color}/{@code noWind}/{@code alpha} keys from per-spawn NBT with NO defaults
 * (a missing key reads as 0) - {@code com.hbm.particle.HbmEffect.TOWER}'s own handler forwards none of
 * that yet (documented in its own javadoc as "simplified"), so this class bakes in the values from
 * CE's own canonical caller for this exact effect - the actual Cooling Tower (Large) block's steam
 * puff, {@code upstream/hbm-ce/.../tileentity/machine/TileEntityTowerLarge.java:57-64}
 * (verbatim: {@code lift=0.5F, base=1F, max=10F, life=750+rand(250)}, no {@code color}/{@code strafe}/
 * {@code noWind} override, so CE's own class defaults apply for those:
 * {@code strafe=0.075F, windDir=true, alphaMod=0.25F}, color a light grey
 * {@code 0.9 + rand()*0.05}) - not an invented default, the confirmed real values for the effect this
 * type is named after.
 * <p>
 * CE physics/render (verbatim): {@code ageScale = age/maxAge}; {@code alpha = alphaMod - ageScale*
 * alphaMod}; {@code scale = base + (max*ageScale - base)^2}; Y drifts toward {@code lift} at
 * {@code +-0.01/tick}; horizontal gaussian "strafe" jitter scaled by {@code ageScale}; if
 * {@code windDir}: {@code motionX += 0.02*ageScale, motionZ -= 0.01*ageScale}; {@code 0.925} friction
 * every axis every tick.
 */
public class CoolingTowerParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/particle/particle_base.png");

    private static final float BASE_SCALE = 1.0F;
    private static final float MAX_SCALE = 10.0F;
    private static final float LIFT = 0.5F;
    private static final float STRAFE = 0.075F;
    private static final float ALPHA_MOD = 0.25F;

    /**
     * Billboard quad half-size. Vanilla {@link Particle} has no such field - only its
     * {@link net.minecraft.client.particle.TextureSheetParticle} subclass declares one - so, like this
     * port's own {@code com.hbm.particle.engine.ParticleNT#quadSize} and CE's real
     * {@code particleScale}, it's declared locally here.
     */
    private float quadSize;

    public CoolingTowerParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 750 + this.random.nextInt(250);
        this.rCol = this.gCol = this.bCol = 0.9F + this.random.nextFloat() * 0.05F;
        this.alpha = ALPHA_MOD;
        this.quadSize = BASE_SCALE;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        float ageScale = (float) this.age / (float) this.lifetime;
        this.alpha = ALPHA_MOD - ageScale * ALPHA_MOD;
        this.quadSize = BASE_SCALE + (float) Math.pow(MAX_SCALE * ageScale - BASE_SCALE, 2);

        this.age++;
        if (this.yd < LIFT) this.yd += 0.01F;

        this.xd += this.random.nextGaussian() * STRAFE * ageScale;
        this.zd += this.random.nextGaussian() * STRAFE * ageScale;

        // CE default windDir = true (this call site never sets "noWind"): steady eastward drift.
        this.xd += 0.02D * ageScale;
        this.zd -= 0.01D * ageScale;

        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.925D;
        this.yd *= 0.925D;
        this.zd *= 0.925D;
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        if (this.age == 0) return; // CE: renderParticle no-ops on the very first tick.

        Vec3 camPos = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        Vector3f l = new Vector3f(camera.getLeftVector()).mul(this.quadSize);
        Vector3f u = new Vector3f(camera.getUpVector()).mul(this.quadSize);
        // CE never overrides getBrightnessForRender() for this class (unlike most of this task's other
        // particles) - it shades with the world's real ambient light at its position, rather than the
        // LightTexture.FULL_BRIGHT most siblings use. Computed the same way this port's own
        // com.hbm.particle.engine.ParticleNT#getLightColor() already does (a confirmed-compiling call
        // in this exact codebase), rather than assuming vanilla Particle exposes an equivalent method
        // under an unverified name/signature.
        BlockPos lightPos = BlockPos.containing(this.x, this.y, this.z);
        int light = this.level.hasChunkAt(lightPos) ? LevelRenderer.getLightColor(this.level, lightPos) : 0;

        addVertex(consumer, pX - l.x - u.x, pY - l.y - u.y, pZ - l.z - u.z, 1, 1, light);
        addVertex(consumer, pX - l.x + u.x, pY - l.y + u.y, pZ - l.z + u.z, 1, 0, light);
        addVertex(consumer, pX + l.x + u.x, pY + l.y + u.y, pZ + l.z + u.z, 0, 0, light);
        addVertex(consumer, pX + l.x - u.x, pY + l.y - u.y, pZ + l.z - u.z, 0, 1, light);
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
            return new CoolingTowerParticle(level, x, y, z);
        }
    }
}
