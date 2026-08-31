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
 * Ported from CE's {@code render/model/ModelJetPack.java} (161 lines, {@code upstream/hbm-ce},
 * read in full for this task) - the plain-{@code ModelBiped} hand-modeled jetpack worn-body rig (8
 * boxes, one parent-child hierarchy, no OBJ - bucket (b) of {@code
 * docs/phase5/armor_humanoidmodel_rendering.md} finding 3). This is {@code JetpackBase}'s <b>Path
 * A</b> standalone-worn model (CE: {@code JetpackBase#getArmorModel}, lazily
 * {@code new ModelJetPack()}-caching exactly like every other leaf in this package) - <b>not</b>
 * {@code JetpackGlider}'s separate Collada "activate" animation (blocked, see {@code
 * docs/phase5/armor_humanoidmodel_rendering.md} Deferred scope item 1) or the mod-slot-insert
 * {@code LayerArmorMod}/"Path B" render (explicitly out of this task's scope, same report's finding
 * 1).
 *
 * <h2>Currently has no live caller - documented, not a bug</h2>
 * This model class is real, complete, and correctly registered against all 5 {@code
 * com.hbm.items.gear.JetpackItems} entries by {@link ArmorRenderRegistry#registerJetpacks}, exactly
 * like every powered-armor set in this package. It will not actually be seen in-game yet: CE's own
 * jetpacks are plain, non-{@code ArmorItem} items made wearable via Forge 1.12's {@code
 * isValidArmor}/{@code getArmorModel} hooks on any {@code Item}, and this port's own {@code
 * com.hbm.items.armor.JetpackBase} already documents (see that class's own javadoc, "Not ported")
 * that the confirmed 1.21.1 replacement - a {@code DataComponents.EQUIPPABLE}/{@code Equippable}
 * builder call at the item's registration site - has not been added yet (a real, already-named,
 * cross-cutting open blocker per {@code docs/phase5/armor_humanoidmodel_rendering.md} Deferred
 * scope item 2, not something this specific model class can resolve). {@code
 * IClientItemExtensions#getGenericArmorModel} is simply never invoked for an item that can never be
 * placed into an equipment slot in the first place. Registering this model now (rather than
 * skipping it) matches this task's own brief: "every custom armor item at least has a real,
 * correctly-registered custom model class" - the moment {@code Equippable} lands on {@code
 * JetpackBase}'s {@code Item.Properties}, standalone jetpack wear renders correctly with zero
 * further change to this class or its registration.
 *
 * <h2>1.21.1 translation notes</h2>
 * <ul>
 *     <li>Same {@link net.minecraft.client.model.geom.builders.LayerDefinition}-baking approach as
 *     {@link GasMaskArmorModel}/{@link M65ArmorModel} - see {@link ArmorPartLayers}'s javadoc for
 *     the shared reasoning.</li>
 *     <li><b>{@code convertToChild} is not a no-op here</b> (unlike the two mask models): CE's
 *     {@code JetPack} parent starts at {@code rotationPoint (0,0,-2)} at construction time, and
 *     every one of the 8 child boxes' literal {@code setRotationPoint} calls in CE's constructor
 *     is relative to the <i>original, pre-{@code render()}</i> entity-local space - {@code
 *     convertToChild} then subtracts that {@code (0,0,-2)} from each child, so this class's box
 *     offsets below are each CE's literal constructor value with {@code +2} folded into Z (e.g.
 *     CE's {@code Tank1.setRotationPoint(0.5F, 2F, 0.5F)} becomes {@code (0.5, 2, 2.5)} here) -
 *     computed once by hand for this task, not guessed.</li>
 *     <li><b>The {@code JetPack} parent's own final position is the vanilla body's pose, verbatim
 *     </b>: CE's {@code setRotationAngles} unconditionally overwrites {@code JetPack}'s {@code
 *     rotationPoint}/most of its {@code rotateAngle} from {@code this.bipedBody} every single frame
 *     before render (so the {@code (0,0,-2)} construction-time value is always dead by render
 *     time) - reproduced via {@link ArmorModelBase#applyPartPose(PoseStack, ModelPart)} applied to
 *     {@link #original}{@code .body}, exactly like {@link ObjArmorModel}'s equivalent per-part pose
 *     sync. <b>Not reproduced</b>: CE's extra {@code + Math.toRadians(netHeadYaw)} Y-rotation nudge
 *     on top of the body's own rotation (a CE-specific "jetpack yaws slightly with the head" touch)
 *     - {@code netHeadYaw} is not available at this call site (armor-layer {@code renderToBuffer}
 *     receives no such parameter, unlike CE's {@code ModelBiped#render}), so this is a deliberate,
 *     documented simplification, not an oversight.</li>
 * </ul>
 */
public final class JetpackWornModel extends ArmorModelBase {

    /** CE: {@code ResourceManager.jetpack_tex}, {@code "textures/armor/jetpack_anim.png"}. */
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/armor/jetpack_anim.png");

    private static volatile ModelPart bakedRoot;

    private final ModelPart jetPack;

    public JetpackWornModel(EquipmentSlot slot) {
        // Real 1.21.1 Model#renderType(ResourceLocation) is final (see ArmorModelBase's two-arg
        // constructor javadoc) - this texture is fixed per-class, so the mapping function below
        // ignores vanilla's own resolved location and always answers TEXTURE, replacing what used
        // to be this class's own `renderType(ResourceLocation)` override.
        super(slot, rl -> RenderType.entityCutoutNoCull(TEXTURE));
        this.jetPack = bakedRoot().getChild("jetpack");
    }

    private static ModelPart bakedRoot() {
        ModelPart root = bakedRoot;
        if (root == null) {
            // See ArmorPartLayers' class javadoc for why this lazy Minecraft.getInstance() bake
            // (rather than an EntityRendererProvider.Context#bakeLayer call) is used here, and its
            // confirmed-vs-well-established-knowledge caveat.
            root = Minecraft.getInstance().getEntityModels().bakeLayer(ArmorPartLayers.JETPACK_WORN);
            bakedRoot = root;
        }
        return root;
    }

    /** CE: {@code ModelJetPack}'s constructor - see class javadoc for the coordinate-space note. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition jetpack = root.addOrReplaceChild("jetpack", CubeListBuilder.create(), PartPose.ZERO);

        jetpack.addOrReplaceChild("pack",
                CubeListBuilder.create().texOffs(12, 10).mirror().addBox(0F, 0F, 0F, 4, 6, 1),
                PartPose.offset(-2F, 3F, 2F));
        jetpack.addOrReplaceChild("tank1",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0F, 0F, 0F, 3, 8, 3),
                PartPose.offset(0.5F, 2F, 2.5F));
        jetpack.addOrReplaceChild("tank2",
                CubeListBuilder.create().texOffs(0, 11).mirror().addBox(0F, 0F, 0F, 3, 8, 3),
                PartPose.offset(-3.5F, 2F, 2.5F));
        jetpack.addOrReplaceChild("tip1",
                CubeListBuilder.create().texOffs(0, 22).mirror().addBox(0F, 0F, 0F, 2, 1, 2),
                PartPose.offset(1F, 1F, 3F));
        jetpack.addOrReplaceChild("tip2",
                CubeListBuilder.create().texOffs(0, 25).mirror().addBox(0F, 0F, 0F, 2, 1, 2),
                PartPose.offset(-3F, 1F, 3F));
        jetpack.addOrReplaceChild("duct1",
                CubeListBuilder.create().texOffs(8, 22).mirror().addBox(0F, 0F, 0F, 2, 1, 2),
                PartPose.offset(1F, 9.5F, 3F));
        jetpack.addOrReplaceChild("duct2",
                CubeListBuilder.create().texOffs(8, 25).mirror().addBox(0F, 0F, 0F, 2, 1, 2),
                PartPose.offset(-3F, 9.5F, 3F));
        jetpack.addOrReplaceChild("thruster1",
                CubeListBuilder.create().texOffs(12, 0).mirror().addBox(0F, 0F, 0F, 3, 2, 3),
                PartPose.offset(0.5F, 10.5F, 2.5F));
        jetpack.addOrReplaceChild("thruster2",
                CubeListBuilder.create().texOffs(12, 5).mirror().addBox(0F, 0F, 0F, 3, 2, 3),
                PartPose.offset(-3.5F, 10.5F, 2.5F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    protected void renderArmorPiece(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
                                     int color, EquipmentSlot slot) {
        if (slot != EquipmentSlot.CHEST || original == null) return;

        poseStack.pushPose();
        applyPartPose(poseStack, original.body);
        jetPack.render(poseStack, consumer, packedLight, packedOverlay, color);
        poseStack.popPose();
    }
}
