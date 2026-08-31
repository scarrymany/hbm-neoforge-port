package com.hbm.client.render.entity.mob;

import com.hbm.entity.mob.EntityTaintCrab;
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
 * Ported from CE's {@code com.hbm.render.entity.mob.RenderTaintCrab} (54 lines, {@code extends
 * RenderLiving<EntityTaintCrab>}) + {@code com.hbm.render.model.ModelTaintCrab} (36 lines, the body
 * transform - both read in full) - see {@code docs/phase5/boss_and_vehicle_entity_renderers.md}
 * section E's own correction: despite sharing a base class ({@link EntityTaintCrab} {@code extends
 * EntityCyberCrab} in this port too), TaintCrab's body is an <b>OBJ</b> rig ({@code
 * ResourceManager.taintcrab.renderPart("Body"/"Legs1"/"Legs2")}), not {@link CrabModel}'s vanilla
 * box-cuboids.
 *
 * <h2>What CE's renderer stack actually draws</h2>
 * <pre>
 * // RenderTaintCrab.doRender (electric tether, see "Tether not reproduced" below):
 * for (target : entity.targets) prontBeam(target - self, RANDOM, SOLID, 0x0051C4, 0x606060, ...);
 * // ModelTaintCrab.render (the body itself):
 * pushMatrix(); rotate(90, 0,-1,0); rotate(180, Z); translate(0,-1.5,0);
 * rot = -(cos(f*0.6662*2+0)*0.4)*f1*57.3;                    // f=limbSwing, f1=limbSwingAmount
 * renderPart("Body");
 * pushMatrix(); rotate(rot, Y);  renderPart("Legs1"); popMatrix();
 * pushMatrix(); rotate(rot, -Y); renderPart("Legs2"); popMatrix();
 * popMatrix();
 * </pre>
 *
 * <h2>Tether not reproduced - a real, upstream (not this task's) scope cut</h2>
 * CE's {@code RenderTaintCrab.doRender} iterates {@code ((EntityTaintCrab) entity).targets} (a
 * {@code List<double[]>} of nearby zap-target coordinates) drawing a {@link
 * com.hbm.client.render.misc.BeamPronter} tether toward each one. This port's own {@link
 * EntityTaintCrab} has <b>no such list</b> - confirmed by direct read of that class's own javadoc
 * ("Tesla-arc zap not reproduced... {@code TileEntityTesla.zap(...)} not reproduced") - the entire
 * server-side targeting mechanic this visual depends on was a deliberate Phase 4 scope cut, not a
 * gap this rendering-scope task should independently re-invent (see this task's own ground rule 8:
 * "do not fix or refactor files clearly owned by a different task"). The body itself renders fully;
 * only the electric-tether overlay is absent, a direct, honest consequence of the already-documented
 * upstream cut, not a new one.
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>{@code rotate(180-yaw, Y)}</b> - same {@code RenderLiving}-convention reasoning as {@link
 *       MaskManRenderer}/{@link CyberCrabRenderer}.</li>
 *   <li><b>{@code limbSwing}/{@code limbSwingAmount}</b> - same {@code walkAnimation} approximation
 *       as {@link MaskManRenderer}.</li>
 *   <li><b>Lazy {@link HbmObjModel#get(ResourceLocation)}</b> - same established convention as this
 *       task's other OBJ-based renderers.</li>
 * </ul>
 *
 * <h2>Asset gap</h2>
 * Neither {@code models/mobs/taintcrab.obj} nor {@code textures/entity/taintcrab.png} exist in this
 * port's {@code src/main/resources} yet - same already-flagged, already-accepted gap.
 */
public class TaintCrabRenderer extends EntityRenderer<EntityTaintCrab> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/mobs/taintcrab.obj");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/entity/taintcrab.png");

    private HbmObjModel cachedModel;

    public TaintCrabRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private HbmObjModel model() {
        if (cachedModel == null) cachedModel = HbmObjModel.get(MODEL);
        return cachedModel;
    }

    @Override
    public void render(EntityTaintCrab entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
        float limbSwing = entity.walkAnimation.position(partialTick);
        float limbSwingAmount = entity.walkAnimation.speed(partialTick);
        float rot = -(Mth.cos(limbSwing * 0.6662F * 2.0F) * 0.4F) * limbSwingAmount * 57.3F;

        HbmObjModel model = model();
        VertexConsumer consumer = bufferSource.getBuffer(HbmObjModel.renderType(TEXTURE));
        int overlay = OverlayTexture.NO_OVERLAY;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));

        poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180F));
        poseStack.translate(0, -1.5F, 0);

        model.renderPart(poseStack, consumer, packedLight, overlay, "Body");

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));
        model.renderPart(poseStack, consumer, packedLight, overlay, "Legs1");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-rot));
        model.renderPart(poseStack, consumer, packedLight, overlay, "Legs2");
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTaintCrab entity) {
        return TEXTURE;
    }
}
