package com.hbm.client.render.entity.effect;

import com.hbm.client.render.ConstantRenderSweep;
import com.hbm.entity.effect.EntityBlackHole;
import com.hbm.entity.effect.EntityRagingVortex;
import com.hbm.entity.effect.EntityVortex;
import com.hbm.main.MainRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Ported from CE's {@code com.hbm.render.entity.RenderBlackHole} ({@code upstream/hbm-ce/.../render/
 * entity/RenderBlackHole.java}, 460 lines, read in full) - the shared renderer for the whole
 * gravity-well family ({@link EntityBlackHole}/{@link EntityVortex}/{@link EntityRagingVortex}, all
 * registered directly against this class; {@code EntityQuasar} gets the thin {@link QuasarRenderer}
 * subclass below). Per {@code docs/phase5/boss_and_vehicle_entity_renderers.md} Headline finding 4
 * and {@code docs/phase5/reactor_and_explosion_visual_effects.md} Headline finding 5 - both already
 * fully researched this renderer, this class does not re-derive the design, only implements it.
 *
 * <h2>What CE's {@code doRender} actually draws</h2>
 * <pre>
 * if (!ClientProxy.renderingConstant) return;
 * pushMatrix(); translate(x,y,z); disableLighting(); disableCull();
 * float size = entity.getDataManager().get(SIZE); scale(size,size,size);
 * bindTexture(hole); blastModel.renderAll();               // Sphere.obj, "hole" texture
 * if (entity instanceof EntityVortex) renderSwirl(...);
 * else if (entity instanceof EntityRagingVortex) { renderSwirl(...); renderJets(...); }
 * else { renderDisc(...); renderJets(...); }                // EntityBlackHole (+ EntityQuasar, below)
 * enableCull(); enableLighting(); popMatrix();
 * </pre>
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>OBJ sphere -&gt; procedural UV-sphere, no asset dependency at all.</b> CE loads a real
 *       {@code models/Sphere.obj} mesh for the black hole's solid core; {@code upstream/neo-edition}'s
 *       own {@code render/entity/effect/RenderBlackHole.java} (396 lines, read in full - a real,
 *       compiling 1.21.1 port, cross-checked strictly for API shape per this port's ground rules, not
 *       behavior) replaces it with a hand-built 16x16-stack/slice UV sphere drawn straight into a
 *       {@code Tesselator}/{@code POSITION_COLOR} triangle stream ({@link #renderSphere} below) - a
 *       small, sensible simplification the boss/vehicle report's Headline finding 4 already endorsed
 *       ("a sphere is trivial to generate in code; porting the entire OBJ loader just for one mesh
 *       would be substantial overkill"). The much larger remainder of the effect (disc/swirl/jets) was
 *       <b>already</b> pure procedural immediate-mode geometry in CE, never OBJ - {@link #renderDisc}/
 *       {@link #renderSwirl}/{@link #renderJets} below are a close, verified-against-CE-source
 *       transcription of that CE math (every color/step/rotation value below was independently
 *       re-verified against CE's real {@code RenderBlackHole.java} source in this task's own
 *       research, not merely copied from Neo Edition's own translation - see e.g. {@link
 *       #getColorFromIteration}'s hex-for-hex match against CE's {@code setColorFromIteration}).</li>
 *   <li><b>{@code IConstantRenderer} gate, missing from Neo Edition, restored here</b> -
 *       Neo Edition's own {@code RenderBlackHole}/{@code RenderQuasar} do <b>not</b> guard {@code
 *       render} on any equivalent of {@code ClientProxy.renderingConstant} at all (confirmed: that repo
 *       has zero {@code IConstantRenderer}/{@code renderingConstant} hits anywhere, per {@code
 *       reactor_and_explosion_visual_effects.md}'s own grep) - a real incompleteness in that reference,
 *       not something to copy (ground rule 2: Neo Edition "has known bugs and incompleteness of its
 *       own"). This port's own {@code com.hbm.client.render.ConstantRenderSweep} already exists
 *       precisely to drive this; {@link #render} below early-returns unless {@link
 *       ConstantRenderSweep#isRenderingConstant()}, the direct, faithful port of CE's own {@code if
 *       (!ClientProxy.renderingConstant) return;}. {@link EntityBlackHole} itself did not yet
 *       {@code implements IConstantRenderer} either (a real gap this port's own base class had, CE's
 *       real class does - see {@code EntityBlackHole}'s own updated class javadoc) - fixed alongside
 *       this renderer as a small, necessary, same-scope correctness fix (matching the precedent
 *       {@code reactor_and_explosion_visual_effects.md} itself set by flagging {@code EntityUFO.beam}
 *       as "a small blocker note... whoever implements that renderer" should also fix).</li>
 *   <li><b>{@code entity.getId() % 90 - 45}/{@code entity.getId() % 360}/{@code entity.tickCount}</b> -
 *       CE's {@code entity.getEntityId()}/{@code entity.ticksExisted}, renamed 1:1, no behavior
 *       change (confirmed identical in both CE and Neo Edition source).</li>
 *   <li><b>{@code Vec3NT.rotateYawSelf}/{@code Vec3.rotateAroundYRad} -&gt; recomputed {@link
 *       Vec3#yRot(float)} per corner</b>, rather than porting CE/Neo Edition's mutable-vector
 *       cumulative-rotation helper (neither class exists in this port). {@link
 *       com.hbm.client.render.entity.effect.CloudTomRenderer} already established this exact
 *       "{@code new Vec3(1,0,0).yRot(angle*i)}, recomputed per iteration rather than mutated in
 *       place" idiom in this same package for an unrelated entity - reused here rather than
 *       re-derived, and mathematically equivalent to CE's cumulative rotation (both produce the same
 *       16 evenly-spaced unit vectors around the circle; recomputing avoids any small floating-point
 *       drift a long cumulative-rotation chain could otherwise accumulate, which is a strict
 *       improvement, not a behavior change - the visual is 16 evenly-distributed, per-{@code k}
 *       identically-colored wedges either way, so rotation direction/starting-phase parity has no
 *       visible effect).</li>
 *   <li><b>Raw {@link Tesselator}/{@link BufferBuilder}/{@link BufferUploader#drawWithShader} draws,
 *       not a {@link MultiBufferSource}-batched {@code RenderType}</b> - this renderer mid-draw swaps
 *       blend functions between passes ({@code SRC_ALPHA,ONE_MINUS_SRC_ALPHA} for the first "j" pass,
 *       {@code SRC_ALPHA,ONE} additive for the second) exactly like CE's own immediate-mode GL calls
 *       do, which is not expressible as one static {@code RenderType}. Same already-established,
 *       already-reviewed technique {@code com.hbm.client.render.blockentity.rbmk.
 *       RBMKConsoleHeatmapRenderer} uses for an unrelated in-world effect (see that class's own
 *       "Manual immediate-mode draw" javadoc section) - reused, not re-derived. Because of this, the
 *       {@code MultiBufferSource}/{@code packedLight} parameters {@link #render} receives are
 *       deliberately unused, matching CE's own fixed-function immediate-mode draw never sampling the
 *       lightmap for this effect either.</li>
 *   <li><b>Color arrays are {@code float[4]} in {@code 0..1} range, not {@code int[4]} in
 *       {@code 0..255}</b> - matching CE's own {@code RenderBlackHole} internal representation
 *       exactly ({@code protected void setColorFromIteration(int, float, float[] col)}, a reused
 *       {@code float[4]} scratch array packed to a GL color only at the final draw call via {@code
 *       NTMBufferBuilder.packColor}/{@code packCurrentColor}) - a more literal translation than
 *       converting to {@code int} 0-255 the way {@code upstream/neo-edition}'s own port does. This
 *       also lets every draw call use {@code VertexConsumer.setColor(float,float,float,float)}, the
 *       overload already confirmed real and in active use elsewhere in this exact port ({@code
 *       CloudTomRenderer}, {@code RBMKConsoleHeatmapRenderer}), rather than depending on the
 *       {@code setColor(int,int,int,int)} overload, which is only independently confirmed via Neo
 *       Edition's own source in this task's research and not yet exercised anywhere in this port's
 *       own already-reviewed code - a small, deliberate risk reduction for this renderer's high
 *       draw-call volume.</li>
 * </ul>
 *
 * <h2>Deliberately not ported</h2>
 * CE's {@code renderFlare(EntityBlackHole)} method (lines 368-426, a random-triangle-fan "lens flare"
 * effect) is <b>dead code in CE itself</b> - {@code doRender} never calls it (confirmed by direct
 * read of the full 460-line source: the only 3 render calls inside {@code doRender} are {@code
 * renderDisc}/{@code renderSwirl}/{@code renderJets}, gated by the {@code instanceof} chain above).
 * Neo Edition independently reached the same conclusion (its own port omits an equivalent method
 * entirely) - not ported here either, per ground rule 1 ("CE is the sole source of truth for
 * behavior" - and CE's own behavior is "never call this method").
 *
 * <h2>Asset gap</h2>
 * {@code textures/models/explosion/blackhole.png} (bound but never actually sampled by any draw call
 * in this class - see {@link #getTextureLocation}), {@code textures/entity/bhole.png} (swirl), and
 * {@code textures/entity/bholedisc.png} (disc) do not exist in this port's {@code src/main/resources}
 * yet (confirmed by directory search; on-disk names are lowercase, matching CE's real files despite
 * CE's Java source spelling {@code "BlackHole.png"}/{@code "bholeDisc.png"} with mixed case - same
 * already-flagged class of gap {@link CloudFleijaRenderer}'s own javadoc documents for its sibling
 * entity, not re-derived here). The disc/swirl draw calls bind a missing texture and will show
 * Minecraft's stock magenta/black "missing texture" checkerboard rather than throwing (a plain
 * texture-manager fallback, unlike a missing OBJ file's hard runtime exception - the central sphere
 * itself needs no texture at all, it is untextured {@code POSITION_COLOR} geometry, so it renders its
 * intended solid-black appearance correctly today even with zero asset migration done).
 */
public class BlackHoleRenderer<T extends EntityBlackHole> extends EntityRenderer<T> {

    protected static final ResourceLocation HOLE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/models/explosion/blackhole.png");
    protected static final ResourceLocation SWIRL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/entity/bhole.png");
    protected static final ResourceLocation DISC =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/entity/bholedisc.png");

    private static final int STACKS = 16;
    private static final int SLICES = 16;

    public BlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
        // CE: if (!ClientProxy.renderingConstant) return; - see class javadoc.
        if (!ConstantRenderSweep.isRenderingConstant()) return;

        poseStack.pushPose();

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        float size = entity.getSize();
        poseStack.scale(size, size, size);

        Matrix4f matrix = poseStack.last().pose();
        renderSphere(matrix, 1.0F, 0F, 0F, 0F, 1F);

        if (entity instanceof EntityVortex) {
            renderSwirl(entity, partialTick, poseStack);
        } else if (entity instanceof EntityRagingVortex) {
            renderSwirl(entity, partialTick, poseStack);
            renderJets(entity, poseStack);
        } else {
            renderDisc(entity, partialTick, poseStack);
            renderJets(entity, poseStack);
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    /** CE: {@code blastModel.renderAll()} over {@code Sphere.obj} - see class javadoc's "OBJ sphere -&gt; procedural" note. */
    protected void renderSphere(Matrix4f matrix, float radius, float r, float g, float b, float a) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < STACKS; i++) {
            double phi1 = Math.PI * i / STACKS;
            double phi2 = Math.PI * (i + 1) / STACKS;

            for (int j = 0; j < SLICES; j++) {
                double theta1 = 2.0 * Math.PI * j / SLICES;
                double theta2 = 2.0 * Math.PI * (j + 1) / SLICES;

                float x1 = (float) (radius * Math.sin(phi1) * Math.cos(theta1));
                float y1 = (float) (radius * Math.cos(phi1));
                float z1 = (float) (radius * Math.sin(phi1) * Math.sin(theta1));

                float x2 = (float) (radius * Math.sin(phi2) * Math.cos(theta1));
                float y2 = (float) (radius * Math.cos(phi2));
                float z2 = (float) (radius * Math.sin(phi2) * Math.sin(theta1));

                float x3 = (float) (radius * Math.sin(phi2) * Math.cos(theta2));
                float y3 = (float) (radius * Math.cos(phi2));
                float z3 = (float) (radius * Math.sin(phi2) * Math.sin(theta2));

                float x4 = (float) (radius * Math.sin(phi1) * Math.cos(theta2));
                float y4 = (float) (radius * Math.cos(phi1));
                float z4 = (float) (radius * Math.sin(phi1) * Math.sin(theta2));

                buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
                buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
                buffer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);

                buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
                buffer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
                buffer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a);
            }
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    /** {@link QuasarRenderer} overrides this to {@link #DISC}'s purple-tinted {@code bholeD.png} sibling. */
    protected ResourceLocation discTex() {
        return DISC;
    }

    /** CE: {@code RenderBlackHole.renderDisc(EntityBlackHole, float)} - the rotating "accretion disc" of concentric color-ramped fan wedges. */
    protected void renderDisc(Entity entity, float interp, PoseStack poseStack) {
        float glow = 0.75F;

        RenderSystem.setShaderTexture(0, discTex());

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getId() % 90 - 45));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        int count = 16;
        float angleStep = (float) (Math.PI * 2 / count);

        for (int k = 0; k < steps(); k++) {
            poseStack.pushPose();

            float rotation = (entity.tickCount + interp % 360) * -((float) Math.pow(k + 1, 1.25));
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

            Matrix4f matrix = poseStack.last().pose();
            float s = 3F - k * 0.175F;

            for (int j = 0; j < 2; j++) {
                RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

                for (int i = 0; i < count; i++) {
                    Vec3 v0 = new Vec3(1, 0, 0).yRot(angleStep * i);
                    Vec3 v1 = new Vec3(1, 0, 0).yRot(angleStep * (i + 1));
                    float vx0 = (float) v0.x, vz0 = (float) v0.z;
                    float vx1 = (float) v1.x, vz1 = (float) v1.z;

                    float[] inner = (j == 0) ? getColorFromIteration(k, 1F) : new float[]{1F, 1F, 1F, glow};
                    float[] outer = getColorFromIteration(k, 0F);

                    buffer.addVertex(matrix, vx0 * s, 0, vz0 * s)
                            .setUv(0.5F + vx0 * 0.25F, 0.5F + vz0 * 0.25F)
                            .setColor(inner[0], inner[1], inner[2], inner[3]);
                    buffer.addVertex(matrix, vx0 * s * 2, 0, vz0 * s * 2)
                            .setUv(0.5F + vx0 * 0.5F, 0.5F + vz0 * 0.5F)
                            .setColor(outer[0], outer[1], outer[2], outer[3]);
                    buffer.addVertex(matrix, vx1 * s * 2, 0, vz1 * s * 2)
                            .setUv(0.5F + vx1 * 0.5F, 0.5F + vz1 * 0.5F)
                            .setColor(outer[0], outer[1], outer[2], outer[3]);
                    float[] inner2 = (j == 0) ? getColorFromIteration(k, 1F) : new float[]{1F, 1F, 1F, glow};
                    buffer.addVertex(matrix, vx1 * s, 0, vz1 * s)
                            .setUv(0.5F + vx1 * 0.25F, 0.5F + vz1 * 0.25F)
                            .setColor(inner2[0], inner2[1], inner2[2], inner2[3]);
                }

                BufferUploader.drawWithShader(buffer.buildOrThrow());
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            }

            poseStack.popPose();
        }

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        poseStack.popPose();
    }

    /** {@link QuasarRenderer} does not override this (CE's {@code RenderQuasar.steps() -> 15} matches the base). */
    protected int steps() {
        return 15;
    }

    /**
     * CE: {@code RenderBlackHole.setColorFromIteration(int, float, float[])} - the disc's
     * orange-to-white-to-cyan color ramp, transcribed hex-for-hex from CE source. Returns a fresh
     * {@code float[]{r,g,b,a}} (0..1) rather than mutating a shared scratch array the way CE's own
     * {@code float[] col} out-parameter does - simpler and safe given this port's per-call
     * allocation cost is negligible next to this method's own draw-call volume.
     */
    protected float[] getColorFromIteration(int iteration, float alpha) {
        if (iteration < 5) {
            float g = 0.125F + iteration * (1F / 10F);
            return new float[]{1F, g, 0F, alpha};
        }
        if (iteration == 5) {
            return new float[]{1F, 1F, 1F, alpha};
        }
        // iteration > 5
        int i = iteration - 6;
        float r = 1.0F - i * (1F / 9F);
        float g = 1F - i * (1F / 9F);
        float b = i * (1F / 5F);
        return new float[]{r, g, b, alpha};
    }

    /** CE: {@code RenderBlackHole.renderSwirl(EntityBlackHole, float)} - {@link EntityVortex}/{@link EntityRagingVortex}'s rotating funnel-cloud texture. */
    protected void renderSwirl(Entity entity, float interp, PoseStack poseStack) {
        float glow = 0.75F;
        if (entity instanceof EntityRagingVortex) {
            glow = 0.25F;
        }

        RenderSystem.setShaderTexture(0, SWIRL);

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getId() % 90 - 45));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + interp % 360) * -5));

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        Matrix4f matrix = poseStack.last().pose();

        float s = 3F;
        int count = 16;
        float angleStep = (float) (Math.PI * 2 / count);

        float[] colorFull = getColorFull(entity);
        float[] colorNone = getColorNone(entity);
        float[] glowColor = {1F, 1F, 1F, glow};

        // Swirl, inner part (solid).
        for (int j = 0; j < 2; j++) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            for (int i = 0; i < count; i++) {
                Vec3 v0 = new Vec3(1, 0, 0).yRot(angleStep * i);
                Vec3 v1 = new Vec3(1, 0, 0).yRot(angleStep * (i + 1));
                float vx0 = (float) v0.x, vz0 = (float) v0.z;
                float vx1 = (float) v1.x, vz1 = (float) v1.z;

                float[] ring = (j == 0) ? colorFull : glowColor;

                buffer.addVertex(matrix, vx0 * 0.9F, 0, vz0 * 0.9F)
                        .setUv(0.5F + vx0 * 0.25F / s * 0.9F, 0.5F + vz0 * 0.25F / s * 0.9F)
                        .setColor(0F, 0F, 0F, 1F);
                buffer.addVertex(matrix, vx0 * s, 0, vz0 * s)
                        .setUv(0.5F + vx0 * 0.25F, 0.5F + vz0 * 0.25F)
                        .setColor(ring[0], ring[1], ring[2], ring[3]);
                buffer.addVertex(matrix, vx1 * s, 0, vz1 * s)
                        .setUv(0.5F + vx1 * 0.25F, 0.5F + vz1 * 0.25F)
                        .setColor(ring[0], ring[1], ring[2], ring[3]);
                buffer.addVertex(matrix, vx1 * 0.9F, 0, vz1 * 0.9F)
                        .setUv(0.5F + vx1 * 0.25F / s * 0.9F, 0.5F + vz1 * 0.25F / s * 0.9F)
                        .setColor(0F, 0F, 0F, 1F);
            }

            BufferUploader.drawWithShader(buffer.buildOrThrow());
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        }

        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        // Swirl, outer part (fade).
        for (int j = 0; j < 2; j++) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            for (int i = 0; i < count; i++) {
                Vec3 v0 = new Vec3(1, 0, 0).yRot(angleStep * i);
                Vec3 v1 = new Vec3(1, 0, 0).yRot(angleStep * (i + 1));
                float vx0 = (float) v0.x, vz0 = (float) v0.z;
                float vx1 = (float) v1.x, vz1 = (float) v1.z;

                float[] inner = (j == 0) ? colorFull : glowColor;

                buffer.addVertex(matrix, vx0 * s, 0, vz0 * s)
                        .setUv(0.5F + vx0 * 0.25F, 0.5F + vz0 * 0.25F)
                        .setColor(inner[0], inner[1], inner[2], inner[3]);
                buffer.addVertex(matrix, vx0 * s * 2, 0, vz0 * s * 2)
                        .setUv(0.5F + vx0 * 0.5F, 0.5F + vz0 * 0.5F)
                        .setColor(colorNone[0], colorNone[1], colorNone[2], colorNone[3]);
                buffer.addVertex(matrix, vx1 * s * 2, 0, vz1 * s * 2)
                        .setUv(0.5F + vx1 * 0.5F, 0.5F + vz1 * 0.5F)
                        .setColor(colorNone[0], colorNone[1], colorNone[2], colorNone[3]);
                buffer.addVertex(matrix, vx1 * s, 0, vz1 * s)
                        .setUv(0.5F + vx1 * 0.25F, 0.5F + vz1 * 0.25F)
                        .setColor(inner[0], inner[1], inner[2], inner[3]);
            }

            BufferUploader.drawWithShader(buffer.buildOrThrow());
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        }

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        poseStack.popPose();
    }

    /** CE: {@code RenderBlackHole.renderJets(EntityBlackHole, float)} - the two untextured white triangle-fan polar jets ({@link EntityRagingVortex}/{@link EntityBlackHole}/{@code EntityQuasar} only, never plain {@link EntityVortex}). */
    protected void renderJets(Entity entity, PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getId() % 90 - 45));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        Matrix4f matrix = poseStack.last().pose();

        for (int j = -1; j <= 1; j += 2) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

            buffer.addVertex(matrix, 0, 0, 0).setColor(1F, 1F, 1F, 0.35F);

            float jetAngleStep = (float) (Math.PI / 6 * -j);
            for (int i = 0; i <= 12; i++) {
                Vec3 jet = new Vec3(0.5, 0, 0).yRot(jetAngleStep * i);
                buffer.addVertex(matrix, (float) jet.x, 10 * j, (float) jet.z).setColor(1F, 1F, 1F, 0F);
            }

            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        poseStack.popPose();
    }

    /** CE: {@code RenderBlackHole.setColorFull} - 0x3898b3 teal ({@link EntityVortex}), 0xe8390d red ({@link EntityRagingVortex}), 0xFFB900 gold (default/{@code EntityBlackHole}), alpha forced opaque. */
    protected float[] getColorFull(Entity entity) {
        if (entity instanceof EntityVortex) {
            return unpack(0x3898b3, 1F);
        } else if (entity instanceof EntityRagingVortex) {
            return unpack(0xe8390d, 1F);
        } else {
            return unpack(0xFFB900, 1F);
        }
    }

    /** CE: {@code RenderBlackHole.setColorNone} - same 3 colors as {@link #getColorFull}, alpha forced 0. */
    protected float[] getColorNone(Entity entity) {
        if (entity instanceof EntityVortex) {
            return unpack(0x3898b3, 0F);
        } else if (entity instanceof EntityRagingVortex) {
            return unpack(0xe8390d, 0F);
        } else {
            return unpack(0xFFB900, 0F);
        }
    }

    /** CE: {@code NTMRenderHelper.unpackColor(int, float[])} - a plain {@code 0xRRGGBB} unpack to {@code 0..1} float. */
    private static float[] unpack(int rgb, float a) {
        float r = ((rgb >> 16) & 0xFF) / 255F;
        float g = ((rgb >> 8) & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;
        return new float[]{r, g, b, a};
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return HOLE;
    }
}
