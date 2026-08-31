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
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Real render for CE's {@code com.hbm.particle.bullet_hit.ParticleBloodParticle}
 * ({@code upstream/hbm-ce/.../particle/bullet_hit/ParticleBloodParticle.java}, read in full) - a
 * gravity-affected, rotating, fading blood-droplet billboard, one quarter of CE's gun-impact VFX
 * bucket ({@code BulletImpact}, {@code HbmEffectNT.java:1152-1298}, gated in CE on
 * {@code GeneralConfig.bloodFX}). Texture: CE's {@code ResourceManager.blood_particles} =
 * {@code textures/misc/blood_particles.png}, a 4x4 (16-frame) sheet indexed by a per-spawn
 * {@code texIdx} this port cannot carry yet - see below.
 * <p>
 * <b>Deliberately simplified, no call site exists yet</b> ({@code docs/phase5/
 * custom_particle_types_registry.md}'s own survey found zero committed TODO referencing this whole
 * {@code bullet_hit} bucket anywhere in this port - the gun-framework's impact-VFX call site has not
 * been stubbed by whichever pass owns it yet). {@link ModParticleTypes#BLOOD} is a plain
 * {@link SimpleParticleType} with no data channel, so this class cannot receive CE's real per-spawn
 * {@code texIdx}/{@code scale}/{@code scaleOverLife}/{@code lifetime} - it uses CE's own real defaults
 * from {@code BulletImpact}'s entity-hit branch instead of inventing values
 * ({@code HbmEffectNT.java:1268-1276}: {@code idx = rand(9)}, {@code scale = (0.5+rand)*(1+rand*3)},
 * {@code scaleOverLifetime = 0.5+rand*0.5}, {@code lifetime = 10+rand(5)}, tinted dark red
 * {@code color(0.5F, 0F, 0F)}). CE's block-hit decal/flow spray
 * ({@code ParticleDecal}/{@code ParticleDecalFlow}, its own shader-based dissolve system) has no
 * modern equivalent built anywhere in this port and is out of this task's scope - this class only
 * reproduces the free-flying droplet's own real motion/color/alpha/rotation/scale-growth, not the
 * decal it leaves on the surface it hits.
 * <p>
 * CE physics (verbatim): {@code gravity 1F} ({@code motionY -= 0.04F} on top of base
 * {@link Particle#tick()} gravity), {@code particleScale += scaleOverLifetime} every tick
 * ({@code scaleOverLifetime *= 0.97} decay), a random {@code rotationOverLifetime} decaying
 * {@code 0.95}/tick (or {@code 0.7} once grounded), fading over the last 10 ticks of life
 * ({@code alpha = 1 - clamp((age-(maxAge-10)),0,10)*0.1}).
 */
public class BloodParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/misc/blood_particles.png");
    private static final int GRID = 4;
    private static final float CELL = 1F / GRID;

    /**
     * Billboard quad half-size. Vanilla {@link Particle} has no such field - only its
     * {@link net.minecraft.client.particle.TextureSheetParticle} subclass declares one - so, like this
     * port's own {@code com.hbm.particle.engine.ParticleNT#quadSize} and CE's real
     * {@code particleScale}, it's declared locally here.
     */
    private float quadSize;
    private final int texIdx;
    private float scaleOverLifetime;
    private float rotation;
    private float prevRotation;
    private float rotationSpeed;

    public BloodParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
        super(level, x, y, z, dx, dy, dz);
        this.texIdx = this.random.nextInt(9);
        float scale = (1F + this.random.nextFloat() * 3F);
        this.quadSize = (0.5F + this.random.nextFloat()) * scale;
        this.scaleOverLifetime = 0.5F + this.random.nextFloat() * 0.5F;
        this.lifetime = 10 + this.random.nextInt(5);
        this.rotation = this.random.nextFloat() * Mth.TWO_PI;
        this.prevRotation = this.rotation;
        this.rotationSpeed = this.random.nextFloat() * 0.3F - 0.15F;
        this.gravity = 1.0F;
        this.rCol = 0.5F;
        this.gCol = 0F;
        this.bCol = 0F;
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
        this.scaleOverLifetime *= 0.97F;
        this.quadSize += this.scaleOverLifetime;
        if (this.onGround) this.age += 2; // CE: sits and fades ~3x faster once landed.
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        this.alpha = 1F - Mth.clamp((this.age + partialTicks) - (this.lifetime - 10), 0, 10) * 0.1F;
        float angle = Mth.lerp(partialTicks, this.prevRotation, this.rotation);

        // Rotates the billboard's own left/up basis around the view axis by `angle` - the modern
        // equivalent of CE's quaternion-based rotationAngle roll (ParticleBloodParticle.java:142-155).
        // Built via Quaternionf.rotateAxis/.transform rather than an assumed Vector3f.rotateAxis
        // overload, matching this codebase's own confirmed-compiling pattern
        // (com.hbm.client.render.entity.mob.UfoRenderer's identical rotateAxis usage). The view axis
        // itself is derived as left x up (both confirmed real Camera accessors already used elsewhere
        // in this codebase, e.g. TorexRenderer) rather than an unconfirmed Camera#getLookVector call.
        Vector3f left0 = new Vector3f(camera.getLeftVector());
        Vector3f up0 = new Vector3f(camera.getUpVector());
        Vector3f lookAxis = new Vector3f(left0).cross(up0).normalize();
        Quaternionf roll = new Quaternionf().rotateAxis(angle, lookAxis.x(), lookAxis.y(), lookAxis.z());
        Vector3f l = roll.transform(new Vector3f(left0)).mul(this.quadSize * 0.1F);
        Vector3f u = roll.transform(new Vector3f(up0)).mul(this.quadSize * 0.1F);

        float u0 = (this.texIdx % GRID) * CELL;
        float v0 = (this.texIdx / GRID) * CELL;

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
            return new BloodParticle(level, x, y, z, dx, dy, dz);
        }
    }
}
