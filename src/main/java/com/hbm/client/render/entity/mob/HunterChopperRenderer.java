package com.hbm.client.render.entity.mob;

import com.hbm.entity.mob.EntityHunterChopper;
import com.hbm.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Ported from CE's {@code com.hbm.render.entity.RenderHunterChopper} (56 lines, {@code extends
 * Render<EntityHunterChopper>}, read in full) - see {@code docs/phase5/
 * boss_and_vehicle_entity_renderers.md} section D and {@link HunterChopperModel}'s own javadoc for
 * the full box-cuboid model this class positions.
 *
 * <h2>What CE's {@code doRender} actually draws</h2>
 * <pre>
 * translate(x,y,z);
 * translate(0, 0.0625*32, 0);        // 2.0 blocks
 * translate(0, 0.0625*12, 0);        // 0.75 blocks - CE writes these as two separate calls
 * scale(4,4,4);
 * rotate(180, X);
 * rotate(prevYaw + (yaw-prevYaw)*partialTicks - 90, Y);
 * rotate(prevPitch + (pitch-prevPitch)*partialTicks, Z);
 * bindTexture(chopper.png);
 * // rocket instanceof EntityHunterChopper -&gt; mine2.setGunRotations(...) is commented out in CE's
 * // real source (CE disabling its own turret-tracking feature) - not ported, matches CE exactly.
 * mine2.renderAll(0.0625F);
 * </pre>
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>{@code mine2.renderAll(0.0625F)}'s scale parameter has no direct equivalent to pass</b> -
 *       {@link ModelPart#render}(no scale parameter) already bakes the classic "1 unit = 1/16 block"
 *       pixel convention directly into the {@link net.minecraft.client.model.geom.builders.
 *       LayerDefinition} at bake time (confirmed by {@code upstream/neo-edition}'s own {@code
 *       ModelRubble}/{@code RenderRubble} pair - see {@link CrabModel}'s javadoc - which bakes and
 *       renders box coordinates in this exact same numeric convention with no per-call scale
 *       argument anywhere), so {@link ModelPart#render} alone already reproduces the net visual
 *       scale CE's {@code part.render(0.0625F)} calls produced - only CE's <i>additional</i> {@code
 *       scale(4,4,4)} (on top of that inherent pixel conversion) needs to be reproduced explicitly
 *       below, which it is.</li>
 *   <li><b>One shared {@link HunterChopperModel} instance, not one per entity</b> - matches CE's own
 *       {@code ModelHunterChopper mine2} field (constructed once in {@code RenderHunterChopper}'s
 *       constructor, reused for every {@link EntityHunterChopper} instance this one renderer draws) -
 *       the rotor-spin increments in {@link HunterChopperModel#setupAnim} are therefore shared/
 *       global across every on-screen chopper, exactly matching CE's real behavior (not per-instance
 *       state), including the fact that a single chopper's rotor spin rate visually depends on how
 *       many total choppers are being drawn onscreen this frame in the pathological case (CE's own
 *       real quirk, preserved rather than "fixed" - see {@link HunterChopperModel}'s own javadoc).</li>
 *   <li><b>Interpolated yaw/pitch</b> - same established convention as this task's other {@code
 *       Render<T>}-shaped renderers ({@link WormHeadRenderer}/{@link
 *       com.hbm.client.render.entity.missile.MirvRenderer}): {@code entityYaw} is already the
 *       dispatcher-interpolated yaw, pitch is interpolated by hand via {@link Mth#lerp} against
 *       {@code getXRot()}/{@code xRotO}.</li>
 * </ul>
 *
 * <h2>Asset gap</h2>
 * {@code textures/entity/chopper.png} does not exist in this port's {@code src/main/resources} yet -
 * same already-flagged, already-accepted gap as this task's other CE-texture-dependent renderers
 * (unlike this task's OBJ-dependent renderers, this one needs no mesh asset at all - only the one
 * texture PNG - since the body is baked directly from Java-authored box data, per {@code
 * docs/phase5/boss_and_vehicle_entity_renderers.md}'s own "Phase-5-safe scope" item #3).
 */
public class HunterChopperRenderer extends EntityRenderer<EntityHunterChopper> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/entity/chopper.png");

    private final HunterChopperModel model;

    public HunterChopperRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new HunterChopperModel(context.bakeLayer(BossModelLayers.HUNTER_CHOPPER));
        this.shadowRadius = 0F;
    }

    @Override
    public void render(EntityHunterChopper entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        poseStack.pushPose();
        poseStack.translate(0, 0.0625 * 32, 0);
        poseStack.translate(0, 0.0625 * 12, 0);
        poseStack.scale(4F, 4F, 4F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180F));
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));

        model.setupAnim(entity, 0F, 0F, entity.tickCount + partialTick, 0F, 0F);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityHunterChopper entity) {
        return TEXTURE;
    }
}
