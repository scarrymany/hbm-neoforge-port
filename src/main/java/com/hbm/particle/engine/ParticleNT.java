package com.hbm.particle.engine;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * The modern (non-vanilla-{@link net.minecraft.client.particle.Particle}) replacement for CE's raw
 * hardware-instanced particle base classes ({@code com.hbm.particle_instanced.ParticleInstanced} +
 * CE's own {@code com.hbm.particle.Particle} intermediate base), as designed by
 * {@code docs/phase5/particle_engine_and_generic_vfx.md}'s "Recommended architecture" point 3.
 * <p>
 * CE's real instanced engine (confirmed by that report's Finding 1, reading
 * {@code upstream/hbm-ce/.../render/InstancedBillboardBatch.java} directly) is literal raw-OpenGL
 * hardware instancing (VAO + per-instance VBO, {@code vertexAttribDivisor}, {@code
 * glDrawArraysInstanced}) behind a custom GL 3.3 shader - there is no 1.21.1 equivalent to preserve,
 * and reimplementing it would fight {@code RenderSystem}'s own internal state assumptions (and,
 * per that report's Key risk #1, likely break Sodium compatibility, which is very probably why
 * {@code upstream/neo-edition} dropped the raw-GL path too - inferred from its
 * {@code sodium_version} compile-only dependency, not from any code comment explaining why).
 * <p>
 * Confirmed real, compiling 1.21.1 shape - this class is a close structural adaptation of
 * {@code upstream/neo-edition/src/main/java/com/hbm/particle/engine/ParticleNT.java} (209 lines, read
 * in full), used strictly to confirm the real NeoForge/Blaze3D API surface (constructor shape,
 * {@link #tick()}/{@link #move}/{@link #setPos} physics, {@link #render}/{@link #getRenderType}
 * abstract contract) - not for behavior or visual design, per this project's standing rule that Neo
 * Edition is never a source of those. A subclass either returns a real {@link RenderType} from
 * {@link #getRenderType()} to be drawn through the shared {@link ParticleEngineNT}'s batched
 * {@link com.mojang.blaze3d.vertex.VertexConsumer} (see {@link EngineHandler}), or returns
 * {@code null} to opt out of the shared buffer entirely and do fully-manual
 * {@code RenderSystem}/{@code Tesselator}/{@code BufferUploader.drawWithShader} immediate-mode
 * rendering inside {@link #render} instead (the {@code consumer} argument is {@code null} in that
 * case) - the same bridge pattern Neo Edition's own {@code RadiationFogParticle} uses for CE's
 * non-standard {@code blendFuncSeparate} fog puff, cross-checked for API shape only.
 * <p>
 * Public API other Phase 5 renderer tasks (this report's own stated audience: {@code c3}'s mushroom
 * cloud and any future high-volume/custom-blend effect) are expected to build on: extend this class,
 * implement {@link #render}/{@link #getRenderType}, then call
 * {@link ParticleEngineNT#INSTANCE}{@code .add(yourParticle)} to have it ticked and drawn
 * automatically every client frame - no further registration needed.
 */
public abstract class ParticleNT {

    private static final AABB INITIAL_AABB = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    public static final double MAXIMUM_COLLISION_VELOCITY_SQUARED = Mth.square(100.0D);

    protected final ClientLevel level;

    public double xo;
    public double yo;
    public double zo;
    public double x;
    public double y;
    public double z;
    public double xd;
    public double yd;
    public double zd;
    public float quadSize;

    private AABB bb;
    public boolean onGround;
    public boolean noClip;
    public boolean dead;
    protected float bbWidth;
    protected float bbHeight;
    protected final RandomSource random;
    public int age;
    public int lifetime;
    public float gravity;
    public float rCol;
    public float gCol;
    public float bCol;
    public float alpha;
    /** pitch */
    public float xRot;
    /** yaw */
    public float yRot;
    /** roll */
    public float zRot;
    protected float roll;
    protected float oRoll;
    protected float friction;
    public boolean verticalCollision;
    public boolean horizontalCollision;
    protected boolean speedUpWhenYMotionIsBlocked;

    protected ParticleNT(ClientLevel level, double x, double y, double z) {
        this.bb = INITIAL_AABB;
        this.noClip = false;
        this.bbWidth = 0.6F;
        this.bbHeight = 1.8F;
        this.random = RandomSource.create();
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 1.0F;
        this.friction = 0.98F;
        this.speedUpWhenYMotionIsBlocked = false;
        this.level = level;
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.lifetime = (int) (4.0F / (this.random.nextFloat() * 0.9F + 0.1F));
        this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
        this.verticalCollision = false;
        this.horizontalCollision = false;
    }

    /** Base per-tick physics (gravity + friction + collision-move); subclasses commonly override to add their own behavior on top. */
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.dead = true;
        } else {
            this.yd -= 0.04 * (double) this.gravity;
            this.move(this.xd, this.yd, this.zd);
            if (this.speedUpWhenYMotionIsBlocked && this.y == this.yo) {
                this.xd *= 1.1;
                this.zd *= 1.1;
            }

            this.xd *= this.friction;
            this.yd *= this.friction;
            this.zd *= this.friction;
            if (this.onGround) {
                this.xd *= 0.7F;
                this.zd *= 0.7F;
            }
        }
    }

    /**
     * Draws this particle. Called once per frame by {@link ParticleEngineNT#render} for every live,
     * non-dead particle, grouped by {@link #getRenderType()}.
     *
     * @param consumer the shared {@link VertexConsumer} for this particle's {@link #getRenderType()}, obtained from
     *                 the frame's {@link net.minecraft.client.renderer.MultiBufferSource.BufferSource} - {@code null}
     *                 when {@link #getRenderType()} itself returned {@code null} (fully-manual immediate-mode path).
     * @param camera camera of the player, for view-relative billboard math.
     * @param partialTicks interpolation fraction for this frame.
     */
    public abstract void render(VertexConsumer consumer, Camera camera, float partialTicks);

    /**
     * The batched {@link RenderType} this particle draws through, or {@code null} to opt out of the
     * shared buffer and do fully-manual rendering inside {@link #render} instead (see class javadoc).
     */
    public abstract RenderType getRenderType();

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + ", Pos (" + this.x + "," + this.y + "," + this.z
                + "), RGBA (" + this.rCol + "," + this.gCol + "," + this.bCol + "," + this.alpha
                + "), Age " + this.age;
    }

    public void setPos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        double halfWidth = this.bbWidth / 2.0D;
        this.setBoundingBox(new AABB(x - halfWidth, y, z - halfWidth, x + halfWidth, y + this.bbHeight, z + halfWidth));
    }

    @OnlyIn(Dist.CLIENT)
    public void move(double x, double y, double z) {
        double origX = x;
        double origY = y;
        double origZ = z;

        if (!this.noClip && (x != 0.0 || y != 0.0 || z != 0.0) && x * x + y * y + z * z < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
            Vec3 pos = new Vec3(x, y, z);
            Vec3 vec3 = Entity.collideBoundingBox(null, pos, this.getBoundingBox(), this.level, List.of());
            boolean xEqual = Mth.equal(pos.x, vec3.x);
            boolean zEqual = Mth.equal(pos.z, vec3.z);
            this.horizontalCollision = !xEqual || !zEqual;
            this.verticalCollision = pos.y != vec3.y;
            x = vec3.x;
            y = vec3.y;
            z = vec3.z;
        }

        if (x != 0.0 || y != 0.0 || z != 0.0) {
            this.setBoundingBox(this.getBoundingBox().move(x, y, z));
            this.setLocationFromBoundingbox();
        }

        this.onGround = origY != y && origY < 0.0;

        if (this.onGround) {
            this.xd = 0.0;
            this.yd = 0.0;
            this.zd = 0.0;
        } else {
            if (origX != x) this.xd = 0.0;
            if (origZ != z) this.zd = 0.0;
        }
    }

    protected void setLocationFromBoundingbox() {
        AABB aabb = this.getBoundingBox();
        this.x = (aabb.minX + aabb.maxX) / 2.0D;
        this.y = aabb.minY;
        this.z = (aabb.minZ + aabb.maxZ) / 2.0D;
    }

    protected int getLightColor() {
        BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
        return this.level.hasChunkAt(blockpos) ? LevelRenderer.getLightColor(this.level, blockpos) : 0;
    }

    public AABB getBoundingBox() {
        return this.bb;
    }

    public void setBoundingBox(AABB bb) {
        this.bb = bb;
    }

    public AABB getRenderBoundingBox() {
        return this.getBoundingBox().inflate(1.0F);
    }

    public void remove() {
        this.dead = true;
    }

    protected void setSize(float width, float height) {
        if (width != this.bbWidth || height != this.bbHeight) {
            this.bbWidth = width;
            this.bbHeight = height;
            AABB aabb = this.getBoundingBox();
            double x0 = (aabb.minX + aabb.maxX - width) / 2.0D;
            double z0 = (aabb.minZ + aabb.maxZ - width) / 2.0D;
            this.setBoundingBox(new AABB(x0, aabb.minY, z0, x0 + this.bbWidth, aabb.minY + this.bbHeight, z0 + this.bbWidth));
        }
    }
}
