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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Real render for CE's {@code com.hbm.particle.ParticleGiblet}
 * ({@code upstream/hbm-ce/.../particle/ParticleGiblet.java}, read in full) - a heavy, tumbling gib
 * chunk backing {@code Giblets} ({@code HbmEffectNT.java:1442-1467}). Texture varies by
 * {@code gibType} in CE ({@code 0}=meat, {@code 1}=slime, {@code 2}=metal); this class defaults to
 * meat ({@code textures/particle/meat.png}), CE's {@code gibType == 0} case, since
 * {@link ModParticleTypes#GIBLETS} is a plain {@link SimpleParticleType} with no data channel to carry
 * the real per-spawn type. CE's real call site (a "special death" ragdoll-gib burst reading a live
 * entity's real width/height, {@code HbmEffectNT.java:1442-1467}) does not exist as a committed call
 * site in this port yet (see {@code com.hbm.particle.ModParticleTypes}'s own javadoc for the
 * correction that {@code packet/toclient/PacketSpecialDeath.java}'s gib system is a different,
 * unrelated CE class - {@code bullet_hit.ParticleMobGib} - not this one).
 * <p>
 * CE numbers (verbatim): {@code lifetime = 140+rand(20)}, {@code gravity = 2F} ({@code 4F} for
 * {@code gibType == 2}/metal). CE's {@code momentumYaw}/{@code momentumPitch} spin fields are computed
 * but their application to the particle's own rotation is commented out in CE's real source
 * ({@code ParticleGiblet.java}'s {@code onUpdate}) - a real CE dead-code quirk, preserved here by
 * simply not rotating the quad, not "fixed". While airborne (non-metal only), CE spawns one vanilla
 * block-dust puff per tick (melon for meat/slime, matching {@code Block.getStateId(MELON_BLOCK)}) -
 * reproduced via {@link ParticleTypes#BLOCK}.
 */
public class GibletParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/particle/meat.png");

    /**
     * Billboard quad half-size. Vanilla {@link Particle} has no such field - only its
     * {@link net.minecraft.client.particle.TextureSheetParticle} subclass declares one - so, like this
     * port's own {@code com.hbm.particle.engine.ParticleNT#quadSize} and CE's real
     * {@code particleScale}, it's declared locally here.
     */
    private float quadSize;

    public GibletParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
        super(level, x, y, z, dx, dy, dz);
        this.lifetime = 140 + this.random.nextInt(20);
        this.gravity = 2.0F;
        this.quadSize = 0.1F + this.random.nextFloat() * 0.1F;
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

        if (!this.onGround) {
            this.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MELON.defaultBlockState()),
                    this.x, this.y, this.z, 0, 0, 0);
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        Vector3f l = new Vector3f(camera.getLeftVector()).mul(this.quadSize);
        Vector3f u = new Vector3f(camera.getUpVector()).mul(this.quadSize);
        BlockPos lightPos = BlockPos.containing(this.x, this.y, this.z);
        int light = this.level.hasChunkAt(lightPos) ? LevelRenderer.getLightColor(this.level, lightPos) : 0;

        addVertex(consumer, pX - l.x - u.x, pY - l.y - u.y, pZ - l.z - u.z, 0, 0, light);
        addVertex(consumer, pX - l.x + u.x, pY - l.y + u.y, pZ - l.z + u.z, 0, 1, light);
        addVertex(consumer, pX + l.x + u.x, pY + l.y + u.y, pZ + l.z + u.z, 1, 1, light);
        addVertex(consumer, pX + l.x - u.x, pY + l.y - u.y, pZ + l.z - u.z, 1, 0, light);
    }

    private void addVertex(VertexConsumer consumer, float x, float y, float z, float u, float v, int light) {
        consumer.addVertex(x, y, z)
                .setColor(this.rCol, this.gCol, this.bCol, 1.0F)
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
            return new GibletParticle(level, x, y, z, dx, dy, dz);
        }
    }
}
