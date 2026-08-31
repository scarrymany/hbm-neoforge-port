package com.hbm.client.render.entity.effect;

import com.hbm.entity.effect.EntityEMPBlast;
import com.hbm.main.MainRegistry;
import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Ported from CE's {@code com.hbm.render.entity.RenderEMPBlast} (54 lines, read in full) - the
 * expanding-ring renderer for {@link EntityEMPBlast}: one flat ring mesh, scaled on X/Z only
 * (never Y) by {@code entity.scale + partialTicks}, textured with {@code EMPBlast.png}, drawn
 * full-bright. CE's own {@code doRender}:
 * <pre>
 * pushMatrix(); [snapshot lighting/cull state]; translate(x,y,z);
 * if (wasLit) disableLighting(); if (wasCulled) disableCull();
 * scale(entity.scale + partialTicks, 1F, entity.scale + partialTicks);
 * bindTexture(ringTexture); ringModel.renderAll();
 * [restore cull/lighting]; popMatrix();
 * </pre>
 * (CE snapshots/restores the caller's lighting+cull GL state rather than unconditionally
 * toggling it, via its own {@code RenderUtil.isLightingEnabled()}/{@code isCullEnabled()} helpers -
 * a defensive nicety with no 1.21.1 equivalent to preserve: this class's {@link
 * net.minecraft.client.renderer.RenderType}-based draw call is fully self-contained per {@link
 * #render} invocation and never leaks GL state across entities the way CE's raw
 * {@code GlStateManager} calls could).
 *
 * <h2>1.21.1 translation notes</h2>
 * See {@link CloudFleijaRenderer}'s class javadoc for the shared "no explicit translate/pushMatrix,
 * no fixed-function lighting toggle, {@code getPackedLight} is the correct full-bright hook,
 * {@code entityCutoutNoCull}'s double-sided default is a deliberate no-op simplification of CE's
 * single-sided {@code enableCull()}" reasoning - identical here, not re-derived. The X/Z-only,
 * Y-locked-at-1 scale is preserved exactly (CE's own 3-argument {@code GlStateManager.scale} call,
 * not a uniform scale - this ring stays flat/thin as it expands outward, unlike the two sphere
 * clouds).
 *
 * <h2>Asset gap</h2>
 * Neither {@code models/ring.obj} nor {@code textures/models/explosion/empblast.png} exist in this
 * port's resources yet - see {@link CloudFleijaRenderer}'s class javadoc for the identical,
 * already-flagged gap (lowercase real CE filenames used here for the same reason).
 */
public class EmpBlastRenderer extends EntityRenderer<EntityEMPBlast> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/ring.obj");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/models/explosion/empblast.png");

    public EmpBlastRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityEMPBlast entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource buffer, int packedLight) {
        float s = entity.scale + partialTick;

        poseStack.pushPose();
        // CE: GlStateManager.scale(entity.scale + partialTicks, 1F, entity.scale + partialTicks) -
        // X/Z only, Y left at 1 (a flat expanding ring, not a growing sphere).
        poseStack.scale(s, 1F, s);

        VertexConsumer consumer = buffer.getBuffer(HbmObjModel.renderType(TEXTURE));
        HbmObjModel.get(MODEL).renderAll(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }

    /** CE: {@code EntityEMPBlast.getBrightnessForRender() -> 15728880} - see {@link CloudFleijaRenderer} javadoc. */
    @Override
    public int getPackedLight(EntityEMPBlast entity, float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityEMPBlast entity) {
        return TEXTURE;
    }
}
