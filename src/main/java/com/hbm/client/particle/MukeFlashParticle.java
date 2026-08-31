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
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Real render for CE's {@code com.hbm.particle.ParticleMukeFlash}
 * ({@code upstream/hbm-ce/.../particle/ParticleMukeFlash.java}, read in full) - the warm-white flash
 * burst opening a {@code Muke} nuke-flash effect, which ALSO self-triggers CE's toroidal
 * {@link MukeCloudParticle} swarm 15 ticks after spawn (CE's real {@code Muke} handler spawns only a
 * {@code ParticleMukeWave} + this class, {@code HbmEffectNT.java:392-403}; unlike {@code TinyTot},
 * which inlines the same swarm math directly in its own handler and never constructs this class at all
 * - confirmed by reading both handler bodies side by side, {@code HbmEffectNT.java:392-430}). Backs
 * {@link com.hbm.particle.HbmEffect#MUKE}'s flash half (dispatched via
 * {@code ModParticleTypes.MUKE_FLASH}). Texture: {@code textures/particle/flare.png}
 * ({@code ParticleMukeFlash.java:22}).
 * <p>
 * CE numbers (verbatim): {@code maxAge = 20}, no gravity/collision. At {@code age == 15}: 18 "stem"
 * cloud particles (vertical column, {@code d} from 0 to 1.8 step 0.1), 100 "ground" particles
 * (horizontal spray at {@code y+0.5}), 75 "mush" particles (dome, clamped radius) - counts/formulas
 * transcribed verbatim from {@code ParticleMukeFlash.onUpdate()}. Render: {@code alpha = 1 -
 * (age+partialTicks)/maxAge}; {@code scale = (age+partialTicks)*3 + 1}; 24 camera-billboard quads at
 * fixed, per-index-seeded offsets ({@code new Random(i*31L+1L)}, {@code x/z in [-7.5,7.5]},
 * {@code y in [-3.75,3.75]} - CE reseeds per sub-quad index every frame, so the 24-flare cluster layout
 * is identical across every {@code MukeFlash} instance, a real CE quirk preserved here via
 * {@link RandomSource#create(long)} with the identical seed formula); color fixed
 * {@code (1, 0.9, 0.75, alpha*0.5)}.
 */
public class MukeFlashParticle extends Particle {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/particle/flare.png");
    private static final int FLARE_COUNT = 24;

    private final boolean bf;
    private boolean swarmSpawned;

    public MukeFlashParticle(ClientLevel level, double x, double y, double z, boolean bf) {
        super(level, x, y, z, 0, 0, 0);
        this.lifetime = 20;
        this.rCol = this.gCol = this.bCol = 1.0F;
        this.alpha = 1.0F;
        this.bf = bf;
        this.hasPhysics = false;
        this.setSize(0.2F, 0.2F);
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

        if (this.age == 15 && !this.swarmSpawned) {
            this.swarmSpawned = true;
            spawnSwarm();
        }
    }

    /** CE {@code ParticleMukeFlash.onUpdate()}'s age==15 "Stem/Ground/Mush" cloud swarm, transcribed verbatim. */
    private void spawnSwarm() {
        SimpleParticleType cloudType = this.bf ? ModParticleTypes.MUKE_CLOUD_BF.get() : ModParticleTypes.MUKE_CLOUD.get();

        // Stem
        for (double d = 0.0D; d <= 1.6D; d += 0.1D) {
            double mx = this.random.nextGaussian() * 0.05D;
            double mz = this.random.nextGaussian() * 0.05D;
            double my = d + this.random.nextGaussian() * 0.02D;
            this.level.addParticle(cloudType, this.x, this.y, this.z, mx, my, mz);
        }
        // Ground
        for (int i = 0; i < 100; i++) {
            double mx = this.random.nextGaussian() * 0.5D;
            double my = this.random.nextInt(5) == 0 ? 0.02D : 0.0D;
            double mz = this.random.nextGaussian() * 0.5D;
            this.level.addParticle(cloudType, this.x, this.y + 0.5D, this.z, mx, my, mz);
        }
        // Mush
        for (int i = 0; i < 75; i++) {
            double ix = this.random.nextGaussian() * 0.5D;
            double iz = this.random.nextGaussian() * 0.5D;
            if (ix * ix + iz * iz > 1.5D) {
                ix *= 0.5D;
                iz *= 0.5D;
            }
            double iy = 1.8D + (this.random.nextDouble() * 3.0D - 1.5D) * (0.75D - (ix * ix + iz * iz)) * 0.5D;
            this.level.addParticle(cloudType, this.x, this.y, this.z, ix, iy + this.random.nextGaussian() * 0.02D, iz);
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float dX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float dY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float dZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        this.alpha = 1.0F - ((this.age + partialTicks) / (float) this.lifetime);
        float scale = (this.age + partialTicks) * 3.0F + 1.0F;
        float a = this.alpha * 0.5F;

        Vector3f left = new Vector3f(camera.getLeftVector());
        Vector3f up = new Vector3f(camera.getUpVector());

        for (int i = 0; i < FLARE_COUNT; i++) {
            RandomSource seeded = RandomSource.create(i * 31L + 1L);
            float pX = (float) (dX + seeded.nextDouble() * 15.0D - 7.5D);
            float pY = (float) (dY + seeded.nextDouble() * 7.5D - 3.75D);
            float pZ = (float) (dZ + seeded.nextDouble() * 15.0D - 7.5D);

            Vector3f l = new Vector3f(left).mul(scale);
            Vector3f u = new Vector3f(up).mul(scale);

            addVertex(consumer, pX - l.x - u.x, pY - l.y - u.y, pZ - l.z - u.z, 1, 1, a);
            addVertex(consumer, pX - l.x + u.x, pY - l.y + u.y, pZ - l.z + u.z, 1, 0, a);
            addVertex(consumer, pX + l.x + u.x, pY + l.y + u.y, pZ + l.z + u.z, 0, 0, a);
            addVertex(consumer, pX + l.x - u.x, pY + l.y - u.y, pZ + l.z - u.z, 0, 1, a);
        }
    }

    private void addVertex(VertexConsumer consumer, float x, float y, float z, float u, float v, float alpha) {
        consumer.addVertex(x, y, z)
                .setColor(1.0F, 0.9F, 0.75F, alpha)
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
            // CE's "bf" flag has no data channel on this SimpleParticleType-registered entry - always
            // spawns the non-BF (ParticleMukeCloud) swarm variant. The BF-textured variant is reached
            // via ModParticleTypes.MUKE_CLOUD_BF's own dedicated entry (com.hbm.particle.HbmEffect.BF).
            return new MukeFlashParticle(level, x, y, z, false);
        }
    }
}
