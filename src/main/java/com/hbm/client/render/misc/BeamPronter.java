package com.hbm.client.render.misc;

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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

import java.util.Random;

/**
 * Ported from CE's {@code com.hbm.render.misc.BeamPronter} ({@code upstream/hbm-ce/.../render/misc/
 * BeamPronter.java}, 166 lines, read in full) - the shared "tether/beam column" procedural-geometry
 * helper 4 CE renderers in this task's scope depend on: {@code RenderUFO} (the abduction beam, 3
 * nested calls), {@code RenderTaintCrab}/{@code RenderTeslaCrab} (the electric zap tether toward each
 * entry of a per-entity {@code targets} list - see {@link com.hbm.client.render.entity.mob.
 * TaintCrabRenderer}'s own class javadoc for why this port's {@link com.hbm.entity.mob.EntityTaintCrab}/
 * {@link com.hbm.entity.mob.EntityTeslaCrab} have no such list to iterate, so this helper is ported
 * but not actually called by either of those two renderers), and {@code RenderRADBeast} (the tether
 * from the beast to its current "unfortunate soul" grab victim, {@link com.hbm.entity.mob.EntityRADBeast
 * #getTarget()}).
 *
 * <p>CE's real signature is {@code prontBeam(Vec3d skeleton, EnumWaveType, EnumBeamType, int
 * outerColor, int innerColor, int start, int segments, float spinRadius, int layers, float
 * thickness)}, called from inside a caller-pushed GL matrix already translated to the beam's origin
 * (world-space {@code x,y,z}) - {@code skeleton} is the beam's end point <i>relative to that origin</i>
 * (i.e. "target minus source"), not an absolute position. This port keeps that exact contract, just
 * taking the caller's already-translated {@link PoseStack} instead of relying on an implicit GL
 * matrix stack, per this port's already-established {@code GlStateManager} to {@code PoseStack}
 * translation convention (see {@link com.hbm.client.render.entity.effect.BlackHoleRenderer}'s own
 * class javadoc for the same idiom applied to an unrelated effect).
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>Manual immediate-mode draw, not a {@code MultiBufferSource}-batched {@code RenderType}</b> -
 *       same already-established, already-reviewed technique {@link com.hbm.client.render.entity.
 *       effect.BlackHoleRenderer}/{@code com.hbm.client.render.blockentity.rbmk.
 *       RBMKConsoleHeatmapRenderer} both use, reused here rather than re-derived: this helper needs a
 *       one-off additive/translucent blend state exactly matching CE's own hand-managed {@code
 *       GlStateManager} push/pop-state sequence (only {@code SOLID} disables the depth mask and
 *       enables additive blending; {@code LINE} draws opaque, depth-tested lines), which is not a
 *       single static {@code RenderType} a shared {@code MultiBufferSource} buffer could express
 *       cleanly for both modes from one call site.</li>
 *   <li><b>{@code Vec3d.rotateYaw(angle)}/{@code rotateYawSelf(angle)} -&gt; {@link Vec3#yRot(float)}</b>
 *       (a fresh vector each call, not a mutate-in-place cumulative rotation) - same idiom this port's
 *       {@code CloudTomRenderer}/{@code BlackHoleRenderer} already established for an unrelated
 *       effect (see either class's own javadoc for why recomputing rather than mutating is strictly
 *       safer, not a behavior change, for repeated small-angle rotations).</li>
 *   <li><b>{@code sYaw}/{@code sPitch}</b> (the skeleton vector's own yaw/pitch, used to orient the
 *       whole beam's local coordinate frame before the segment loop) is transcribed 1:1 from CE's
 *       {@code Math.atan2}/{@code MathHelper.sqrt} expressions - {@link Mth#sqrt(float)} is this
 *       port's confirmed-real 1.21.1 equivalent (used elsewhere already in this codebase).</li>
 *   <li><b>{@code RenderUtil} GL-state-snapshot fields (CE's {@code prevTex2D}/{@code prevLighting}/
 *       {@code prevCull}/{@code prevBlend}/blend-factor snapshot/{@code prevDepthMask})</b> - CE reads
 *       these purely to restore the caller's exact prior GL state afterward (fixed-function global
 *       state, not scoped). The modern pipeline has no equivalent implicit global state to snapshot
 *       (blend/cull/depth-mask are set explicitly per draw call by whatever code runs next, and this
 *       port's other manual-{@code Tesselator} renderers - {@link com.hbm.client.render.entity.effect.
 *       BlackHoleRenderer} included - already establish the convention of unconditionally setting the
 *       state this one draw call needs, then unconditionally restoring vanilla's own normal defaults
 *       afterward, rather than snapshotting/restoring an arbitrary caller state) - this method follows
 *       that same already-reviewed convention: unconditionally enable blend/disable depth-mask for
 *       {@code SOLID} (or leave depth-mask/blend at vanilla defaults for {@code LINE}, which CE itself
 *       never touches blend for), then unconditionally restore depth-mask/blend/cull to vanilla's own
 *       normal entity-pass defaults on the way out.</li>
 *   <li><b>{@code BobMathUtil.interpolateColor(int,int,float)}</b> - this port's own {@code
 *       com.hbm.util.BobMathUtil} does not carry this exact overload (confirmed by grep - not this
 *       package's file to add a method to per this task's ground rule 8, "stay inside your own task's
 *       package/file scope"), so the identical linear RGB-channel interpolation formula is transcribed
 *       inline as a private helper here instead of importing a nonexistent method.</li>
 * </ul>
 */
public final class BeamPronter {

    private BeamPronter() {}

    public enum WaveType { RANDOM, SPIRAL, STRAIGHT }

    public enum BeamType { SOLID, LINE }

    /**
     * CE: {@code prontBeam(Vec3d skeleton, EnumWaveType wave, EnumBeamType beam, int outerColor,
     * int innerColor, int start, int segments, float spinRadius, int layers, float thickness)}.
     * {@code poseStack} must already be translated to the beam's world-space origin (matching CE's
     * caller-pushed {@code GlStateManager.translate(x,y,z)} before this method's own further
     * pushMatrix) - this method pushes/pops its own additional pose on top, mirroring CE's nested
     * {@code pushMatrix()}/{@code popMatrix()} exactly.
     */
    public static void prontBeam(PoseStack poseStack, Vec3 skeleton, WaveType wave, BeamType beam,
                                  int outerColor, int innerColor, int start, int segments,
                                  float spinRadius, int layers, float thickness) {
        if (segments <= 0) return;

        poseStack.pushPose();

        float sYaw = (float) (Math.atan2(skeleton.x, skeleton.z) * 180F / Math.PI);
        float horiz = Mth.sqrt((float) (skeleton.x * skeleton.x + skeleton.z * skeleton.z));
        float sPitch = (float) (Math.atan2(skeleton.y, horiz) * 180F / Math.PI);

        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
        poseStack.mulPose(Axis.YP.rotationDegrees(sYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(sPitch - 90F));

        boolean solid = beam == BeamType.SOLID;

        if (solid) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(
                solid ? VertexFormat.Mode.QUADS : VertexFormat.Mode.LINES,
                DefaultVertexFormat.POSITION_COLOR);

        double length = skeleton.length();
        double segLength = length / segments;
        Random rand = new Random(start);

        double lastX = 0, lastY = 0, lastZ = 0;

        for (int i = 0; i <= segments; i++) {
            Vec3 unit = new Vec3(0, 1, 0);
            double pX = unit.x * segLength * i;
            double pY = unit.y * segLength * i;
            double pZ = unit.z * segLength * i;

            if (wave != WaveType.STRAIGHT) {
                Vec3 spinner = new Vec3(spinRadius, 0, 0);
                if (wave == WaveType.SPIRAL) {
                    float angle1 = (float) Math.PI * start / 180F;
                    float angle2 = (float) Math.PI * 45F / 180F * i;
                    spinner = spinner.yRot(angle1).yRot(angle2);
                } else {
                    spinner = spinner.yRot((float) (Math.PI * 2 * rand.nextFloat()));
                }
                pX += spinner.x;
                pY += spinner.y;
                pZ += spinner.z;
            }

            if (beam == BeamType.LINE && i > 0) {
                addColored(buffer, matrix, pX, pY, pZ, outerColor);
                addColored(buffer, matrix, lastX, lastY, lastZ, outerColor);
            }

            if (solid && i > 0) {
                float radius = thickness / layers;
                for (int j = 1; j <= layers; j++) {
                    int color = (layers == 1) ? outerColor
                            : interpolateColor(innerColor, outerColor, (float) (j - 1) / (layers - 1));
                    float radJ = radius * j;

                    addColored(buffer, matrix, lastX + radJ, lastY, lastZ + radJ, color);
                    addColored(buffer, matrix, lastX + radJ, lastY, lastZ - radJ, color);
                    addColored(buffer, matrix, pX + radJ, pY, pZ - radJ, color);
                    addColored(buffer, matrix, pX + radJ, pY, pZ + radJ, color);

                    addColored(buffer, matrix, lastX - radJ, lastY, lastZ + radJ, color);
                    addColored(buffer, matrix, lastX - radJ, lastY, lastZ - radJ, color);
                    addColored(buffer, matrix, pX - radJ, pY, pZ - radJ, color);
                    addColored(buffer, matrix, pX - radJ, pY, pZ + radJ, color);

                    addColored(buffer, matrix, lastX + radJ, lastY, lastZ + radJ, color);
                    addColored(buffer, matrix, lastX - radJ, lastY, lastZ + radJ, color);
                    addColored(buffer, matrix, pX - radJ, pY, pZ + radJ, color);
                    addColored(buffer, matrix, pX + radJ, pY, pZ + radJ, color);

                    addColored(buffer, matrix, lastX + radJ, lastY, lastZ - radJ, color);
                    addColored(buffer, matrix, lastX - radJ, lastY, lastZ - radJ, color);
                    addColored(buffer, matrix, pX - radJ, pY, pZ - radJ, color);
                    addColored(buffer, matrix, pX + radJ, pY, pZ - radJ, color);
                }
            }

            lastX = pX;
            lastY = pY;
            lastZ = pZ;
        }

        if (beam == BeamType.LINE) {
            addColored(buffer, matrix, 0, 0, 0, innerColor);
            addColored(buffer, matrix, 0, length, 0, innerColor);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        if (solid) {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }

        poseStack.popPose();
    }

    private static void addColored(BufferBuilder buffer, Matrix4f matrix, double x, double y, double z, int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255F;
        float g = ((rgb >> 8) & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;
        buffer.addVertex(matrix, (float) x, (float) y, (float) z).setColor(r, g, b, 1F);
    }

    /** CE: {@code BobMathUtil.interpolateColor(int, int, float)} - see class javadoc. */
    private static int interpolateColor(int colorA, int colorB, float percentB) {
        float rA = (colorA >> 16 & 0xFF), gA = (colorA >> 8 & 0xFF), bA = (colorA & 0xFF);
        float rB = (colorB >> 16 & 0xFF), gB = (colorB >> 8 & 0xFF), bB = (colorB & 0xFF);
        int r = (int) (rA + (rB - rA) * percentB) & 0xFF;
        int g = (int) (gA + (gB - gA) * percentB) & 0xFF;
        int b = (int) (bA + (bB - bA) * percentB) & 0xFF;
        return (r << 16) | (g << 8) | b;
    }
}
