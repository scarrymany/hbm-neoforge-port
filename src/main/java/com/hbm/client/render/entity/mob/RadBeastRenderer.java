package com.hbm.client.render.entity.mob;

import com.hbm.client.render.misc.BeamPronter;
import com.hbm.entity.mob.EntityRADBeast;
import com.hbm.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.render.entity.mob.RenderRADBeast} (80 lines, {@code extends
 * RenderLiving<EntityRADBeast>}, read in full) - see {@link RadBeastModel}'s own javadoc for the
 * full body-vs-mask fidelity breakdown, and {@code docs/phase5/boss_and_vehicle_entity_renderers.md}
 * section G.
 *
 * <h2>What CE's renderer stack actually draws</h2>
 * <pre>
 * victim = entity.getUnfortunateSoul();
 * if (victim != null) {
 *   tX,tY,tZ = victim position (+ victim.height/2 on Y);
 *   if (victim == Minecraft.player) tY -= 1.5;         // first-person camera-height correction
 *   prontBeam((tX,tY,tZ) - (sx,sy+1.25,sz), RANDOM, SOLID, 0x004000,0x004000, ...);
 * }
 * super.doRender(...);  // RenderLiving: positions/rotates, renders ModelBlaze body...
 * // ...renderModel override, called from inside that same pass:
 * bindTexture(ModelM65Blaze.png); modelM65.render(...);   // the mask, layered on top
 * </pre>
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>{@code getUnfortunateSoul()} -&gt; {@code entity.getTarget()}</b> - this port's {@link
 *       EntityRADBeast} tracks its grab victim via vanilla's own {@code Mob#getTarget()} (a public,
 *       already-available accessor - confirmed by direct read, no entity-side fix needed for this
 *       renderer, unlike {@link UfoRenderer}'s {@code EntityUFO.beam}), not a CE-shaped bespoke
 *       getter - reused directly.</li>
 *   <li><b>{@code Minecraft.getMinecraft().player} -&gt; {@code Minecraft.getInstance().player}</b> -
 *       the 1.21.1 client-singleton accessor rename, well-established.</li>
 *   <li><b>{@code rotate(180-yaw, Y)}</b> - same {@code RenderLiving}-convention reasoning as {@link
 *       MaskManRenderer}/{@link CyberCrabRenderer}.</li>
 *   <li><b>Body vs. mask fidelity</b> - see {@link RadBeastModel}'s own javadoc; the mask is a full
 *       10-box CE transcription, the body is a disclosed simplified placeholder.</li>
 * </ul>
 *
 * <h2>Asset gap</h2>
 * Neither {@code textures/entity/radbeast.png} nor {@code textures/armor/modelm65blaze.png} exist in
 * this port's {@code src/main/resources} yet - same already-flagged, already-accepted gap (no mesh
 * asset needed at all, per {@code docs/phase5/boss_and_vehicle_entity_renderers.md}'s own "Phase-5-
 * safe scope" item #4 - both parts are Java-authored box data).
 */
public class RadBeastRenderer extends EntityRenderer<EntityRADBeast> {

    private static final ResourceLocation BODY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/entity/radbeast.png");
    private static final ResourceLocation MASK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/armor/modelm65blaze.png");

    private final RadBeastModel model;

    public RadBeastRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RadBeastModel(context.bakeLayer(BossModelLayers.RAD_BEAST));
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(EntityRADBeast entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
        LivingEntity victim = entity.getTarget();
        if (victim != null) {
            double sx = 0, sy = 1.25, sz = 0;
            double tX = victim.getX() - entity.getX();
            double tY = victim.getY() + victim.getBbHeight() / 2D - entity.getY() - sy;
            double tZ = victim.getZ() - entity.getZ();

            if (victim == Minecraft.getInstance().player) {
                tY -= 1.5D;
            }

            poseStack.pushPose();
            poseStack.translate(sx, sy, sz);
            BeamPronter.prontBeam(poseStack, new Vec3(tX, tY, tZ), BeamPronter.WaveType.RANDOM,
                    BeamPronter.BeamType.SOLID, 0x004000, 0x004000,
                    (int) (entity.level().getGameTime() % 1000 + 1),
                    (int) (Math.sqrt(tX * tX + tY * tY + tZ * tZ) * 5), 0.125F, 2, 0.03125F);
            poseStack.popPose();
        }

        float limbSwing = entity.walkAnimation.position(partialTick);
        float limbSwingAmount = entity.walkAnimation.speed(partialTick);
        float netHeadYaw = entity.getYHeadRot() - entityYaw;
        float headPitch = entity.getXRot();
        model.setupAnim(entity, limbSwing, limbSwingAmount, entity.tickCount + partialTick, netHeadYaw, headPitch);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));

        VertexConsumer bodyConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(BODY_TEXTURE));
        model.body().render(poseStack, bodyConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.pushPose();
        poseStack.scale(1.125F * 1.01F, 1.125F * 1.01F, 1.125F * 1.01F);
        VertexConsumer maskConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(MASK_TEXTURE));
        model.mask().render(poseStack, maskConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityRADBeast entity) {
        return BODY_TEXTURE;
    }
}
