package com.hbm.client.render.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Ported from CE's {@code render/model/ModelM65.java} (180 lines, {@code upstream/hbm-ce}, read in
 * full for this task) - the plain-{@code ModelBiped} hand-modeled M65 respirator (9 boxes across a
 * {@code mask}/{@code filter} pair, no OBJ - bucket (b) of {@code
 * docs/phase5/armor_humanoidmodel_rendering.md} finding 3). CE shares exactly <b>one</b> {@code
 * ModelM65} class/instance across 5 different items with 5 different textures ({@code
 * gas_mask_m65}, {@code gas_mask_mono}, {@code gas_mask_olde} via {@code ArmorGasMask}; {@code
 * hazmat_helmet_red}, {@code hazmat_helmet_grey} via {@code ArmorHazmatMask} - all 5 confirmed by
 * direct CE source read for this task) - mirrored here as one shared model class taking its
 * texture as a constructor parameter, rather than 5 near-identical subclasses.
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *     <li>Same {@link net.minecraft.client.model.geom.builders.LayerDefinition}-baking approach as
 *     {@link GasMaskArmorModel} - see that class's and {@link ArmorPartLayers}' javadoc for the
 *     shared reasoning. Every box's literal position/size/texture-offset/rotation below is
 *     transcribed 1:1 from CE's real constructor (including its {@code yOffset = 0.5F} per-box
 *     vertical nudge, folded directly into each literal Y coordinate here rather than kept as a
 *     separate runtime addition) - CE's {@code convertToChild} math resolves to a no-op here for
 *     the same reason as {@link GasMaskArmorModel} (neither {@code mask} nor {@code filter} ever
 *     has its own {@code rotationPoint} moved away from the default {@code (0,0,0)}).</li>
 *     <li><b>Live head-pose sync + the {@code d ≈ 1.136} "puff out" scale</b> - identical technique
 *     to {@link GasMaskArmorModel} (see that class's javadoc), just with CE's own {@code d = (1F /
 *     16F) * 18F * 1.01F} scale factor computed as a real constant below rather than CE's literal
 *     magic-number expression, and applied to <i>both</i> {@code mask} and {@code filter} (CE
 *     applies the scale once, around both renders, inside one shared {@code pushMatrix}/{@code
 *     popMatrix} block - reproduced the same way here).</li>
 *     <li><b>Not reproduced</b>: CE's {@code filter.render(...)} is conditional on {@code
 *     ArmorUtil.getGasMaskFilterRecursively(...)} finding an installed filter in the wearer's own
 *     head slot - this class always draws {@code filter}, a deliberate, documented simplification
 *     (avoids threading the equipped {@code ItemStack} through {@link ArmorModelBase}'s shared
 *     {@code getPropertiesFrom} contract, which no other leaf in this package needs) rather than an
 *     oversight. A future pass wanting exact parity should thread the stack through and gate {@link
 *     #filter}'s render the same way CE does.</li>
 * </ul>
 */
public final class M65ArmorModel extends ArmorModelBase {

    /** CE: {@code (1F / 16F) * 18F * 1.01F} - see class javadoc. */
    private static final float SCALE = (1F / 16F) * 18F * 1.01F;

    private static volatile ModelPart bakedMask;
    private static volatile ModelPart bakedFilter;

    private final ResourceLocation texture;
    private final ModelPart mask;
    private final ModelPart filter;

    /**
     * @param texture this concrete item's real CE texture (e.g. {@code gas_mask_m65.png}) - see
     *                class javadoc for why this is a constructor parameter, not a constant.
     */
    public M65ArmorModel(EquipmentSlot slot, ResourceLocation texture) {
        super(slot);
        this.texture = texture;
        bake();
        this.mask = bakedMask;
        this.filter = bakedFilter;
    }

    private static void bake() {
        if (bakedMask == null) {
            // See ArmorPartLayers' class javadoc for why this lazy Minecraft.getInstance() bake
            // (rather than an EntityRendererProvider.Context#bakeLayer call) is used here, and its
            // confirmed-vs-well-established-knowledge caveat.
            ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(ArmorPartLayers.M65_MASK);
            bakedMask = root.getChild("mask");
            bakedFilter = root.getChild("filter");
        }
    }

    /** CE: {@code ModelM65}'s constructor - see class javadoc for the coordinate-space note. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition mask = root.addOrReplaceChild("mask", CubeListBuilder.create(), PartPose.ZERO);
        mask.addOrReplaceChild("maskHead",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0F, 0F, 0F, 8, 8, 8),
                PartPose.offset(-4F, -7.5F, -4F));
        mask.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(0F, 0F, 0F, 3, 3, 1),
                PartPose.offset(-1.5F, -3.0F, -5F));
        mask.addOrReplaceChild("outlet",
                CubeListBuilder.create().texOffs(0, 20).mirror().addBox(0F, -2F, 0F, 2, 2, 1),
                PartPose.offsetAndRotation(-1F, -3.0F, -5F, -0.4799655F, 0F, 0F));
        mask.addOrReplaceChild("noseSlope",
                CubeListBuilder.create().texOffs(8, 16).mirror().addBox(0F, 0F, -2F, 3, 2, 2),
                PartPose.offsetAndRotation(-1.5F, -1.5F, -4F, 0.6108652F, 0F, 0F));
        mask.addOrReplaceChild("eye1",
                CubeListBuilder.create().texOffs(0, 23).mirror().addBox(0F, 0F, 0F, 3, 3, 0),
                PartPose.offset(-3.5F, -5.5F, -4.2F));
        mask.addOrReplaceChild("eye2",
                CubeListBuilder.create().texOffs(0, 26).mirror().addBox(0F, 0F, 0F, 3, 3, 0),
                PartPose.offset(0.5F, -5.5F, -4.2F));
        mask.addOrReplaceChild("iForgot",
                CubeListBuilder.create().texOffs(6, 20).mirror().addBox(0F, 0F, 0F, 2, 2, 1),
                PartPose.offset(-1F, -2.7F, -6F));

        PartDefinition filter = root.addOrReplaceChild("filter", CubeListBuilder.create(), PartPose.ZERO);
        filter.addOrReplaceChild("filterConnector",
                CubeListBuilder.create().texOffs(6, 23).mirror().addBox(0F, 0F, -3F, 2, 2, 1),
                PartPose.offsetAndRotation(-1F, -1.5F, -4F, 0.6108652F, 0F, 0F));
        filter.addOrReplaceChild("filter1",
                CubeListBuilder.create().texOffs(18, 21).mirror().addBox(0F, -1F, -5F, 3, 4, 2),
                PartPose.offsetAndRotation(-1.5F, -1.5F, -4F, 0.6108652F, 0F, 0F));
        filter.addOrReplaceChild("filter2",
                CubeListBuilder.create().texOffs(18, 16).mirror().addBox(0F, -0.5F, -5F, 4, 3, 2),
                PartPose.offsetAndRotation(-2F, -1.5F, -4F, 0.6108652F, 0F, 0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public RenderType renderType(ResourceLocation vanillaResolvedLocation) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    protected void renderArmorPiece(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
                                     int color, EquipmentSlot slot) {
        if (slot != EquipmentSlot.HEAD || original == null) return;

        poseStack.pushPose();
        applyPartPose(poseStack, original.head);
        poseStack.scale(SCALE, SCALE, SCALE);
        mask.render(poseStack, consumer, packedLight, packedOverlay, color);
        filter.render(poseStack, consumer, packedLight, packedOverlay, color);
        poseStack.popPose();
    }
}
