package com.hbm.client.render.armor;

import java.util.function.Function;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Reusable base for every custom "standalone worn armor piece has its own 3D shape" client model
 * this port writes - the confirmed real 1.21.1 successor to CE's abstract {@code ModelBiped}
 * subclass {@code render/model/ModelArmorBase.java} (183 lines, {@code upstream/hbm-ce}), which
 * every one of CE's concrete OBJ-driven power-armor models ({@code ModelArmorHEV}, {@code
 * ModelArmorAJR(O)}, {@code ModelArmorBJ}, {@code ModelArmorT51}, ... - see {@code
 * docs/phase5/armor_humanoidmodel_rendering.md} finding 3's full bucket-(a) census) and every
 * hand-modeled leaf ({@code ModelGasMask}, {@code ModelJetPack} - bucket (b)) extends.
 *
 * <h2>API shape - confirmed, not guessed</h2>
 * This is CE's <b>"Path A"</b> mechanism only (a standalone-worn item's own model) - CE's {@code
 * ItemArmor#getArmorModel(EntityLivingBase, ItemStack, EntityEquipmentSlot, ModelBiped)}. The
 * confirmed real 1.21.1 hook is {@code
 * net.neoforged.neoforge.client.extensions.common.IClientItemExtensions#getGenericArmorModel(
 * LivingEntity, ItemStack, EquipmentSlot, HumanoidModel)}, returning a plain {@code
 * net.minecraft.client.model.Model} (<b>not</b> itself a {@link HumanoidModel} subclass - the
 * vanilla model being replaced is handed in as the {@code original} parameter, matching this exact
 * class's {@link #getPropertiesFrom} field). This is not a naming guess: it is the exact,
 * already-compiling shape {@code upstream/neo-edition}'s {@code
 * render/model/armor/ModelArmorBase.java} (112 lines) and {@code items/armor/ArmorNo9.java} (77
 * lines) both use at this exact {@code neo_version=21.1.228}, and this port's own already-committed
 * {@code com.hbm.items.gear.ArmorModel}/{@code ArmorGasMask}/{@code ArmorHazmatMask} already
 * declare the identical override (with a {@code // TODO(Phase 5)} body this class exists to let a
 * future leaf fill in). See {@code docs/phase5/armor_humanoidmodel_rendering.md} ("Confirmed real
 * NeoForge 1.21.1 API shapes" / Path A) for the full citation trail.
 *
 * <p>This class is deliberately <b>not</b> a line-for-line port of Neo Edition's own {@code
 * ModelArmorBase}: Neo Edition wraps every part in its own {@code com.hbm.render.loader.
 * ModelRendererObj} (a per-part mutable position/rotation object, copied-from the live vanilla
 * {@link HumanoidModel} field by field every frame) and draws through its own {@code RenderContext}
 * static GL-state helper - both are Neo Edition's own added infrastructure, not present in CE, and
 * not required by CE's actual behavior. This port's own OBJ primitive for the same job is {@code
 * com.hbm.render.loader.HbmObjModel} (already committed this wave by the sibling {@code
 * docs/phase5/renderer_framework_and_obj_models.md} task), which takes the idiomatic modern-MC
 * approach instead: it submits geometry straight into the caller-supplied {@link VertexConsumer}
 * under whatever {@link PoseStack} transform is already on top of the stack, with no side-channel
 * GL state of its own. This class follows that same idiom - a leaf's {@link
 * #renderArmorPiece(PoseStack, VertexConsumer, int, int, int, EquipmentSlot)} override receives the
 * real {@link PoseStack}/{@link VertexConsumer}/packed-light/packed-overlay {@link #renderToBuffer}
 * was itself called with, and is expected to push/pop its own transforms and call straight into
 * {@code HbmObjModel.get(resourceLocation).renderOnly(poseStack, consumer, packedLight,
 * packedOverlay, "Head", ...)} (or {@code renderPart}/{@code renderAllExcept}/{@code renderAll})
 * for its OBJ-driven parts, rather than maintaining a parallel tree of per-part wrapper objects.
 *
 * <h2>Per-slot dispatch, matching CE's own {@code type} int</h2>
 * CE's {@code ModelArmorHEV} (64 lines, representative bucket-(a) leaf, read in full for this task)
 * dispatches on a constructor-supplied {@code type} int (0=helmet, 1=chest+arms, 2=legs,
 * 3=boots) inside one {@code renderArmor(Entity, float)} override. This class's {@link #slot}
 * field is the direct, more type-safe replacement for that {@code type} int - {@link EquipmentSlot}
 * is already the value {@link #getGenericArmorModel}'s real 1.21.1 signature hands every
 * implementation, so there is no reason to reintroduce a parallel int enum.
 *
 * <h2>Live-pose sync</h2>
 * {@link #getPropertiesFrom} is the direct successor to CE's {@code ModelArmorBase.
 * copyPropertiesFromBiped(ModelBiped)} / Neo Edition's {@code ModelArmorBase.getPropertiesFrom} -
 * call it once per {@code getGenericArmorModel} invocation (every frame the piece is visible)
 * before returning {@code this} so {@link #original}/{@link #livingEntity} reflect the current
 * frame's pose (walk-cycle arm swing, head look angle, crouch/baby-scale flags, etc. - all live on
 * the vanilla {@link HumanoidModel} fields {@code head}/{@code body}/{@code rightArm}/{@code
 * leftArm}/{@code rightLeg}/{@code leftLeg}/{@code crouching}/{@code young}, confirmed public and
 * directly readable by Neo Edition's own compiling {@code ModelArmorBase}). A leaf wanting a
 * custom part (e.g. an OBJ helmet mesh) to inherit the vanilla head's live look-angle rotation
 * should call {@link #applyPartPose(PoseStack, ModelPart)} with {@code original.head} before
 * drawing that part - the modern {@link PoseStack}-based equivalent of CE/Neo Edition's per-part
 * {@code copyRotationFrom} field-copy trick (this port's ground rules already flag CE's raw GL
 * matrix-stack tricks as needing a {@link PoseStack}-based redesign, not a 1:1 port - see {@code
 * items/armor/ItemArmorMod.java}'s own javadoc for the identical reasoning applied to a sibling
 * mechanism). {@link ModelPart#translateAndRotate(PoseStack)} is well-established public
 * Minecraft/NeoForge modeling API (stable across many versions) but is <b>not</b> demonstrated by a
 * compiling call site in either reference tree in this pass - flagged, not silently assumed;
 * cross-check against a real jar/javadoc before depending on it for a real (non-placeholder) leaf.
 *
 * <h2>Scope note</h2>
 * This class only covers Path A. CE's other two armor-adjacent render mechanisms - the mod-slot
 * ("chip") insert dispatcher ({@code LayerArmorMod}, "Path B") and {@code IArmorDisableModel}'s
 * body-part-hiding listener - are explicitly out of this task's scope per its own brief (it names
 * only {@code getGenericArmorModel}/{@code RegisterClientExtensionsEvent}) and per {@code
 * docs/phase5/armor_humanoidmodel_rendering.md}'s own three-mechanism split; see that report's
 * "Phase-5-safe scope" items 1/5 for what a future task should build for those two.
 */
public abstract class ArmorModelBase extends Model {

    /**
     * The {@link EquipmentSlot} this instance was constructed to render (CE's {@code type} int
     * equivalent - see class javadoc). Fixed for the lifetime of the instance: {@link
     * ArmorRenderRegistry}'s registration helper caches one instance per slot, never reuses one
     * instance across slots.
     */
    protected final EquipmentSlot slot;

    /**
     * The live vanilla {@link HumanoidModel} being replaced this frame, re-assigned every {@link
     * #getPropertiesFrom} call. Null only before the very first render call. A leaf reads this for
     * pose data (see class javadoc's "Live-pose sync" section) - it is never itself mutated here.
     */
    @Nullable
    protected HumanoidModel<? extends LivingEntity> original;

    /**
     * The entity wearing this piece this frame, re-assigned every {@link #getPropertiesFrom} call
     * exactly like {@code IClientItemExtensions#getGenericArmorModel}'s own {@code living}
     * parameter (which may itself be an {@code ArmorStand}, not only a {@code Player} - CE's own
     * {@code EntityLivingBase} signature carries the same breadth). Null only before the first call.
     */
    @Nullable
    protected LivingEntity livingEntity;

    /**
     * @param slot the equipment slot this instance renders - see {@link #slot}.
     */
    protected ArmorModelBase(EquipmentSlot slot) {
        this(slot, RenderType::entityCutoutNoCull);
    }

    /**
     * @param slot       the equipment slot this instance renders - see {@link #slot}.
     * @param renderType this instance's texture -> {@link RenderType} function, forwarded straight
     *                   to {@link Model}'s own constructor. <b>This is the only real customization
     *                   point left</b>: real 1.21.1 {@link Model#renderType(ResourceLocation)} is
     *                   {@code final} (confirmed directly by the real javac diagnostic this
     *                   constructor overload exists to fix - {@code
     *                   docs/phase6/BUILD_ERRORS.md} cluster {@code fc8-armor-rendertype-final}) -
     *                   a leaf can no longer override that method to pick a per-instance texture the
     *                   way {@link GasMaskArmorModel}/{@link JetpackWornModel}/{@link M65ArmorModel}/
     *                   {@link ObjArmorModel} each need to. A leaf wanting non-default behavior must
     *                   instead build its own {@code ResourceLocation -> RenderType} lambda and pass
     *                   it here, exactly like each of those four leaves' constructors now do -
     *                   ordinary Java constructor-argument scoping (evaluated before {@code super()}
     *                   runs) lets such a lambda close over the leaf's own constructor parameters
     *                   (e.g. {@link M65ArmorModel}'s {@code texture} parameter) or {@code static}
     *                   fields (e.g. {@link GasMaskArmorModel#TEXTURE}) without ever needing to
     *                   capture {@code this} before the supertype constructor has run.
     */
    protected ArmorModelBase(EquipmentSlot slot, Function<ResourceLocation, RenderType> renderType) {
        // Model's constructor wants a texture -> RenderType function (used by generic model-render
        // call sites that ask a Model for its own RenderType via #renderType(ResourceLocation), not
        // by the armor-layer render path itself - that path resolves its RenderType independently
        // from the item's own vanilla armor-texture resolution and hands this class's
        // #renderToBuffer a VertexConsumer already bound to it). The single-arg constructor's
        // entityCutoutNoCull default matches CE's own choice of cutout, double-sided-disabled
        // rendering for its OBJ armor parts (RenderFloodlight and this area's own HbmObjModel.
        // renderType(ResourceLocation) default agree - see that method's javadoc) and is a safe
        // default for a leaf that needs no per-instance texture. Confirmed real, compiling
        // constructor shape: upstream/neo-edition's ModelArmorBase.java, `super(RenderType::
        // entityCutoutNoCull);`.
        super(renderType);
        this.slot = slot;
    }

    /** @return the {@link EquipmentSlot} this instance renders - see {@link #slot}. */
    public EquipmentSlot getSlot() {
        return slot;
    }

    /**
     * Re-syncs this instance to the current frame's live pose - see class javadoc's "Live-pose
     * sync" section. {@link ArmorRenderRegistry}'s shared registration helper calls this once per
     * {@code getGenericArmorModel} invocation, immediately before returning the cached instance;
     * a leaf class should not need to call this itself.
     */
    public void getPropertiesFrom(HumanoidModel<? extends LivingEntity> original, @Nullable LivingEntity livingEntity) {
        this.original = original;
        this.livingEntity = livingEntity;
    }

    /**
     * Pushes {@code part}'s current translation/rotation onto {@code poseStack} - see class
     * javadoc's "Live-pose sync" section for the citation/caveat on {@link
     * ModelPart#translateAndRotate(PoseStack)}. Callers must {@link PoseStack#pushPose()}/{@link
     * PoseStack#popPose()} around this themselves, matching every other {@link PoseStack} caller in
     * this port (this method does not push/pop on the caller's behalf, so it can be called multiple
     * times in a row to compose parent/child transforms, exactly like vanilla's own {@code
     * ModelPart#render} recursion does internally).
     */
    protected static void applyPartPose(PoseStack poseStack, ModelPart part) {
        part.translateAndRotate(poseStack);
    }

    /**
     * {@link Model}'s single required override. Final: every leaf implements {@link
     * #renderArmorPiece} instead, which additionally receives {@link #slot} as an explicit
     * parameter (CE's own {@code renderArmor(Entity, float)} closes over its {@code type} field the
     * same way {@link #slot} is already available as an instance field here - the parameter exists
     * purely so a leaf's per-slot {@code switch} reads the same way CE's {@code ModelArmorHEV.
     * renderArmor}'s {@code switch(type)} does, without forcing a field read).
     */
    @Override
    public final void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int color) {
        renderArmorPiece(poseStack, consumer, packedLight, packedOverlay, color, slot);
    }

    /**
     * Draws this piece's geometry for {@code slot} (always equal to {@link #slot} - see {@link
     * #renderToBuffer}'s javadoc for why it is still passed explicitly). {@link #original}/{@link
     * #livingEntity} are guaranteed non-null here ({@link #getPropertiesFrom} always runs first -
     * see {@link ArmorRenderRegistry}).
     */
    protected abstract void renderArmorPiece(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                                              int packedOverlay, int color, EquipmentSlot slot);
}
