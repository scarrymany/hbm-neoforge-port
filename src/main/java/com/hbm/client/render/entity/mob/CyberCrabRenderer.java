package com.hbm.client.render.entity.mob;

import com.hbm.entity.mob.EntityCyberCrab;
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

/**
 * Ported from CE's {@code com.hbm.render.entity.RenderCyberCrab} (29 lines, {@code extends
 * RenderLiving<EntityCyberCrab>}, read in full) - see {@code docs/phase5/
 * boss_and_vehicle_entity_renderers.md} section E. The simplest renderer in this task's whole batch:
 * CE's class body is just a constructor (wiring {@link CrabModel}'s CE ancestor + the {@code crab.png}
 * texture) and a texture-location override - {@code doRender} is never overridden at all, i.e. CE
 * relies entirely on {@code RenderLiving}'s own default body-position/rotation/shadow handling.
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>Plain {@link EntityRenderer}, not {@code MobRenderer}</b> - a deliberate choice shared
 *       across this whole task's batch (see {@link MaskManRenderer}'s own javadoc for the full
 *       reasoning: avoiding {@code MobRenderer}/{@code LivingEntityRenderer}'s generic-signature
 *       uncertainty at this exact {@code neo_version}), independently re-confirmed safe here by
 *       {@code upstream/neo-edition}'s own real, compiling {@code RenderRubble}/{@code ModelRubble}
 *       pair (cross-checked strictly for API shape, not behavior - see {@link CrabModel}'s own
 *       javadoc), which is exactly this same "plain {@code EntityRenderer} manually drives a
 *       box-cuboid {@code EntityModel}" pattern with a custom (non-{@code Mob}) entity. This class
 *       therefore reproduces the standard vanilla {@code MobRenderer} body-positioning convention by
 *       hand: {@code rotate(180-yaw, Y)} (see {@link MaskManRenderer}'s javadoc for why this is the
 *       correct, well-established choice for a CE {@code RenderLiving}-based renderer, not a guess).</li>
 *   <li><b>{@code limbSwing}/{@code limbSwingAmount}</b> - approximated via {@link
 *       net.minecraft.world.entity.LivingEntity#walkAnimation}, same already-established convention
 *       as {@link MaskManRenderer}.</li>
 * </ul>
 *
 * <h2>Asset gap</h2>
 * {@code textures/entity/crab.png} does not exist in this port's {@code src/main/resources} yet -
 * same already-flagged, already-accepted gap as this task's other CE-texture-dependent renderers.
 */
public class CyberCrabRenderer extends EntityRenderer<EntityCyberCrab> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/entity/crab.png");

    private final CrabModel model;

    public CyberCrabRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CrabModel(context.bakeLayer(BossModelLayers.CRAB));
        this.shadowRadius = 1.0F;
    }

    @Override
    public void render(EntityCyberCrab entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));

        float limbSwing = entity.walkAnimation.position(partialTick);
        float limbSwingAmount = entity.walkAnimation.speed(partialTick);
        float netHeadYaw = entity.getYHeadRot() - entityYaw;
        float headPitch = entity.getXRot();
        float ageInTicks = entity.tickCount + partialTick;

        model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityCyberCrab entity) {
        return TEXTURE;
    }
}
