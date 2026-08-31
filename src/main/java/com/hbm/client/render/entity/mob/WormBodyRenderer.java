package com.hbm.client.render.entity.mob;

import com.hbm.entity.mob.EntityBOTPrimeBody;
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
 * Ported from CE's {@code com.hbm.render.entity.RenderWormBody} ({@code upstream/hbm-ce/.../render/
 * entity/RenderWormBody.java}, 50 lines, read in full) - {@link EntityBOTPrimeBody}'s renderer, one
 * of up to 74 simultaneous body-segment instances per worm boss per {@code docs/phase5/
 * boss_and_vehicle_entity_renderers.md} section A. Structurally identical to {@link
 * WormHeadRenderer} (see that class's own javadoc for the full 1.21.1 translation notes, not
 * repeated here) - a single unnamed-group OBJ mesh (a separate mesh/texture from the head), rotation
 * only, no other live per-frame state.
 *
 * <h2>Asset gap</h2>
 * Neither {@code models/mobs/bot_prime_body.obj} nor {@code textures/entity/mark_zero_body.png}
 * exist in this port's {@code src/main/resources} yet - see {@link WormHeadRenderer}'s own class
 * javadoc "Asset gap" note for the identical, already-accepted situation.
 *
 * <h2>Open performance question (not resolved here)</h2>
 * {@code docs/phase5/boss_and_vehicle_entity_renderers.md}'s own "Open questions/risks" section
 * flags up to 74 simultaneous {@link EntityBOTPrimeBody} instances (plus 1 head) each independently
 * issuing its own OBJ {@code renderAll()} draw call every frame as a real, untested performance
 * question for this port - re-flagged here rather than silently assumed fine, matching that report's
 * own framing.
 */
public class WormBodyRenderer extends EntityRenderer<EntityBOTPrimeBody> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/mobs/bot_prime_body.obj");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/entity/mark_zero_body.png");

    private HbmObjModel cachedModel;

    public WormBodyRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private HbmObjModel model() {
        if (cachedModel == null) cachedModel = HbmObjModel.get(MODEL);
        return cachedModel;
    }

    @Override
    public void render(EntityBOTPrimeBody entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch - 90.0F));

        VertexConsumer consumer = bufferSource.getBuffer(HbmObjModel.renderType(TEXTURE));
        model().renderAll(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityBOTPrimeBody entity) {
        return TEXTURE;
    }
}
