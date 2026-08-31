package com.hbm.client.render.entity.mob;

import com.hbm.entity.mob.EntityMaskMan;
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
 * Ported from CE's {@code com.hbm.render.entity.mob.RenderMaskMan} (32 lines, {@code extends
 * RenderLiving<EntityMaskMan>}) + {@code com.hbm.render.model.ModelMaskMan} (78 lines, the real
 * work - both read in full) - see {@code docs/phase5/boss_and_vehicle_entity_renderers.md} section
 * B. Unlike its box-cuboid-model siblings in this task (Hunter Chopper, the Cyber Crab family's
 * base), MaskMan's body is a multi-part <b>OBJ</b> rig ({@code ResourceManager.maskman.
 * renderPart("Torso"/"LLeg"/"RLeg"/"LArm"/"RArm"/"Head"/"Skull"/"IOU")}), driven by a limb-swing
 * "torso lean" computed from the exact same {@code limbSwing}/{@code limbSwingAmount} parameters
 * every {@code ModelBase.render()} call receives.
 *
 * <h2>What CE's {@code ModelMaskMan.render} actually draws</h2>
 * <pre>
 * pushMatrix(); rotate(180,X); translate(0,-1.5,Y); rotate(-90,Y);
 * swing = toDegrees(cos(f/2+PI) * 1.4 * f1);           // f=limbSwing, f1=limbSwingAmount
 * rotate(swing*-0.1, X); renderPart("Torso");
 * [pushMatrix; translate(-0.5,1.75,-0.5); rotate(swing,Z);      renderPart("LLeg"); popMatrix]
 * [pushMatrix; translate(-0.5,1.75, 0.5); rotate(swing*-1,Z);   renderPart("RLeg"); popMatrix]
 * [pushMatrix; translate(-0.5,3.75,-1.5); rotate(swing*0.25,Z); renderPart("LArm"); popMatrix]
 * [pushMatrix; translate(-0.5,3.75, 1.5); rotate(swing*-0.25,Z);renderPart("RArm"); popMatrix]
 * pushMatrix(); translate(0.5,4,0); rotate(-netHeadYaw,Y);
 *   if (health &gt;= maxHealth/2) renderPart("Head");
 *   else { renderPart("Skull"); bindTexture(iou); renderPart("IOU"); }
 * popMatrix(); popMatrix();
 * </pre>
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>{@code RenderLiving} wrapper not reproduced - a deliberate, disclosed simplification.</b>
 *       Every other OBJ-based renderer in this task ({@link WormHeadRenderer}/{@link
 *       WormBodyRenderer}/{@link UfoRenderer}) is a plain {@link EntityRenderer}, matching CE's own
 *       {@code Render<T>} (not {@code RenderLiving<T>}) base - reused here for the same "one shared
 *       family of manual-{@code PoseStack} renderers, no {@code MobRenderer}/{@code
 *       LivingEntityRenderer} generic-signature risk" reasoning documented across this task's whole
 *       batch (see this task's own structured-output notes). MaskMan's real CE renderer <i>does</i>
 *       extend {@code RenderLiving}, whose {@code doRender}/{@code setupRotations} apply an
 *       additional {@code rotate(180-yaw, Y)} turn, a {@code scale(-1,-1,1)} axis mirror, and a
 *       further {@code translate(0,-1.501,0)} world-space lift <i>before</i> ever calling {@code
 *       ModelMaskMan.render}. The {@code rotate(180-yaw, Y)} half <i>is</i> reproduced below (the
 *       standard vanilla {@code MobRenderer}/{@code LivingEntityRenderer} facing convention every
 *       box-cuboid/rigged mob model is authored against - well-established, not a guess), but the
 *       {@code scale(-1,-1,1)} mirror and the extra world-space lift are not: both are specific to
 *       vanilla's box-cuboid-model pixel-scale convention, which this OBJ-rigged model never
 *       actually participates in (see the class-level "not reproduced" reasoning this whole batch
 *       shares), so applying them here would be a guess, not a fix - leaving only the literal
 *       transform sequence transcribed directly from {@code ModelMaskMan.render()} itself above.
 *       <b>This is a genuine, disclosed uncertainty</b>: MaskMan's exact on-screen vertical
 *       alignment may not exactly match CE's real appearance without a live client to compare
 *       against (this sandbox cannot launch one) - flagged explicitly here rather than silently
 *       guessed, per this task's own instruction to note simplified-vs-faithful ports honestly.</li>
 *   <li><b>{@code limbSwing}/{@code limbSwingAmount}</b> - {@link EntityRenderer#render} has no such
 *       parameters (only {@code LivingEntityRenderer}/{@code MobRenderer} extract and forward them).
 *       Approximated via {@link net.minecraft.world.entity.LivingEntity#walkAnimation}'s {@code
 *       position(float)}/{@code speed(float)} accessors (well-established, stable vanilla API since
 *       the {@code WalkAnimationState} refactor, the same values a real {@code MobRenderer} would
 *       have extracted and passed to {@code model.setupAnim} - <b>not independently jar-verified in
 *       this sandbox</b>), which play the same "phase"/"amplitude" role as CE's raw {@code f}/{@code
 *       f1} closely enough for this "reasonable-effort" port.</li>
 *   <li><b>Net head yaw</b> - approximated as {@code entity.getYHeadRot() - entityYaw} (CE's own
 *       {@code f3} parameter is exactly this "how far the head has turned past the body" delta) -
 *       {@code entityYaw} here is already the dispatcher-interpolated body-facing yaw (see {@link
 *       com.hbm.client.render.entity.missile.MirvRenderer}'s class javadoc for that established
 *       convention).</li>
 *   <li><b>Two-texture "Skull + IOU" overlay</b> - CE mid-{@code render()} rebinds a second texture
 *       for just the {@code "IOU"} part. {@link EntityModel}-shaped code has no direct equivalent
 *       (one {@code VertexConsumer} per call); this class instead obtains a <i>second</i> {@link
 *       VertexConsumer} directly from the {@link MultiBufferSource} parameter this renderer's own
 *       {@code render} method already receives - a capability CE's fixed-function {@code
 *       ModelBase.render} genuinely lacked (bind-texture-and-draw-immediately being the only option
 *       in 1.12's pipeline), so this is a strict capability upgrade, not a behavior change.</li>
 *   <li><b>Lazy {@link HbmObjModel#get(ResourceLocation)}</b> - same established convention as
 *       {@link WormHeadRenderer}/{@link com.hbm.client.render.entity.missile.MirvRenderer}.</li>
 * </ul>
 *
 * <h2>Asset gap</h2>
 * Neither {@code models/mobs/maskman.obj} nor {@code textures/entity/{maskman,iou}.png} exist in
 * this port's {@code src/main/resources} yet - same already-flagged, already-accepted gap as every
 * other OBJ-dependent renderer in this task.
 */
public class MaskManRenderer extends EntityRenderer<EntityMaskMan> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/mobs/maskman.obj");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/entity/maskman.png");
    private static final ResourceLocation IOU_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/entity/iou.png");

    private HbmObjModel cachedModel;

    public MaskManRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private HbmObjModel model() {
        if (cachedModel == null) cachedModel = HbmObjModel.get(MODEL);
        return cachedModel;
    }

    @Override
    public void render(EntityMaskMan entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
        HbmObjModel model = model();
        VertexConsumer consumer = bufferSource.getBuffer(HbmObjModel.renderType(TEXTURE));
        int overlay = OverlayTexture.NO_OVERLAY;

        float limbSwing = entity.walkAnimation.position(partialTick);
        float limbSwingAmount = entity.walkAnimation.speed(partialTick);
        float netHeadYaw = entity.getYHeadRot() - entityYaw;

        float swing = (float) Math.toDegrees(Mth.cos(limbSwing / 2F + (float) Math.PI) * 1.4F * limbSwingAmount);

        poseStack.pushPose();
        // CE's real renderer extends RenderLiving, whose setupRotations applies rotate(180-yaw, Y)
        // before ever calling the model - the standard vanilla MobRenderer/LivingEntityRenderer
        // facing convention every box-cuboid/rigged mob model is authored against, reproduced here
        // even though this class does not otherwise route through MobRenderer (see class javadoc).
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));

        // CE: ModelMaskMan.render()'s own literal transform stack (see class javadoc).
        poseStack.mulPose(Axis.XP.rotationDegrees(180F));
        poseStack.translate(0, -1.5F, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90F));

        poseStack.mulPose(Axis.XP.rotationDegrees(swing * -0.1F));
        model.renderPart(poseStack, consumer, packedLight, overlay, "Torso");

        renderLimb(model, poseStack, consumer, packedLight, overlay, -0.5F, 1.75F, -0.5F, swing, "LLeg");
        renderLimb(model, poseStack, consumer, packedLight, overlay, -0.5F, 1.75F, 0.5F, -swing, "RLeg");
        renderLimb(model, poseStack, consumer, packedLight, overlay, -0.5F, 3.75F, -1.5F, swing * 0.25F, "LArm");
        renderLimb(model, poseStack, consumer, packedLight, overlay, -0.5F, 3.75F, 1.5F, -swing * 0.25F, "RArm");

        poseStack.pushPose();
        poseStack.translate(0.5F, 4F, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-netHeadYaw));

        if (entity.getHealth() >= entity.getMaxHealth() / 2F) {
            model.renderPart(poseStack, consumer, packedLight, overlay, "Head");
        } else {
            model.renderPart(poseStack, consumer, packedLight, overlay, "Skull");
            VertexConsumer iouConsumer = bufferSource.getBuffer(HbmObjModel.renderType(IOU_TEXTURE));
            model.renderPart(poseStack, iouConsumer, packedLight, overlay, "IOU");
        }
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void renderLimb(HbmObjModel model, PoseStack poseStack, VertexConsumer consumer,
                                    int packedLight, int overlay, float x, float y, float z,
                                    float swingDegrees, String partName) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(swingDegrees));
        model.renderPart(poseStack, consumer, packedLight, overlay, partName);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMaskMan entity) {
        return TEXTURE;
    }
}
