package com.hbm.client.render.entity.mob;

import com.hbm.entity.mob.EntityBOTPrimeHead;
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
 * Ported from CE's {@code com.hbm.render.entity.RenderWormHead} ({@code upstream/hbm-ce/.../render/
 * entity/RenderWormHead.java}, 51 lines, read in full) - {@link EntityBOTPrimeHead}'s renderer, the
 * head segment of the "worm" boss per {@code docs/phase5/boss_and_vehicle_entity_renderers.md}
 * section A. A single unnamed-group OBJ mesh, no live per-frame state beyond rotation.
 *
 * <h2>What CE's {@code doRender} actually draws</h2>
 * <pre>
 * pushMatrix(); translate(x,y,z);
 * rotate(prevYaw + (yaw-prevYaw)*partialTicks - 90, 0,1,0);
 * rotate(prevPitch + (pitch-prevPitch)*partialTicks - 90, 0,0,1);
 * bindTexture(mark_zero_head.png); shadeModel(SMOOTH); disableCull();
 * body.renderAll();                 // bot_prime_head.obj, single unnamed group
 * enableCull(); shadeModel(FLAT); popMatrix();
 * </pre>
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>{@code disableCull()}/{@code enableCull()}</b> - not reproduced as an explicit {@code
 *       RenderSystem} call: {@link HbmObjModel#renderType(ResourceLocation)} already returns {@code
 *       RenderType.entityCutoutNoCull(texture)} (see that method's own javadoc), which draws both
 *       faces unconditionally - the same net effect as CE's manual disable/enable pair, with no
 *       explicit toggle needed here.</li>
 *   <li><b>{@code shadeModel(GL_SMOOTH)}</b> - a fixed-function per-vertex-color interpolation mode
 *       toggle with no 1.21.1 equivalent (the modern pipeline always interpolates vertex
 *       attributes/lighting smoothly across a triangle - "flat shading" would require an explicit
 *       flat-interpolation qualifier in a custom shader, which this renderer has no reason to add).
 *       Not reproduced - CE's own default (untinted OBJ mesh, {@code GL_FLAT} the rest of the time)
 *       has no visible difference from always-smooth for an untinted mesh with no per-vertex color
 *       variation.</li>
 *   <li><b>Interpolated yaw/pitch</b> - {@code entityYaw} is already the dispatcher-interpolated yaw
 *       (see {@link com.hbm.client.render.entity.missile.MirvRenderer}'s own class javadoc for the
 *       full explanation of this convention, reused here); pitch is interpolated by hand via {@link
 *       Mth#lerp} against {@code getXRot()}/{@code xRotO} (CE: {@code rotationPitch}/{@code
 *       prevRotationPitch}).</li>
 *   <li><b>Lazy {@link HbmObjModel#get(ResourceLocation)}</b> - same already-established convention
 *       {@link com.hbm.client.render.entity.missile.MirvRenderer} uses (see that class's own javadoc
 *       for why: a missing {@code .obj} resource throws only on first actual render, not at
 *       client-setup time).</li>
 * </ul>
 *
 * <h2>Asset gap</h2>
 * Neither {@code models/mobs/bot_prime_head.obj} nor {@code textures/entity/mark_zero_head.png}
 * exist in this port's {@code src/main/resources} yet (confirmed by directory search against CE's
 * real on-disk asset tree) - same already-flagged, already-accepted "renderer code correct, raw
 * asset not yet copied" gap {@link com.hbm.client.render.entity.missile.MirvRenderer}'s own javadoc
 * documents, not this rendering-scope task's job to fix (a separate asset-migration pass).
 */
public class WormHeadRenderer extends EntityRenderer<EntityBOTPrimeHead> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/mobs/bot_prime_head.obj");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/entity/mark_zero_head.png");

    private HbmObjModel cachedModel;

    public WormHeadRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private HbmObjModel model() {
        if (cachedModel == null) cachedModel = HbmObjModel.get(MODEL);
        return cachedModel;
    }

    @Override
    public void render(EntityBOTPrimeHead entity, float entityYaw, float partialTick, PoseStack poseStack,
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
    public ResourceLocation getTextureLocation(EntityBOTPrimeHead entity) {
        return TEXTURE;
    }
}
