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

import com.hbm.main.MainRegistry;

/**
 * Ported from CE's {@code render/model/ModelGasMask.java} (138 lines, {@code upstream/hbm-ce},
 * read in full for this task) - the plain-{@code ModelBiped} hand-modeled gas mask (CE's own
 * comment: 6 boxes, no OBJ at all - bucket (b) of {@code
 * docs/phase5/armor_humanoidmodel_rendering.md} finding 3). Backs the {@code gas_mask} item only
 * ({@code com.hbm.items.gear.ArmorGasMask.ModelKind#GAS_MASK}; {@code gas_mask_m65}/{@code
 * _mono}/{@code _olde} use the separate, more detailed {@link M65ArmorModel} in CE, confirmed by
 * CE's own {@code ArmorGasMask#getArmorModel}'s {@code this == ModItems.gas_mask} identity check).
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *     <li>CE's {@code ModelRenderer} box tree (one {@code mask} parent, 6 {@code Shape1..6}
 *     children, built via CE's own {@code convertToChild} helper) is baked as a {@link
 *     net.minecraft.client.model.geom.builders.LayerDefinition} instead - see {@link
 *     ArmorPartLayers} for why armor Path A leaves need this extra baking step at all. Every box's
 *     literal position/size/texture-offset below is transcribed 1:1 from CE's real constructor
 *     (not guessed) - CE's own {@code convertToChild} math resolves to a no-op here because CE
 *     never repositions the {@code mask} parent's own {@code rotationPoint} away from its default
 *     {@code (0,0,0)}, so every child's stored absolute coordinates are already correct without
 *     any additional translation.</li>
 *     <li><b>Live head-pose sync</b>: CE's {@code RenderModelSyncUtil.copyAngles(source.bipedHead,
 *     this.mask)} (copies the live vanilla head's rotation onto the mask group every frame) is
 *     reproduced via {@link ArmorModelBase#applyPartPose(PoseStack, ModelPart)} applied to {@link
 *     #original}{@code .head} before drawing - the exact idiom {@link ArmorModelBase}'s own class
 *     javadoc prescribes for this situation. Every box below is authored in the same local
 *     coordinate space as vanilla's own head box (e.g. {@code Shape1} sits at
 *     {@code (-4,-7.9625,-4)}, essentially the standard {@code (-4,-8,-4)} head-box corner), which
 *     is exactly why this works without any extra manual offset.</li>
 *     <li><b>The {@code 1.15F} "puff out" scale</b> ({@code GlStateManager.scale(1.15F,1.15F,1.15F)}
 *     around the mask's own {@code rotationPoint}, which - per the point above - is {@code (0,0,0)},
 *     i.e. scaling about the head's own pivot) is reproduced as a plain {@link PoseStack#scale}
 *     call after the pose-copy push, no translate-then-scale-then-translate-back dance needed
 *     (that dance existed only to scale about a nonzero pivot in CE's fixed-function stack; here
 *     the pivot is already at the local origin once {@link ArmorModelBase#applyPartPose} has been
 *     applied).</li>
 *     <li>The {@code isChild}/sneak-offset branch in CE's own {@code render(...)} is not reproduced
 *     - that offset is already applied once, upstream, by the vanilla armor-layer's own {@link
 *     PoseStack} setup before any per-piece {@code renderToBuffer} call (confirmed by every other
 *     leaf in this package following the identical omission, e.g. {@link ObjArmorModel}/{@link
 *     HevArmorModel} never re-apply it either).</li>
 * </ul>
 */
public final class GasMaskArmorModel extends ArmorModelBase {

    /**
     * CE: {@code ArmorGasMask#getArmorTexture} for {@code gas_mask}, real literal path {@code
     * "hbm:textures/armor/GasMask.png"} - normalized to this port's lowercase/snake_case asset
     * convention (see e.g. {@code GunModels}'s own "Resource paths" section) since CE's own
     * {@code ResourceManager} never carried a canonical entry for this item's texture (only
     * {@code ArmorGasMask}'s hardcoded string did).
     */
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/armor/gas_mask.png");

    private static volatile ModelPart bakedRoot;

    private final ModelPart mask;

    public GasMaskArmorModel(EquipmentSlot slot) {
        super(slot);
        this.mask = bakedRoot().getChild("mask");
    }

    private static ModelPart bakedRoot() {
        ModelPart root = bakedRoot;
        if (root == null) {
            // See ArmorPartLayers' class javadoc for why this lazy Minecraft.getInstance() bake
            // (rather than an EntityRendererProvider.Context#bakeLayer call) is used here, and its
            // confirmed-vs-well-established-knowledge caveat.
            root = Minecraft.getInstance().getEntityModels().bakeLayer(ArmorPartLayers.GAS_MASK);
            bakedRoot = root;
        }
        return root;
    }

    /** CE: {@code ModelGasMask}'s constructor - see class javadoc for the coordinate-space note. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition mask = root.addOrReplaceChild("mask", CubeListBuilder.create(), PartPose.ZERO);

        mask.addOrReplaceChild("shape1",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0F, 0F, 0F, 8, 8, 3),
                PartPose.offset(-4F, -7.9625F, -4F));
        mask.addOrReplaceChild("shape2",
                CubeListBuilder.create().texOffs(22, 0).mirror().addBox(0F, 0F, 0F, 2, 2, 1),
                PartPose.offset(-3F, -4.9625F, -4.5333334F));
        mask.addOrReplaceChild("shape3",
                CubeListBuilder.create().texOffs(22, 0).mirror().addBox(0F, 0F, 0F, 2, 2, 1),
                PartPose.offset(1F, -4.9625F, -4.5F));
        mask.addOrReplaceChild("shape4",
                CubeListBuilder.create().texOffs(0, 11).mirror().addBox(0F, 0F, 0F, 2, 2, 2),
                PartPose.offsetAndRotation(-1F, -2.9625F, -4F, -0.7853982F, 0F, 0F));
        mask.addOrReplaceChild("shape5",
                CubeListBuilder.create().texOffs(0, 15).mirror().addBox(0F, 2F, -0.5F, 3, 4, 3),
                PartPose.offsetAndRotation(-1.5F, -2.9625F, -4F, -0.7853982F, 0F, 0F));
        mask.addOrReplaceChild("shape6",
                CubeListBuilder.create().texOffs(0, 22).mirror().addBox(0F, 0F, 0F, 8, 1, 5),
                PartPose.offset(-4F, -4.9625F, -1F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public RenderType renderType(ResourceLocation vanillaResolvedLocation) {
        return RenderType.entityCutoutNoCull(TEXTURE);
    }

    @Override
    protected void renderArmorPiece(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
                                     int color, EquipmentSlot slot) {
        if (slot != EquipmentSlot.HEAD || original == null) return;

        poseStack.pushPose();
        applyPartPose(poseStack, original.head);
        poseStack.scale(1.15F, 1.15F, 1.15F);
        mask.render(poseStack, consumer, packedLight, packedOverlay, color);
        poseStack.popPose();
    }
}
