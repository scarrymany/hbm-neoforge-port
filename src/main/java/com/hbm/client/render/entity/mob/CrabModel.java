package com.hbm.client.render.entity.mob;

import com.hbm.entity.mob.EntityCyberCrab;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * Ported from CE's {@code com.hbm.render.model.ModelCrab} ({@code upstream/hbm-ce/.../render/model/
 * ModelCrab.java}, 172 lines, {@code extends ModelBase}, read in full) - 20 plain box-cuboids, zero
 * OBJ dependency, confirmed by {@code docs/phase5/boss_and_vehicle_entity_renderers.md} Headline
 * finding #3/section E as one of only two genuinely vanilla-box-cuboid models in this whole task's
 * scope (the other being {@link HunterChopperModel}). Backs {@link EntityCyberCrab} directly; {@code
 * EntityTaintCrab}/{@code EntityTeslaCrab} extend {@code EntityCyberCrab} in this port but use their
 * own separate OBJ-rigged bodies (see {@link TaintCrabRenderer}/{@link TeslaCrabRenderer}), not this
 * model - CE makes the identical split within one inheritance family (that report's own explicit
 * warning: "do not assume family membership predicts model tech even within a single inheritance
 * chain").
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *   <li><b>{@link net.minecraft.client.model.geom.builders.LayerDefinition}/{@link ModelPart} tree,
 *       not raw {@code ModelRenderer} fields</b> - the modern, stable (since ~1.17) replacement for
 *       CE's {@code ModelRenderer[] modelcrabModel} array; baked once via {@link #createBodyLayer()}
 *       registered against {@link BossModelLayers#CRAB} (see that class's own javadoc for why this
 *       needed its own new file rather than a shared-aggregator edit) and obtained per-renderer-
 *       instance via {@code EntityRendererProvider.Context#bakeLayer}. <b>This exact package/class
 *       shape ({@code MeshDefinition}/{@code PartDefinition}/{@code CubeListBuilder}/{@code
 *       PartPose}) is well-established, long-stable Minecraft-modding knowledge, not independently
 *       jar-verified in this sandbox</b> - flagged per this task's ground rules (no equivalent
 *       custom-box-model renderer exists anywhere in {@code upstream/neo-edition} to cross-check
 *       against, confirmed by that report's own "Open questions/risks" section).</li>
 *   <li><b>The outer {@code renderAll()} wrapper (CE: {@code translate(0,1.5,0); rotate(-90,Y);}
 *       before rendering all 20 boxes)</b> is reproduced as one intermediate {@code "crab"} {@link
 *       PartDefinition} carrying that exact offset/rotation, with all 20 real boxes added as {@code
 *       "crab"}'s own children rather than {@link #root}'s direct children - {@link ModelPart#render}
 *       naturally propagates a parent's transform to every child, reproducing CE's GL matrix-stack
 *       nesting exactly without needing a second manual {@link com.mojang.blaze3d.vertex.PoseStack}
 *       push in {@link #renderToBuffer}.</li>
 *   <li><b>Leg/foot Y-rotation "walk" animation</b> ({@code setRotationAngles}) - CE recomputes each
 *       of the 8 leg/foot parts' {@code rotateAngleY} from scratch every call (an absolute overwrite
 *       driven by {@code limbSwing}/{@code limbSwingAmount}, not an incremental accumulation) -
 *       transcribed 1:1 into {@link #setupAnim} below, including the exact base angles/signs for
 *       each of the 4 leg+foot pairs and the shared {@code f9} swing term.</li>
 *   <li><b>Zero-thickness/degenerate boxes</b> - none in this model (unlike {@link
 *       HunterChopperModel}'s rotor-blade cross-planes) - every box here has positive dimensions.</li>
 * </ul>
 */
public class CrabModel extends EntityModel<EntityCyberCrab> {

    private final ModelPart root;
    private final ModelPart crab;
    private final ModelPart leg6, leg7, leg8, leg9;
    private final ModelPart foot10, foot11, foot12, foot13;

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition crab = root.addOrReplaceChild("crab",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0F, 1.5F, 0F, 0F, -(float) Math.toRadians(90D), 0F));

        crab.addOrReplaceChild("box1", CubeListBuilder.create().texOffs(1, 1).mirror().addBox(0F, 0F, 0F, 4, 1, 4),
                PartPose.offset(-2F, -3F, -2F));
        crab.addOrReplaceChild("box2", CubeListBuilder.create().texOffs(17, 1).mirror().addBox(0F, 0F, 0F, 4, 1, 6),
                PartPose.offset(-2F, -4F, -3F));
        crab.addOrReplaceChild("box3", CubeListBuilder.create().texOffs(33, 1).mirror().addBox(0F, 0F, 0F, 3, 1, 3),
                PartPose.offset(-1.5F, -5F, -1.5F));
        crab.addOrReplaceChild("box4", CubeListBuilder.create().texOffs(49, 1).mirror().addBox(0F, 0F, 0F, 4, 1, 2),
                PartPose.offset(-2F, -4.5F, -1F));
        crab.addOrReplaceChild("box5", CubeListBuilder.create().texOffs(1, 9).mirror().addBox(0F, 0F, 0F, 6, 1, 4),
                PartPose.offset(-3F, -4F, -2F));

        crab.addOrReplaceChild("leg6", CubeListBuilder.create().texOffs(25, 9).mirror().addBox(-0.5F, 0F, 2F, 1, 1, 3),
                PartPose.offsetAndRotation(0F, -3F, 0F, -0.17453293F, 0.78539816F, 0F));
        crab.addOrReplaceChild("leg7", CubeListBuilder.create().texOffs(41, 9).mirror().addBox(-0.5F, 0F, 2F, 1, 1, 3),
                PartPose.offsetAndRotation(0F, -3F, 0F, -0.17453293F, -0.78539816F, 0F));
        crab.addOrReplaceChild("leg8", CubeListBuilder.create().texOffs(1, 17).mirror().addBox(-0.5F, 0F, 2F, 1, 1, 3),
                PartPose.offsetAndRotation(0F, -3F, 0F, -0.17453293F, -2.35619449F, 0F));
        crab.addOrReplaceChild("leg9", CubeListBuilder.create().texOffs(17, 17).mirror().addBox(-0.5F, 0F, 2F, 1, 1, 3),
                PartPose.offsetAndRotation(0F, -3F, 0F, -0.17453293F, 2.35619449F, 0F));

        crab.addOrReplaceChild("foot10", CubeListBuilder.create().texOffs(57, 9).mirror().addBox(-0.5F, 1F, 4F, 1, 3, 1),
                PartPose.offsetAndRotation(0F, -3F, 0F, 0.17453293F, -0.78539816F, 0F));
        crab.addOrReplaceChild("foot11", CubeListBuilder.create().texOffs(33, 17).mirror().addBox(-0.5F, 1F, 4F, 1, 3, 1),
                PartPose.offsetAndRotation(0F, -3F, 0F, 0.17453293F, 0.78539816F, 0F));
        crab.addOrReplaceChild("foot12", CubeListBuilder.create().texOffs(41, 17).mirror().addBox(-0.5F, 1F, 4F, 1, 3, 1),
                PartPose.offsetAndRotation(0F, -3F, 0F, 0.17453293F, -2.35619449F, 0F));
        crab.addOrReplaceChild("foot13", CubeListBuilder.create().texOffs(49, 17).mirror().addBox(-0.5F, 1F, 4F, 1, 3, 1),
                PartPose.offsetAndRotation(0F, -3F, 0F, 0.17453293F, 2.35619449F, 0F));

        crab.addOrReplaceChild("fang14", CubeListBuilder.create().texOffs(17, 1).mirror().addBox(-0.5F, 0F, 1.5F, 1, 1, 1),
                PartPose.offsetAndRotation(0F, -3F, 0F, -0.43633231F, -0.6981317F, 0F));
        crab.addOrReplaceChild("fang15", CubeListBuilder.create().texOffs(33, 9).mirror().addBox(-0.5F, 0F, 1.5F, 1, 1, 1),
                PartPose.offsetAndRotation(0F, -3F, 0F, -0.43633231F, 0.87266463F, 0F));
        crab.addOrReplaceChild("fang16", CubeListBuilder.create().texOffs(49, 9).mirror().addBox(-0.5F, 0F, 1.5F, 1, 1, 1),
                PartPose.offsetAndRotation(0F, -3F, 0F, -0.43633231F, -2.26892803F, 0F));
        crab.addOrReplaceChild("fang17", CubeListBuilder.create().texOffs(9, 17).mirror().addBox(-0.5F, 0F, 1.5F, 1, 1, 1),
                PartPose.offsetAndRotation(0F, -3F, 0F, -0.43633231F, 2.44346095F, 0F));

        crab.addOrReplaceChild("box18", CubeListBuilder.create().texOffs(1, 25).mirror().addBox(0F, 0F, 0F, 2, 1, 4),
                PartPose.offset(-1F, -4.5F, -2F));
        crab.addOrReplaceChild("box19", CubeListBuilder.create().texOffs(17, 25).mirror().addBox(0F, 0F, 0F, 5, 1, 3),
                PartPose.offset(-2.5F, -3.5F, -1.5F));
        crab.addOrReplaceChild("box20", CubeListBuilder.create().texOffs(33, 25).mirror().addBox(0F, 0F, 0F, 3, 1, 5),
                PartPose.offset(-1.5F, -3.5F, -2.5F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    public CrabModel(ModelPart root) {
        // Matches upstream/neo-edition's real, compiling ModelRubble<T> constructor shape at this
        // exact neo_version (RenderType::entityCutoutNoCull - the same "both faces, alpha-cutout"
        // convention this port's own HbmObjModel.renderType(ResourceLocation) already uses for CE's
        // OBJ-mesh entities, chosen here for the identical reason: CE's ModelCrab blanket-sets
        // mirror=true on every box, and no-cull is the safe default against any mirrored-winding
        // edge case rather than risking backface-culled geometry).
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.crab = root.getChild("crab");
        this.leg6 = crab.getChild("leg6");
        this.leg7 = crab.getChild("leg7");
        this.leg8 = crab.getChild("leg8");
        this.leg9 = crab.getChild("leg9");
        this.foot10 = crab.getChild("foot10");
        this.foot11 = crab.getChild("foot11");
        this.foot12 = crab.getChild("foot12");
        this.foot13 = crab.getChild("foot13");
    }

    /** CE: {@code ModelCrab.setRotationAngles} - see class javadoc. */
    @Override
    public void setupAnim(EntityCyberCrab entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                           float netHeadYaw, float headPitch) {
        float f9 = -(Mth.cos(limbSwing * 0.6662F * 2.0F) * 0.4F) * limbSwingAmount;
        f9 *= 1.5F;

        foot11.yRot = 0.78539816F + f9;
        foot10.yRot = -0.78539816F - f9;
        foot12.yRot = -2.35619449F - f9;
        foot13.yRot = 2.35619449F + f9;

        leg6.yRot = foot11.yRot;
        leg7.yRot = foot10.yRot;
        leg8.yRot = foot12.yRot;
        leg9.yRot = foot13.yRot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer,
                                int packedLight, int packedOverlay, int color) {
        root.render(poseStack, consumer, packedLight, packedOverlay, color);
    }

    public ModelPart root() {
        return root;
    }
}
