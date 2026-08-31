package com.hbm.client.render.entity.missile;

import com.hbm.client.render.ConstantRenderSweep;
import com.hbm.entity.missile.EntityMIRV;
import com.hbm.main.MainRegistry;
import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Ported from CE's {@code com.hbm.render.entity.RenderMirv} ({@code upstream/hbm-ce/.../render/
 * entity/RenderMirv.java}, 53 lines, read in full) - the single-mesh OBJ "warhead cone" renderer for
 * the MIRV cluster sub-munition {@link EntityMIRV} ({@code EntityMissileCustom.mirvSplit()} spawns
 * these; per {@code docs/phase4}'s own missile report this port's {@link EntityMIRV} is a plain
 * ballistic free-faller with no seek/guidance logic of its own).
 *
 * <h2>What CE's {@code doRender} actually draws</h2>
 * <pre>
 * if (!ClientProxy.renderingConstant) return;
 * pushMatrix(); translate(x,y,z);
 * rotate(prevYaw + (yaw-prevYaw)*partialTicks - 90, 0,1,0);
 * rotate(prevPitch + (pitch-prevPitch)*partialTicks, 0,0,1);
 * bindTexture(boyTexture);           // textures/models/misc/universaldark.png
 * boyModel.renderAll();              // Mirv.obj, one unnamed/"Cone" group
 * popMatrix();
 * </pre>
 * Headline finding 5 of {@code docs/phase5/reactor_and_explosion_visual_effects.md} names {@code
 * RenderMirv} directly as one of CE's real {@code IConstantRenderer} consumers (alongside {@code
 * RenderBomber}/{@code RenderDeathBlast}/{@code RenderOrbitalLaser}/{@code RenderFOEQ}/{@code
 * RenderBombletZeta}) - confirmed independently here by reading {@code EntityMIRV.java:36}
 * ({@code implements IChunkLoader, IConstantRenderer, IRadarDetectable, IThrowable}) directly.
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>{@code IConstantRenderer} gate</b>: same {@code com.hbm.client.render.ConstantRenderSweep}
 *       mechanism {@link com.hbm.client.render.entity.effect.BlackHoleRenderer}/{@code
 *       com.hbm.client.render.entity.effect.CloudTomRenderer} already use in this port - {@link
 *       #render} early-returns unless {@link ConstantRenderSweep#isRenderingConstant()}. This port's
 *       {@link EntityMIRV} did not yet {@code implements IConstantRenderer} (a real gap - CE's real
 *       class does; fixed alongside this renderer, see {@link EntityMIRV}'s own updated class
 *       javadoc, matching the same small-necessary-fix precedent {@code
 *       reactor_and_explosion_visual_effects.md} set for {@code EntityUFO.beam}).</li>
 *   <li><b>Interpolated yaw/pitch</b>: CE hand-computes {@code prevRotationYaw + (rotationYaw -
 *       prevRotationYaw) * partialTicks} itself inside {@code doRender}. This port's {@link #render}
 *       receives that same interpolated yaw pre-computed as its own {@code entityYaw} parameter
 *       (confirmed: {@code com.hbm.client.render.ConstantRenderSweep#onRenderLevelStage} - the
 *       dispatcher call this class's {@code render} is ultimately invoked from - already computes
 *       {@code Mth.lerp(partialTick, entity.yRotO, entity.getYRot())} and passes it as the {@code
 *       yaw} argument to {@code EntityRenderDispatcher.render}, which forwards it unchanged as
 *       {@code entityYaw} here; this is also vanilla's own standard convention for every {@code
 *       EntityRenderer}, not specific to the constant-render sweep). Pitch has no equivalent
 *       dispatcher-level parameter, so it is interpolated by hand below via {@link Mth#lerp}, the
 *       direct 1.21.1 equivalent of CE's own linear interpolation expression, against {@code
 *       getXRot()}/{@code xRotO} (renamed from CE's {@code rotationPitch}/{@code
 *       prevRotationPitch}).</li>
 *   <li><b>{@code rotate(yaw-90, Y)} then {@code rotate(pitch, Z)}</b> - ported as two {@link
 *       PoseStack#mulPose} calls in the identical call order (Y then Z), which reproduces the same
 *       composed transform CE's GL matrix stack produces (later {@code glRotate}/{@code mulPose}
 *       calls apply closer to the vertex, matching between both APIs) - not reordered.</li>
 *   <li><b>OBJ single-group {@code renderAll()}</b>: unlike the gravity-well family (see {@link
 *       com.hbm.client.render.entity.effect.BlackHoleRenderer}'s own javadoc for why that family
 *       swaps its OBJ sphere for procedural geometry), the MIRV warhead cone genuinely is CE's real
 *       visual - there is no cheap procedural substitute for an arbitrary cone mesh the way there is
 *       for a perfect sphere, so this class depends on this port's real {@link HbmObjModel} pipeline
 *       (confirmed real and committed, {@code docs/phase5/renderer_framework_and_obj_models.md}).
 *       {@link HbmObjModel#get(ResourceLocation)} is called from inside {@link #render} (not an
 *       eager {@code public static final} field) - matching {@code com.hbm.client.render.blockentity.
 *       rbmk.RBMKFuelColumnRenderer}'s own already-established, already-reviewed lazy-load
 *       convention in this port, which exists specifically so a missing {@code .obj} resource throws
 *       only on first actual render (a rare, already-visible failure mode) rather than crashing the
 *       entire {@code EntityRenderDispatcher} construction pass at client-setup time (which an eager
 *       static-field load - {@link HbmObjModel#load} throws unchecked on a missing resource - would
 *       risk for every registered renderer, not just this one). See {@link HbmObjModel}'s own class
 *       javadoc, "Safe to eagerly load from a static field," for why this caution is specific to
 *       resources that may not exist yet rather than a general rule against static fields.</li>
 * </ul>
 *
 * <h2>Asset gap</h2>
 * Neither {@code models/mirv.obj} (single unnamed/{@code "Cone"} group - confirmed by reading CE's
 * real on-disk {@code assets/hbm/models/mirv.obj}, 1 group) nor {@code textures/models/misc/
 * universaldark.png} exist in this port's {@code src/main/resources} yet (confirmed by directory
 * search). CE's own Java source spells the model path {@code "models/Mirv.obj"} (capital M) - this
 * class uses the real lowercase on-disk filename instead, same already-established convention as
 * {@link com.hbm.client.render.entity.effect.CloudFleijaRenderer}'s identical note. Until both files
 * are migrated in, this renderer's first actual render call throws a {@link
 * com.hbm.render.loader.ModelFormatException} (wrapping the underlying missing-resource {@code
 * IOException}) rather than drawing - a real, already-flagged, already-accepted gap for this exact
 * "renderer code correct, raw asset not yet copied" situation elsewhere in this port (see {@link
 * HbmObjModel}'s own javadoc), not something this rendering-scope task is positioned to fix (a
 * separate, dedicated asset-migration pass, out of scope per this task's own ground rules - see
 * this task's structured-output notes).
 */
public class MirvRenderer extends EntityRenderer<EntityMIRV> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/mirv.obj");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/models/misc/universaldark.png");

    private HbmObjModel cachedModel;

    public MirvRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private HbmObjModel model() {
        if (cachedModel == null) cachedModel = HbmObjModel.get(MODEL);
        return cachedModel;
    }

    @Override
    public void render(EntityMIRV entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
        // CE: if (!ClientProxy.renderingConstant) return; - see class javadoc.
        if (!ConstantRenderSweep.isRenderingConstant()) return;

        // CE: prevRotationPitch + (rotationPitch - prevRotationPitch) * partialTicks - entityYaw
        // above is already the dispatcher-interpolated yaw, see class javadoc.
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));

        VertexConsumer consumer = bufferSource.getBuffer(HbmObjModel.renderType(TEXTURE));
        model().renderAll(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMIRV entity) {
        return TEXTURE;
    }
}
