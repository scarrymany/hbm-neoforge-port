package com.hbm.client.render.item;

import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Reusable {@link BlockEntityWithoutLevelRenderer} (BEWLR) base for every full-3D, multi-part
 * OBJ-model item this port needs to draw in hand / on the ground / in a GUI slot instead of
 * vanilla's flat 2D generated-layer icon - primarily CE's ~54-65 Sedna guns
 * ({@code com.hbm.items.weapon.sedna.*}), but written generically enough for any other
 * OBJ-modeled plain item (grenades, tools, the {@code IMetaItemTesr}-flagged
 * {@code ItemGear}/{@code ItemBatteryPack} gap named by
 * {@code docs/phase5/renderer_framework_and_obj_models.md}'s "This port's own gap already
 * flagged" table) that wants the same dispatch shape without a block entity behind it.
 *
 * <h2>What this class is, and is not</h2>
 * This is <b>plumbing only</b> - the {@link ItemDisplayContext} dispatch, the per-context
 * "setup" hook methods, and a few small conveniences for feeding {@link HbmObjModel} (this
 * port's own OBJ loader/renderer, {@code com.hbm.render.loader.HbmObjModel} - read in full while
 * writing this class) named parts into the {@link MultiBufferSource}/{@link VertexConsumer} this
 * method already receives. It deliberately contains <b>zero</b> gun-specific logic: no
 * {@code BusAnimation} sampling, no ammo/reload-state reads, no muzzle-flash/shell-eject code.
 * Per this task's own scope boundary, all of that belongs to whichever concrete subclass a
 * future gun renderer (Content-wave task {@code c6}, "weapon gun rendering") writes - see
 * {@link #renderModel} below for the exact extension point, and {@link ExamplePlaceholderBEWLR}
 * in this package for a minimal, compiling, non-registered worked example.
 *
 * <h2>Sources this shape is confirmed against</h2>
 * CE's own base class ({@code com.hbm.render.item.TEISRBase} +
 * {@code com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase}, 134+531 lines, both read in
 * full) is the 1.12.2 {@code TileEntityItemStackRenderer} equivalent this class replaces
 * end-to-end. The concrete 1.21.1 API shape below (constructor args, the
 * {@code renderByItem(ItemStack, ItemDisplayContext, PoseStack, MultiBufferSource, int, int)}
 * override signature, the {@code ItemDisplayContext} enum members) is read directly, not
 * guessed, from {@code upstream/neo-edition}'s own real, compiling
 * {@code com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase} (309 lines, read in full) -
 * per this port's ground rules, used strictly to confirm the real NeoForge/vanilla API shape,
 * never for behavior. Neo Edition's per-context dispatch (translate/rotate the whole model into
 * item-space, then branch on {@code displayContext} to call a {@code setupFirstPerson}/
 * {@code setupThirdPerson}/{@code setupInv}/{@code setupEntity} hook followed by a
 * {@code renderFirstPerson}/{@code renderStatic} draw call) is the exact shape this class's
 * {@link #renderByItem} dispatch + {@code setupXxx}/{@link #renderModel} hooks mirror - see that
 * method's body for the one deliberate structural simplification made here (a single
 * {@link #renderModel} draw hook taking the resolved {@link ItemDisplayContext} instead of Neo
 * Edition's two separately-abstract {@code renderFirstPerson}/{@code renderStatic} methods): both
 * are equally valid, this port's own is simpler for a base class with no concrete gun logic yet
 * and lets a subclass still branch on {@code ctx} internally if first-person truly needs
 * different geometry than third-person/GUI/ground (most CE guns render the identical mesh in
 * every context, differing only in the {@code setupXxx} transform - see CE's own
 * {@code ItemRenderUzi.renderFirstPerson}/{@code renderStatic}, which call the same
 * {@code renderMainBody()} helper from both).
 *
 * <p>Neo Edition's own thread-local {@code RenderContext} singleton (a static current-PoseStack/
 * packedLight/packedOverlay holder its {@code renderByItem} pushes into once per call) is
 * <b>deliberately not copied</b> - this class instead threads the real {@link PoseStack}/
 * {@link MultiBufferSource}/light/overlay values through ordinary method parameters, which is
 * equally valid, more conventional modern-Minecraft style (matches vanilla's own
 * {@code ItemRenderer}/{@code BlockEntityRenderer} argument-passing convention throughout), and
 * avoids adding a mutable-static-singleton dependency to this port's rendering code for no
 * behavioral gain.
 *
 * <h2>Registration</h2>
 * A concrete subclass is registered per gun item via {@link HbmItemRendererRegistry#register} -
 * see that class's own javadoc for the confirmed {@code IClientItemExtensions}/
 * {@code RegisterClientExtensionsEvent} wiring and why it is kept as a small separate helper
 * rather than having this class implement {@code IClientItemExtensions} itself.
 *
 * <h2>OBJ pivot/orientation note for future subclasses (not enforced here)</h2>
 * CE's/Neo Edition's gun OBJ models are authored with a pivot that needs a per-item
 * "center in item-space, then flip 180 degrees about Y" normalization
 * ({@code poseStack.translate(0.5, 0, 0.5); poseStack.mulPose(Axis.YP.rotationDegrees(180F))} in
 * Neo Edition's {@code ItemRenderWeaponBase.renderByItem}, applied unconditionally before the
 * per-context switch) before any named part looks right in Minecraft's item-render space. This
 * base class deliberately does <b>not</b> bake that transform in as a mandatory step - it is a
 * property of how a specific family of OBJ files was modeled in Blender, not a universal law for
 * every possible OBJ item this class might ever back (a machine-part item authored with a
 * different pivot convention would not want it forced on unconditionally). A concrete gun
 * subclass should apply it itself, once, from inside its own {@code setupXxx} overrides (or a
 * small shared gun-specific base class layered on top of this one) rather than this generic
 * framework class assuming every future user needs the exact same correction.
 */
public abstract class HbmItemBEWLR extends BlockEntityWithoutLevelRenderer {

    protected HbmItemBEWLR() {
        // Exact 1.21.1 constructor shape confirmed against upstream/neo-edition's own
        // ItemRenderWeaponBase() (com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase.java:71-73,
        // read in full): (BlockEntityRenderDispatcher, EntityModelSet), both obtained from the live
        // Minecraft instance. Safe to call at BEWLR-instance-construction time (not a registry
        // .get() call, no eager-static-field-crash hazard per this port's own recurring-bug-pattern
        // ground rule) because every real construction site is either a lazily-instantiated field on
        // a client-only renderer class or happens from inside RegisterClientExtensionsEvent /
        // FMLClientSetupEvent.enqueueWork - both fire well after Minecraft's own client bootstrap has
        // a live Minecraft.getInstance() to hand back.
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    // ------------------------------------------------------------------------------------
    // Dispatch - the one method vanilla/NeoForge actually calls.
    // ------------------------------------------------------------------------------------

    /**
     * Final on purpose: every future concrete gun/OBJ-item renderer should extend the
     * {@code setupXxx}/{@link #renderModel} hooks below, not re-implement the
     * {@link ItemDisplayContext} switch itself - keeping one confirmed-correct dispatch shared
     * by every subclass instead of ~54-65 hand-copied switch statements (one per CE gun) is
     * exactly the kind of duplication this task's "reusable base/utility" framing asks for.
     */
    @Override
    public final void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                                    MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        try {
            switch (displayContext) {
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND ->
                        setupFirstPerson(stack, displayContext, poseStack);
                case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND ->
                        setupThirdPerson(stack, displayContext, poseStack);
                case GUI -> setupInventory(stack, poseStack);
                case GROUND -> setupGround(stack, poseStack);
                // FIXED (item frame), HEAD (armor-stand/player head slot - vanilla only reaches this
                // for a small set of headwear-shaped items, but the enum member exists and CE's own
                // 1.12 TransformType had the analogous HEAD case too), and NONE all fall back to the
                // same "just place it looking reasonable, no held-item context" hook, matching Neo
                // Edition's ItemRenderWeaponBase's own `default -> {}` no-special-case branch, except
                // this class still gives a subclass a named hook to override instead of silently
                // doing nothing.
                default -> setupEntity(stack, displayContext, poseStack);
            }
            renderModel(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
        } finally {
            poseStack.popPose();
        }
    }

    // ------------------------------------------------------------------------------------
    // Per-context setup hooks - all no-op by default, override only the ones a concrete item
    // actually needs a different pose for. Named and grouped to mirror CE's/Neo Edition's own
    // ItemRenderWeaponBase hook method names (setupFirstPerson/setupThirdPerson/setupInv/
    // setupEntity) so a port of a concrete CE gun renderer maps onto this framework almost
    // mechanically - only setupGround has no direct CE analogue (CE folds ground into "entity"
    // context; kept separate here since 1.21's ItemDisplayContext distinguishes them and a future
    // subclass may legitimately want a different ground-drop pose than a FIXED/HEAD pose).
    // ------------------------------------------------------------------------------------

    /** First-person (in the local player's own hand) - view-bob/sway/recoil pose math goes here. */
    protected void setupFirstPerson(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack) {
    }

    /** Third-person (another entity's hand, or the local player seen from a third-person camera). */
    protected void setupThirdPerson(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack) {
    }

    /** Inventory/GUI slot icon. */
    protected void setupInventory(ItemStack stack, PoseStack poseStack) {
    }

    /** Dropped-on-the-ground entity render. */
    protected void setupGround(ItemStack stack, PoseStack poseStack) {
    }

    /** {@code FIXED} (item frame), {@code HEAD}, {@code NONE}, and any other unhandled context. */
    protected void setupEntity(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack) {
    }

    /**
     * The actual draw call - every subclass must implement this. Called once per
     * {@link #renderByItem} invocation, after whichever {@code setupXxx} hook above ran, with the
     * same {@link PoseStack} (already mutated by that hook) still active. A concrete subclass
     * reads live gameplay/animation state off {@code stack} (this port's own gun state lives in
     * its Data Components - see {@code com.hbm.items.weapon.sedna.GunStateComponent} - not out of
     * scope for reading here, just not this class's job to interpret) and issues one or more
     * {@link HbmObjModel#renderPart}-family calls, typically through this class's
     * {@link #renderPart}/{@link #renderOnly}/{@link #renderAll} convenience overloads below.
     */
    protected abstract void renderModel(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedLight, int packedOverlay);

    // ------------------------------------------------------------------------------------
    // IClientItemExtensions delegate targets - NOT an implementation of that interface itself
    // (see HbmItemRendererRegistry's javadoc for why registration keeps a separate small wrapper
    // object). A subclass overrides these instead of the interface's own methods; the registry
    // forwards 1:1.
    // ------------------------------------------------------------------------------------

    /**
     * Mirrors {@code IClientItemExtensions#applyForgeHandTransform}'s exact signature (confirmed
     * from Neo Edition's real, compiling {@code GunFactoryClient.registerGunItemRenderer}, which
     * always returns {@code true} after its own pose mutation - i.e. "I positioned this myself,
     * do not also apply the vanilla default hand transform"). Deliberately defaults to
     * {@code false} here (let vanilla apply its own default transform) rather than {@code true}:
     * this base class does no first-person pose math of its own ({@link #setupFirstPerson} is a
     * no-op until a subclass overrides it), and per
     * {@code docs/phase5/weapon_gun_rendering_animloader.md}'s "Key risks" #2, this port has
     * <b>zero Mixin infrastructure yet</b> (no {@code .mixins.json}, no {@code [[mixins]]} TOML
     * entry - confirmed absent, vs. Neo Edition's confirmed-present {@code hbmsntm.mixins.json}),
     * so returning {@code true} unconditionally here, before any subclass actually supplies real
     * first-person pose math, would suppress vanilla's default swing/bob without this port having
     * anything to show in its place. Once a concrete gun subclass implements real
     * {@link #setupFirstPerson} pose math it should override this to return {@code true} for that
     * case - full visual parity (no vanilla swing-arc bleed-through even with a {@code true}
     * return) additionally needs the Mixin pair Neo Edition ships
     * ({@code GameRendererMixin}/{@code ItemInHandRendererMixin}) that this port does not have
     * yet; that is a build-tooling gap out of this task's scope, not something this class can
     * paper over.
     */
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                            ItemStack itemInHand, float partialTick, float equipProcess,
                                            float swingProcess) {
        return false;
    }

    /**
     * Mirrors {@code IClientItemExtensions#getArmPose}'s exact signature. Returning {@code null}
     * (the default) tells {@link HbmItemRendererRegistry}'s wrapper to fall through to
     * {@code IClientItemExtensions.super.getArmPose(...)} - i.e. "no custom arm pose, use
     * vanilla's default for this item" - matching Neo Edition's own 4 ported guns, none of which
     * override the pose either (per {@code docs/phase5/weapon_gun_rendering_animloader.md}'s
     * confirmed-API-shapes section: "neither CE nor Neo Edition's 4 ported guns need this... but
     * the hook exists and is real; confirm per-weapon in Phase 5 rather than assuming none need
     * it"). {@link #captureHoldingEntity} below is always called first regardless of what this
     * method returns, matching Neo Edition's own {@code renderer.setEntity(living)}-then-
     * fall-through pattern.
     */
    @Nullable
    public HumanoidModel.ArmPose getArmPose(LivingEntity living, InteractionHand hand, ItemStack itemStack) {
        return null;
    }

    /**
     * Called once per {@code getArmPose} query (i.e. once per living entity per frame that is
     * actually holding this item) with the entity holding the item. This is the confirmed hook
     * point for correct third-person-of-other-players rendering (a gun's own recoil/muzzle-flash
     * state needs to know <i>which</i> entity to read live fire-timing state for, not just "the
     * local player" - see {@code docs/phase5/weapon_gun_rendering_animloader.md}'s "Multiplayer
     * third-person muzzle-flash correctness" finding for the concrete CE bug / Neo Edition fix
     * this exists to let a subclass implement correctly). No-op by default; a subclass with no
     * per-entity state (most CE guns' current, static-field-based `lastShot`/`shotRand` fast path)
     * can leave this unoverridden.
     */
    public void captureHoldingEntity(LivingEntity living) {
    }

    // ------------------------------------------------------------------------------------
    // HbmObjModel render conveniences - thin bridges from this method's own (PoseStack,
    // MultiBufferSource, packedLight, packedOverlay) args onto HbmObjModel's
    // (PoseStack, VertexConsumer, packedLight, packedOverlay) contract, saving every subclass the
    // same three-line "obtain a VertexConsumer for this texture's RenderType" boilerplate. Every
    // overload also exists in a form taking an explicit RenderType instead of a texture
    // ResourceLocation, for the rarer translucent/additive/fullbright part (see HbmObjModel's own
    // class javadoc, "Texture / light note for future call sites") a concrete renderer may need.
    // ------------------------------------------------------------------------------------

    protected static VertexConsumer buffer(MultiBufferSource bufferSource, ResourceLocation texture) {
        return bufferSource.getBuffer(HbmObjModel.renderType(texture));
    }

    protected static VertexConsumer buffer(MultiBufferSource bufferSource, RenderType renderType) {
        return bufferSource.getBuffer(renderType);
    }

    protected void renderPart(HbmObjModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                               ResourceLocation texture, int packedLight, int packedOverlay, String partName) {
        model.renderPart(poseStack, buffer(bufferSource, texture), packedLight, packedOverlay, partName);
    }

    protected void renderPart(HbmObjModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                               RenderType renderType, int packedLight, int packedOverlay, String partName) {
        model.renderPart(poseStack, buffer(bufferSource, renderType), packedLight, packedOverlay, partName);
    }

    protected void renderPart(HbmObjModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                               ResourceLocation texture, int packedLight, int packedOverlay, int argbTint,
                               String partName) {
        model.renderPart(poseStack, buffer(bufferSource, texture), packedLight, packedOverlay, argbTint, partName);
    }

    protected void renderOnly(HbmObjModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                               ResourceLocation texture, int packedLight, int packedOverlay, String... partNames) {
        model.renderOnly(poseStack, buffer(bufferSource, texture), packedLight, packedOverlay, partNames);
    }

    protected void renderAllExcept(HbmObjModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                    ResourceLocation texture, int packedLight, int packedOverlay,
                                    String... excludedPartNames) {
        model.renderAllExcept(poseStack, buffer(bufferSource, texture), packedLight, packedOverlay, excludedPartNames);
    }

    protected void renderAll(HbmObjModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                              ResourceLocation texture, int packedLight, int packedOverlay) {
        model.renderAll(poseStack, buffer(bufferSource, texture), packedLight, packedOverlay);
    }
}
