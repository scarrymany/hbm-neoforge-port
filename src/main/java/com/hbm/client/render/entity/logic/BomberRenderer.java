package com.hbm.client.render.entity.logic;

import com.hbm.client.render.ConstantRenderSweep;
import com.hbm.entity.logic.EntityBomber;
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
 * Ported from CE's {@code com.hbm.render.entity.RenderBomber} (85 lines, {@code extends
 * Render<EntityBomber>}, read in full) - see {@code docs/phase5/
 * boss_and_vehicle_entity_renderers.md} section J. Two alternate full airframes selected by the
 * synced {@code STYLE} byte (see {@link EntityBomber#getStyle()}'s own javadoc for the small,
 * necessary server-side fix this task made alongside this renderer): styles 0-4 render a small
 * Dornier-style plane (5x scale, 5 texture variants), styles 5-8 a much larger B-29-style plane
 * (~9.68x scale, 4 texture variants) - a real, deliberate "two visually distinct plane families
 * sharing one entity class" CE design, not a simple recolor.
 *
 * <h2>What CE's {@code doRender} actually draws</h2>
 * <pre>
 * if (!ClientProxy.renderingConstant) return;
 * translate(x,y,z);
 * rotate(prevYaw + (yaw-prevYaw)*partialTicks - 90, Y);
 * rotate(90, Z);
 * rotate(prevPitch + (pitch-prevPitch)*partialTicks, Z);
 * enableLighting(); disableCull();
 * style = entity.getDataManager().get(STYLE);
 * bindTexture([dornier_0..4 | b29_0..3][style]);
 * switch(style) {
 *   0-4: scale(5,5,5); rotate(-90, Y); dornier.renderAll(); break;
 *   5-8: scale(30/3.1, 30/3.1, 30/3.1); rotate(180, Y); b29.renderAll(); break;
 * }
 * enableCull();
 * </pre>
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>{@code IConstantRenderer} gate</b>: same {@code ConstantRenderSweep} mechanism this
 *       task's other {@code IConstantRenderer} renderers use - see {@link
 *       com.hbm.client.render.entity.effect.BlackHoleRenderer}'s own javadoc for the full
 *       mechanism. {@link EntityBomber} already {@code implements IConstantRenderer} as of this
 *       task's own small entity-side fix (see that class's javadoc).</li>
 *   <li><b>Interpolated yaw/pitch</b> - same established convention as this task's other {@code
 *       Render<T>}-shaped renderers.</li>
 *   <li><b>Two lazily-loaded {@link HbmObjModel}s</b> (Dornier and B29) - same established lazy-load
 *       convention as {@link com.hbm.client.render.entity.missile.MirvRenderer}, just two
 *       independent cached fields instead of one.</li>
 * </ul>
 *
 * <h2>Asset gap</h2>
 * Neither {@code models/{dornier,b29}.obj} nor any of the 9 texture PNGs ({@code textures/models/
 * planes/{dornier_0..4,b29_0..3}.png}) exist in this port's {@code src/main/resources} yet - same
 * already-flagged, already-accepted gap as this task's other OBJ-dependent renderers.
 */
public class BomberRenderer extends EntityRenderer<EntityBomber> {

    private static final ResourceLocation DORNIER_MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/dornier.obj");
    private static final ResourceLocation B29_MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/b29.obj");

    private static final ResourceLocation[] DORNIER_TEXTURES = {
            tex("dornier_0"), tex("dornier_1"), tex("dornier_2"), tex("dornier_3"), tex("dornier_4")
    };
    private static final ResourceLocation[] B29_TEXTURES = {
            tex("b29_0"), tex("b29_1"), tex("b29_2"), tex("b29_3")
    };

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/models/planes/" + name + ".png");
    }

    private HbmObjModel dornierModel;
    private HbmObjModel b29Model;

    public BomberRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private HbmObjModel dornier() {
        if (dornierModel == null) dornierModel = HbmObjModel.get(DORNIER_MODEL);
        return dornierModel;
    }

    private HbmObjModel b29() {
        if (b29Model == null) b29Model = HbmObjModel.get(B29_MODEL);
        return b29Model;
    }

    @Override
    public void render(EntityBomber entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
        if (!ConstantRenderSweep.isRenderingConstant()) return;

        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));

        int style = entity.getStyle();
        ResourceLocation texture = style >= 0 && style <= 4
                ? DORNIER_TEXTURES[style]
                : (style >= 5 && style <= 8 ? B29_TEXTURES[style - 5] : DORNIER_TEXTURES[1]);

        VertexConsumer consumer = bufferSource.getBuffer(HbmObjModel.renderType(texture));

        if (style >= 5 && style <= 8) {
            poseStack.scale(30F / 3.1F, 30F / 3.1F, 30F / 3.1F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180F));
            b29().renderAll(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        } else {
            poseStack.scale(5F, 5F, 5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
            dornier().renderAll(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        }

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityBomber entity) {
        return DORNIER_TEXTURES[1];
    }
}
