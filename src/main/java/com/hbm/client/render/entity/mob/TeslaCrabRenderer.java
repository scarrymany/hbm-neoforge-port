package com.hbm.client.render.entity.mob;

import com.hbm.entity.mob.EntityTeslaCrab;
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
 * Ported from CE's {@code com.hbm.render.entity.mob.RenderTeslaCrab} (54 lines, {@code extends
 * RenderLiving<EntityTeslaCrab>}) + {@code com.hbm.render.model.ModelTeslaCrab} (37 lines - both
 * read in full) - see {@code docs/phase5/boss_and_vehicle_entity_renderers.md} section E. Nearly
 * identical in shape to {@link TaintCrabRenderer} (same OBJ-rigged body technology, same swing math,
 * same missing-tether situation - see that class's own javadoc for the full explanation of both, not
 * repeated here); the two differences CE's real source has are: no extra {@code rotate(90,0,-1,0)}
 * Y-turn before the body transform, and the two swinging OBJ parts are named {@code "Front"}/{@code
 * "Back"} rather than {@code "Legs1"}/{@code "Legs2"}.
 *
 * <h2>What CE's {@code ModelTeslaCrab.render} actually draws</h2>
 * <pre>
 * pushMatrix(); rotate(180, Z); translate(0,-1.5,0);
 * rot = -(cos(f*0.6662*2+0)*0.4)*f1*57.3;
 * renderPart("Body");
 * pushMatrix(); rotate(rot, Y);  renderPart("Front"); popMatrix();
 * pushMatrix(); rotate(rot, -Y); renderPart("Back");  popMatrix();
 * popMatrix();
 * </pre>
 *
 * <h2>Asset gap</h2>
 * Neither {@code models/mobs/teslacrab.obj} nor {@code textures/entity/teslacrab.png} exist in this
 * port's {@code src/main/resources} yet - same already-flagged, already-accepted gap.
 */
public class TeslaCrabRenderer extends EntityRenderer<EntityTeslaCrab> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/mobs/teslacrab.obj");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/entity/teslacrab.png");

    private HbmObjModel cachedModel;

    public TeslaCrabRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private HbmObjModel model() {
        if (cachedModel == null) cachedModel = HbmObjModel.get(MODEL);
        return cachedModel;
    }

    @Override
    public void render(EntityTeslaCrab entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
        float limbSwing = entity.walkAnimation.position(partialTick);
        float limbSwingAmount = entity.walkAnimation.speed(partialTick);
        float rot = -(Mth.cos(limbSwing * 0.6662F * 2.0F) * 0.4F) * limbSwingAmount * 57.3F;

        HbmObjModel model = model();
        VertexConsumer consumer = bufferSource.getBuffer(HbmObjModel.renderType(TEXTURE));
        int overlay = OverlayTexture.NO_OVERLAY;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));

        poseStack.mulPose(Axis.ZP.rotationDegrees(180F));
        poseStack.translate(0, -1.5F, 0);

        model.renderPart(poseStack, consumer, packedLight, overlay, "Body");

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));
        model.renderPart(poseStack, consumer, packedLight, overlay, "Front");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-rot));
        model.renderPart(poseStack, consumer, packedLight, overlay, "Back");
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTeslaCrab entity) {
        return TEXTURE;
    }
}
