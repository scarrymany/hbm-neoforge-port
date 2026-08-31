package com.hbm.client.render.entity.logic;

import com.hbm.client.render.ConstantRenderSweep;
import com.hbm.entity.logic.EntityDeathBlast;
import com.hbm.main.MainRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Ported from CE's {@code com.hbm.render.entity.RenderDeathBlast} (137 lines, {@code extends
 * Render<EntityDeathBlast>}, read in full) - see {@code docs/phase5/
 * boss_and_vehicle_entity_renderers.md}'s named {@code IConstantRenderer} list and {@code
 * docs/phase5/reactor_and_explosion_visual_effects.md}'s Headline finding 5, both of which name this
 * class directly. Draws an 8-wedge vertical "light pillar" fan (2 nested color layers, red then
 * magenta) from {@code y=0} to {@code y=250}, plus a separate pulsing "orb" of nested additive-
 * blended spheres that shrinks and fades over {@link EntityDeathBlast#MAX_AGE}.
 *
 * <h2>What CE's {@code doRender}/{@code renderOrb} actually draw</h2>
 * <pre>
 * if (!renderingConstant) return;
 * disableLighting(); disableTexture2D(); shadeModel(SMOOTH); depthMask(false); enableBlend();
 * blendFunc(SRC_ALPHA, ONE);                                    // additive
 * vector = (0.5,0,0);
 * for i in 0..8: quad(vector*1, y:0-&gt;250) color(1,0,0,1); vector.rotateYaw(45);   // red outer fan
 * for i in 0..8: quad(vector*0.5, y:0-&gt;250) color(1,0,1,1); vector.rotateYaw(45); // magenta inner fan
 * [restore state]
 *
 * // renderOrb:
 * scale = max(0, 10 - 10*ticksExisted/maxAge); alpha = ticksExisted/maxAge;
 * color(1,0,1,alpha); enableBlend(); scale(scale); blendFunc(SRC_ALPHA, ONE_MINUS_SRC_ALPHA);
 * sphere.renderAll();
 * blendFunc(SRC_ALPHA, ONE); scale(1.25); color(1,0,0,alpha*0.125);
 * for i in 0..8: sphere.renderAll(); scale(1.05);
 * </pre>
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>{@code IConstantRenderer} gate</b> - same {@code ConstantRenderSweep} mechanism as this
 *       task's other {@code IConstantRenderer} renderers (see {@link
 *       com.hbm.client.render.entity.effect.BlackHoleRenderer}'s own javadoc for the full
 *       mechanism); {@link EntityDeathBlast} already {@code implements IConstantRenderer} (a real,
 *       already-committed Phase 4 fact, independently re-confirmed by direct read here).</li>
 *   <li><b>{@code Vec3NT.rotateYawSelf(angle)} -&gt; recomputed {@link Vec3#yRot(float)} per
 *       iteration</b> - same already-established idiom {@link
 *       com.hbm.client.render.entity.effect.BlackHoleRenderer}/{@code CloudTomRenderer} use for an
 *       unrelated effect (both fans start from the same base orientation since CE's own mutate-in-
 *       place vector completes a full 360 degrees across the first loop's 8 iterations before the
 *       second loop begins, so recomputing each loop independently from {@code angle*i} is
 *       mathematically equivalent, not a behavior change).</li>
 *   <li><b>{@code sphere.renderAll()} (an untextured, {@code glColor}-tinted OBJ sphere) -&gt;
 *       procedural {@code POSITION_COLOR} UV-sphere</b>, reusing {@link
 *       com.hbm.client.render.entity.effect.BlackHoleRenderer#renderSphere}'s own already-endorsed
 *       substitution pattern (per {@code docs/phase5/boss_and_vehicle_entity_renderers.md} Headline
 *       finding 4: "a sphere is trivial to generate in code; porting the entire OBJ loader just for
 *       one mesh would be substantial overkill") rather than routing an untextured tint through
 *       {@link com.hbm.render.loader.HbmObjModel}'s texture-sampling {@code VertexConsumer} contract,
 *       which has no clean way to reproduce "ignore the mesh's UVs entirely, just flat-tint every
 *       vertex" without binding some placeholder texture. A private local copy of that same 16x16
 *       stack/slice generation is used here (not a shared static helper) to avoid a cross-package
 *       coupling this task's own file-scope ground rules would flag.</li>
 *   <li><b>Raw {@link Tesselator}/{@link BufferBuilder} draws, not a {@code MultiBufferSource}-
 *       batched {@code RenderType}</b> - same already-established, already-reviewed technique {@link
 *       com.hbm.client.render.entity.effect.BlackHoleRenderer} uses (mid-draw blend-function swaps
 *       are not expressible as one static {@code RenderType}) - the {@code MultiBufferSource}/{@code
 *       packedLight} parameters {@link #render} receives are deliberately unused, matching CE's own
 *       fixed-function immediate-mode draw never sampling the lightmap.</li>
 *   <li><b>{@code entity.ticksExisted}/{@code EntityDeathBlast.maxAge} -&gt; {@code
 *       entity.tickCount}/{@link EntityDeathBlast#MAX_AGE}</b> - renamed 1:1, no behavior change.</li>
 * </ul>
 */
public class DeathBlastRenderer extends EntityRenderer<EntityDeathBlast> {

    private static final int STACKS = 10;
    private static final int SLICES = 10;

    public DeathBlastRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public void render(EntityDeathBlast entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
        if (!ConstantRenderSweep.isRenderingConstant()) return;

        poseStack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        emitFan(buffer, matrix, 1.0F, 1F, 0F, 0F, 1F);
        emitFan(buffer, matrix, 0.5F, 1F, 0F, 1F, 1F);

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();

        renderOrb(entity, poseStack);
    }

    /** CE's outer/inner fan loops (see class javadoc) - a fresh {@code Vec3(radius,0,0).yRot(45*i)} per wedge. */
    private static void emitFan(BufferBuilder buffer, Matrix4f matrix, float radius, float r, float g, float b, float a) {
        for (int i = 0; i < 8; i++) {
            Vec3 v0 = new Vec3(radius, 0, 0).yRot((float) Math.toRadians(45 * i));
            Vec3 v1 = new Vec3(radius, 0, 0).yRot((float) Math.toRadians(45 * (i + 1)));

            buffer.addVertex(matrix, (float) v0.x, 250F, (float) v0.z).setColor(r, g, b, a);
            buffer.addVertex(matrix, (float) v0.x, 0F, (float) v0.z).setColor(r, g, b, a);
            buffer.addVertex(matrix, (float) v1.x, 0F, (float) v1.z).setColor(r, g, b, a);
            buffer.addVertex(matrix, (float) v1.x, 250F, (float) v1.z).setColor(r, g, b, a);
        }
    }

    /** CE: {@code RenderDeathBlast.renderOrb} - see class javadoc. */
    private void renderOrb(EntityDeathBlast entity, PoseStack poseStack) {
        float scale = 10F - 10F * ((float) entity.tickCount / (float) EntityDeathBlast.MAX_AGE);
        float alpha = (float) entity.tickCount / (float) EntityDeathBlast.MAX_AGE;
        if (scale < 0F) scale = 0F;

        poseStack.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        poseStack.scale(scale, scale, scale);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        renderSphere(poseStack.last().pose(), 1F, 0F, 1F, alpha);

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        poseStack.scale(1.25F, 1.25F, 1.25F);
        for (int i = 0; i < 8; i++) {
            renderSphere(poseStack.last().pose(), 1F, 0F, 0F, alpha * 0.125F);
            poseStack.scale(1.05F, 1.05F, 1.05F);
        }

        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        poseStack.popPose();
    }

    /** CE OBJ {@code sphere.renderAll()} -&gt; procedural UV-sphere - see class javadoc. */
    private static void renderSphere(Matrix4f matrix, float r, float g, float b, float a) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < STACKS; i++) {
            double phi1 = Math.PI * i / STACKS;
            double phi2 = Math.PI * (i + 1) / STACKS;
            for (int j = 0; j < SLICES; j++) {
                double theta1 = 2.0 * Math.PI * j / SLICES;
                double theta2 = 2.0 * Math.PI * (j + 1) / SLICES;

                float x1 = (float) (Math.sin(phi1) * Math.cos(theta1)), y1 = (float) Math.cos(phi1), z1 = (float) (Math.sin(phi1) * Math.sin(theta1));
                float x2 = (float) (Math.sin(phi2) * Math.cos(theta1)), y2 = (float) Math.cos(phi2), z2 = (float) (Math.sin(phi2) * Math.sin(theta1));
                float x3 = (float) (Math.sin(phi2) * Math.cos(theta2)), y3 = (float) Math.cos(phi2), z3 = (float) (Math.sin(phi2) * Math.sin(theta2));
                float x4 = (float) (Math.sin(phi1) * Math.cos(theta2)), y4 = (float) Math.cos(phi1), z4 = (float) (Math.sin(phi1) * Math.sin(theta2));

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

    @Override
    public ResourceLocation getTextureLocation(EntityDeathBlast entity) {
        return null;
    }
}
