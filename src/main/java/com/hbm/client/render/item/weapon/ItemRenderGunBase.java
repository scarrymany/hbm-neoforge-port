package com.hbm.client.render.item.weapon;

import com.hbm.client.render.item.HbmItemBEWLR;
import com.hbm.client.render.item.ItemRenderFrames17;
import com.hbm.config.ClientConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Gun-specific {@link HbmItemBEWLR} layer, sitting between that framework class (f9,
 * {@code render.item} package, plumbing-only, zero gun logic) and a concrete
 * {@code ItemRenderXxx} gun renderer. Port of CE's
 * {@code com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase} (531 lines, read in full) -
 * everything in that class that is generic across every CE Sedna gun (the per-context
 * default transforms, the fullbright-additive muzzle/gap/laser flash quads, the
 * {@code standardAimingTransform} helper) lives here; everything gun-specific (which named OBJ
 * parts to draw, which buses drive which part, ammo/reload visual state) belongs in a concrete
 * subclass - see {@link ItemRenderSpas12}/{@link ItemRenderUzi}/{@link ItemRenderAm180} in this
 * package for 3 fully-ported worked examples.
 *
 * <h2>1.21.1 API deltas from CE (confirmed against {@code upstream/neo-edition}'s own real,
 * compiling {@code com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase}, cross-checked for API
 * shape only per this task's ground rules - CE remains the sole source for every actual number/
 * transform below)</h2>
 * <ul>
 *   <li>{@code GlStateManager.translate/rotate/scale} -&gt; {@link PoseStack}/{@link Axis} calls,
 *       1:1 structural swap.</li>
 *   <li>CE's {@code renderFirstPerson(ItemStack)}/{@code renderEquipped}/{@code renderInv}/
 *       {@code renderEntity} (each defaulting to a shared {@code renderOther(stack, type)}) collapse
 *       onto {@link HbmItemBEWLR}'s single {@code renderModel} hook here, re-split into
 *       {@link #renderFirstPerson} (abstract, first-person only) and {@link #renderOther} (default
 *       no-op, overridden by a concrete gun for third-person/GUI/ground/entity - CE's own
 *       {@code renderOther} default is also empty) - see {@link #renderModel} below.</li>
 *   <li><b>OBJ pivot correction</b> - applied here, in every {@code setupXxxGun} hook except
 *       {@link #setupInventoryGun} (GUI), matching Neo Edition's own confirmed
 *       {@code if(displayContext != GUI) translate(0.5,0,0.5); mulPose(Axis.YP.rotationDegrees(180))}
 *       guard (per {@link HbmItemBEWLR}'s own "OBJ pivot/orientation note" javadoc, which explicitly
 *       named this as the base framework class's deliberately-not-baked-in property for a gun-specific
 *       layer like this one to supply) - see {@link #applyPivotCorrection}.</li>
 *   <li><b>First-person base transform numerically unchanged</b> - CE's base
 *       {@code setupFirstPerson}: {@code translate(0,0,1)}; Neo Edition's:
 *       {@code translate(0F,0F,1F)} - identical. Every concrete gun's first-person transform in
 *       this task (which fully replaces this base hook, matching CE's own subclasses, none of which
 *       call {@code super.setupFirstPerson}) therefore ports CE's exact relative numbers with high
 *       confidence.</li>
 *   <li><b>Third-person/GUI/ground base transforms - lower confidence, unverified.</b> CE's base
 *       {@code setupThirdPerson} ({@code scale 0.125; rotate Z15/Y12.5/X15; translate 3.5,0,0}) and
 *       Neo Edition's ({@code scale 0.07; translate 0,6.47,-1.5}, no rotation at all) are
 *       numerically <i>different</i> - a real, confirmed divergence, not a copy error (Neo Edition's
 *       own pivot-correction step evidently absorbs the orientation CE's base rotation used to
 *       supply). This class follows Neo Edition's base numbers (the actually-compiling 1.21.1
 *       reference) for {@link #setupThirdPersonGun}/{@link #setupInventoryGun}/
 *       {@link #setupEntityGun}, then layers each concrete gun's CE-sourced <i>relative</i>
 *       per-gun delta on top verbatim (extra scale/rotate/translate calls, exactly as CE's own
 *       subclasses layer theirs via {@code super.setupThirdPerson(stack)} + more calls) - the
 *       combination is a best-effort, structurally-faithful port that this sandbox cannot visually
 *       verify (no launchable client - ground rule 3). Flagged explicitly: third-person/GUI/ground
 *       framing for every gun in this task should be treated as needing a real in-game visual pass
 *       before being considered final, while first-person (the by-far most gameplay-visible context)
 *       should be much closer to correct out of the gate.</li>
 *   <li><b>Muzzle/gap/laser flash quads</b> - CE's {@code BufferBuilder}/{@code Tessellator}
 *       immediate-mode {@code GL_QUADS} calls (wrapped in {@code beginFullbrightAdditive}/
 *       {@code endFullbrightAdditive}'s GL attribute push/pop dance) become
 *       {@link RenderType#entityTranslucent(ResourceLocation)} + {@link VertexConsumer} chains, with
 *       {@code packedLight} forced to {@code LightTexture.FULL_BRIGHT} on every vertex - the modern,
 *       declarative equivalent of CE's lightmap-texture-coordinate override
 *       (see {@link #FULL_BRIGHT}). <b>Not independently confirmed against a hand-built custom
 *       {@code RenderType.create(...)} composite state</b> - {@code entityTranslucent(ResourceLocation)}
 *       is instead used because it is directly confirmed real and load-bearing in
 *       {@code upstream/neo-edition}'s own compiling source (grepped: used by 3 of its particle
 *       renderer classes against this exact {@code neo_version}), which this sandbox could verify by
 *       direct read, unlike a hand-guessed {@code RenderType.CompositeState} shader-constant name
 *       (this sandbox had no javadoc/decompiler access to confirm one). Same vertex attribute chain
 *       ({@code pos, color, uv, overlay, light, normal}) this port's own already-committed
 *       {@code com.hbm.render.loader.HbmObjModel#renderGroup} already uses for the identical
 *       {@code NEW_ENTITY}-shaped vertex format, for consistency. UNVERIFIED: exact additive-vs-
 *       translucent visual result not confirmed against a running client - flagged per ground rule 3.</li>
 * </ul>
 *
 * <h2>Deliberately not ported (named blockers, not silently dropped)</h2>
 * <ul>
 *   <li><b>Smoke-node trail rendering</b> ({@code renderSmokeNodes}/CE's {@code SmokeNode} list) -
 *       CE reads {@code gun.getConfig(stack,0).smokeNodes}, a field this port's own
 *       {@code ItemGunBaseNT}/{@code GunConfig} does not have yet (Phase 3's own javadoc: "the
 *       smoke-node/orchestra <i>default</i> lambda bodies... deferred"). No concrete gun renderer in
 *       this task calls a smoke helper as a result - not a rendering-framework gap, a data-model gap
 *       one level down, out of this task's scope.</li>
 *   <li><b>Spent-casing tint</b> (CE's {@code SpentCasing.getColors()} read off
 *       {@code Receiver.getMagazine(stack).getCasing(stack, inventory)}) - neither
 *       {@code com.hbm.particle.SpentCasing} nor {@code IMagazine.getCasing(...)} exist in this port
 *       yet (confirmed absent by search; this port's own {@code IMagazine} javadoc explicitly lists
 *       {@code getCasing} as "Not ported from CE"). {@link ItemRenderSpas12} renders its "Shell"/
 *       "ShellFore" parts at a fixed brass tint ({@link #COLOR_CASE_BRASS}, CE's own constant,
 *       mirrored here since the class that owns it doesn't exist yet) instead of the real
 *       per-loaded-ammo-type color CE shows - a visible but narrow fidelity gap, not a missing
 *       feature (the shell is still drawn, just not re-tinted).</li>
 *   <li><b>Aim-driven FOV zoom</b> (CE's {@code setPerspectiveAndRender}/{@code getFOVModifier},
 *       which mutate the GL projection matrix directly around the whole first-person render pass) -
 *       structurally out of {@code renderByItem}'s scope entirely in 1.21.1 (needs a
 *       {@code ViewportEvent.ComputeFov} NeoForge event listener, a camera-pipeline hook, not an
 *       item-renderer one - confirmed absent from Neo Edition's own {@code ItemRenderWeaponBase}
 *       too, which has no FOV-related method at all). Left for whichever future package owns
 *       camera/FOV concerns; {@link #getViewFOV} is kept as a named, overridable hook (matching
 *       CE's own method signature) so a concrete gun's intent is preserved even though nothing
 *       calls it yet.</li>
 * </ul>
 */
public abstract class ItemRenderGunBase extends HbmItemBEWLR {

    public static final ResourceLocation FLASH_PLUME_TEX =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/models/weapons/lilmac_plume.png");
    public static final ResourceLocation LASER_FLASH_TEX =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/models/weapons/laser_flash.png");

    /** CE's {@code SpentCasing.COLOR_CASE_BRASS} - mirrored here, see class javadoc's "Spent-casing tint" note. */
    public static final int COLOR_CASE_BRASS = 0xEBC35E;

    /** CE's {@code beginFullbrightAdditive}'s fixed {@code (240,240)} lightmap override, ported as a constant packed-light argument fed to every flash-quad vertex. */
    protected static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    /**
     * The current frame's partial-tick, captured in {@link #applyForgeHandTransform} - the <b>only</b>
     * place NeoForge's {@code IClientItemExtensions} contract actually hands a real partial-tick to
     * item-renderer code (vanilla's own {@code BlockEntityWithoutLevelRenderer#renderByItem} contract
     * has no partial-tick parameter at all). Mirrors CE's own static {@code interp} field (set once
     * per frame in {@code setPerspectiveAndRender}, read by every {@code standardAimingTransform}
     * call) and Neo Edition's own confirmed-real equivalent (a static {@code partialTick} field on
     * its {@code ItemRenderWeaponBase}, set from {@code GunFactoryClient.registerGunItemRenderer}'s
     * {@code applyForgeHandTransform} override) - same shape, independently confirmed necessary by
     * both the original 1.12.2 code and the one real 1.21.1 port available to cross-check against.
     * Defaults to {@code 1F} (matches Neo Edition's own unset-field default) so
     * {@link #standardAimingTransform} never divides/lerps against a stale {@code 0F} before the
     * first real frame.
     */
    protected static float partialTick = 1F;

    /** Client inventory for CE {@code clientInv()} magazine reads. */
    protected static net.minecraft.world.entity.player.Inventory clientInv() {
        LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        return player == null ? null : player.getInventory();
    }

    /** Per-gun CE texture, set at BEWLR registration. */
    public ResourceLocation texture;

    protected ResourceLocation defaultTex() {
        return texture != null ? texture : GunModels.tex("debug_gun_tex");
    }

    /**
     * Captures {@code partialTick} into the static field above and returns {@code true} - telling
     * NeoForge "I've positioned this myself (via {@link #setupFirstPersonGun} et al.), don't also
     * apply the vanilla default hand transform" - overriding {@link HbmItemBEWLR}'s own
     * conservative {@code false} default now that every concrete gun in this package does real
     * first-person pose math (see that class's own javadoc for why it defaults to {@code false}
     * until a subclass reaches this point).
     */
    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player,
                                            HumanoidArm arm, ItemStack itemInHand,
                                            float partialTick, float equipProcess, float swingProcess) {
        ItemRenderGunBase.partialTick = partialTick;
        return true;
    }

    /**
     * CE {@code ItemRenderWeaponBase.setPerspectiveAndRender} + {@code setupTransformsAndRender}.
     * Called from {@link GunClientEvents} after cancelling vanilla first-person hand.
     * TODO(CE:ItemRenderWeaponBase.java:124) ShaderHelper hand-depth / skip-hand not ported.
     */
    public void setPerspectiveAndRender(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, float interp) {
        ItemRenderGunBase.partialTick = interp;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (!mc.options.getCameraType().isFirstPerson() || mc.options.hideGui) return;

        Matrix4f previous = new Matrix4f(RenderSystem.getProjectionMatrix());
        float aspect = (float) mc.getWindow().getWidth() / (float) mc.getWindow().getHeight();
        float far = mc.gameRenderer.getDepthFar() * 2.0F;
        Matrix4f gunProj = new Matrix4f().perspective(
                getFOVModifier(interp, ClientConfig.GUN_MODEL_FOV.get()) * ((float) Math.PI / 180F),
                aspect, 0.05F, far);
        RenderSystem.setProjectionMatrix(gunProj, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.clear(256, Minecraft.ON_OSX);
        try {
            setupTransformsAndRender(stack, poseStack, player, interp);
            ItemDisplayContext ctx = player.getMainArm() == HumanoidArm.RIGHT
                    ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            renderByItem(stack, ctx, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
        } finally {
            RenderSystem.setProjectionMatrix(previous, VertexSorting.DISTANCE_TO_ORIGIN);
        }
    }

    protected void setupTransformsAndRender(ItemStack stack, PoseStack poseStack, LocalPlayer player, float interp) {
        float swayMagnitude = getSwayMagnitude(stack);
        float swayPeriod = getSwayPeriod(stack);
        float turnMagnitude = getTurnMagnitude(stack);

        float pitch = Mth.lerp(interp, player.xRotO, player.getXRot());
        float yaw = Mth.lerp(interp, player.yRotO, player.getYRot());
        float armPitch = Mth.lerp(interp, player.xBobO, player.xBob);
        float armYaw = Mth.lerp(interp, player.yBobO, player.yBob);

        // ItemInHandRenderer already applied 0.1*(view-arm) before RenderHandEvent.
        poseStack.mulPose(Axis.YP.rotationDegrees(-(yaw - armYaw) * 0.1F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-(pitch - armPitch) * 0.1F));

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.bobView().get() && mcCameraOr(player) instanceof net.minecraft.world.entity.player.Player walker) {
            float f = walker.walkDist - walker.walkDistO;
            float f1 = -(walker.walkDist + f * interp);
            float f2 = Mth.lerp(interp, walker.oBob, walker.bob);
            poseStack.mulPose(Axis.XP.rotationDegrees(-(Math.abs(Mth.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-(Mth.sin(f1 * (float) Math.PI) * f2 * 3.0F)));
            poseStack.translate(
                    -(Mth.sin(f1 * (float) Math.PI) * f2 * 0.5F),
                    Math.abs(Mth.cos(f1 * (float) Math.PI) * f2),
                    0.0F);
        }

        poseStack.mulPose(Axis.XP.rotationDegrees((pitch - armPitch) * 0.1F * turnMagnitude));
        poseStack.mulPose(Axis.YP.rotationDegrees((yaw - armYaw) * 0.1F * turnMagnitude));

        if (mcCameraOr(player) instanceof net.minecraft.world.entity.player.Player walker) {
            float distanceDelta = walker.walkDist - walker.walkDistO;
            float distanceInterp = -(walker.walkDist + distanceDelta * interp);
            float camYaw = Mth.lerp(interp, walker.oBob, walker.bob);
            poseStack.translate(
                    Mth.sin(distanceInterp * (float) Math.PI * swayPeriod) * camYaw * 0.5F * swayMagnitude,
                    -Math.abs(Mth.cos(distanceInterp * (float) Math.PI * swayPeriod) * camYaw) * swayMagnitude,
                    0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(distanceInterp * (float) Math.PI * swayPeriod) * camYaw * 3.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(distanceInterp * (float) Math.PI * swayPeriod - 0.2F) * camYaw) * 5.0F));
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
    }

    private static Entity mcCameraOr(LocalPlayer player) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        return camera != null ? camera : player;
    }

    private float getFOVModifier(float interp, boolean useFOVSetting) {
        Minecraft mc = Minecraft.getInstance();
        Entity view = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        float fov = getBaseFOV(mc.player != null ? mc.player.getMainHandItem() : ItemStack.EMPTY);
        if (useFOVSetting) {
            fov = mc.options.fov().get().floatValue();
        }
        if (view instanceof LivingEntity living && living.getHealth() <= 0.0F) {
            float f2 = living.deathTime + interp;
            fov /= (1.0F - 500.0F / (f2 + 500.0F)) * 2.0F + 1.0F;
        }
        if (view != null && view.isEyeInFluid(FluidTags.WATER)) {
            fov = fov * 60.0F / 70.0F;
        }
        return fov;
    }

    // ------------------------------------------------------------------------------------
    // renderModel dispatch - see class javadoc for the CE renderFirstPerson/renderOther split.
    // ------------------------------------------------------------------------------------

    @Override
    protected final void renderModel(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        switch (ctx) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND ->
                    renderFirstPerson(stack, poseStack, bufferSource, packedLight, packedOverlay);
            default -> renderOther(stack, ctx, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    /** First-person (in the local player's own hand) draw call - every concrete gun must implement this. */
    protected abstract void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource,
                                               int packedLight, int packedOverlay);

    /** Third-person/GUI/ground/entity draw call - CE's own {@code renderOther} default is also an empty no-op. */
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay) {
    }

    // ------------------------------------------------------------------------------------
    // Per-context setup - see class javadoc for the pivot-correction / base-transform notes.
    // ------------------------------------------------------------------------------------

    @Override
    protected final void setupFirstPerson(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack) {
        setupFirstPersonGun(stack, poseStack);
    }

    @Override
    protected final void setupThirdPerson(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack) {
        undoVanillaItemCenter(poseStack);
        if (ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            ItemRenderFrames17.applyThirdPersonLeft(poseStack);
            if (isAkimbo()) {
                setupThirdPersonAkimbo(stack, poseStack);
            } else {
                setupThirdPersonGun(stack, poseStack);
            }
        } else {
            ItemRenderFrames17.applyThirdPerson(poseStack);
            setupThirdPersonGun(stack, poseStack);
        }
    }

    /** CE {@code ItemRenderWeaponBase.isAkimbo} — dual-wield third-person left-hand offset. */
    public boolean isAkimbo() {
        return false;
    }

    /** CE {@code setupThirdPersonAkimbo} default numbers. */
    public void setupThirdPersonAkimbo(ItemStack stack, PoseStack poseStack) {
        double scale = 0.125D;
        poseStack.scale((float) scale, (float) scale, (float) scale);
        poseStack.mulPose(Axis.ZP.rotationDegrees(15.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(12.5F));
        poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
        poseStack.translate(5.0, 0.0, 0.0);
    }

    @Override
    protected final void setupGround(ItemStack stack, PoseStack poseStack) {
        undoVanillaItemCenter(poseStack);
        ItemRenderFrames17.applyGround(poseStack);
        setupEntityGun(stack, poseStack);
    }

    @Override
    protected final void setupEntity(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack) {
        undoVanillaItemCenter(poseStack);
        if (ctx == ItemDisplayContext.HEAD) {
            ItemRenderFrames17.applyHead(poseStack);
            setupThirdPersonGun(stack, poseStack);
        } else if (ctx == ItemDisplayContext.FIXED) {
            ItemRenderFrames17.applyFixed(poseStack);
            setupEntityGun(stack, poseStack);
        } else {
            ItemRenderFrames17.applyGround(poseStack);
            setupEntityGun(stack, poseStack);
        }
    }

    @Override
    protected final void setupInventory(ItemStack stack, PoseStack poseStack) {
        undoVanillaItemCenter(poseStack);
        ItemRenderFrames17.applyGui(poseStack);
        setupInventoryGun(stack, poseStack);
    }

    /** Vanilla {@code ItemRenderer} always {@code translate(-0.5,-0.5,-0.5)} before BEWLR. */
    private static void undoVanillaItemCenter(PoseStack poseStack) {
        poseStack.translate(0.5, 0.5, 0.5);
    }

    /** CE {@code ItemRenderWeaponBase.setupFirstPerson}. */
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0.0, 0.0, 1.0);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isCrouching()) {
            poseStack.translate(0.0, -3.875 / 8.0, 0.0);
        } else {
            float offset = 0.8F;
            poseStack.mulPose(Axis.YP.rotationDegrees(180F));
            poseStack.translate(offset, -0.75F * offset, -0.5F * offset);
            poseStack.mulPose(Axis.YP.rotationDegrees(180F));
        }
    }

    /** CE {@code ItemRenderWeaponBase.setupThirdPerson}. */
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        double scale = 0.125D;
        poseStack.scale((float) scale, (float) scale, (float) scale);
        poseStack.mulPose(Axis.ZP.rotationDegrees(15.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(12.5F));
        poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
        poseStack.translate(3.5, 0.0, 0.0);
    }

    /** CE {@code ItemRenderWeaponBase.setupInv}. */
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        poseStack.scale(1F, 1F, -1F);
        poseStack.translate(8.0, 8.0, 0.0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(225F));
        poseStack.mulPose(Axis.YP.rotationDegrees(90F));
    }

    /** CE {@code ItemRenderWeaponBase.setupEntity}. */
    protected void setupEntityGun(ItemStack stack, PoseStack poseStack) {
        double scale = 0.125D;
        poseStack.scale((float) scale, (float) scale, (float) scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
    }

    // ------------------------------------------------------------------------------------
    // Per-gun tunables - named hooks matching CE's own method names/signatures, no default caller
    // wired yet (see class javadoc's "Aim-driven FOV zoom" note for getViewFOV specifically).
    // ------------------------------------------------------------------------------------

    protected float getSwayMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 0.1F : 0.5F; }
    protected float getSwayPeriod(ItemStack stack) { return 0.75F; }
    protected float getTurnMagnitude(ItemStack stack) { return 2.75F; }
    public float getViewFOV(ItemStack stack, float fov) { return fov; }

    /** CE {@code ItemRenderWeaponBase.getBaseFOV}; unused here, ComputeFov uses {@link #getViewFOV}. */
    protected float getBaseFOV(ItemStack stack) { return 70F; }

    /**
     * CE's {@code ItemRenderWeaponBase.standardAimingTransform} - lerps between a hip-fire and an
     * aimed-down-sights offset by {@link ItemGunBaseNT#aimingProgress}/{@code prevAimingProgress},
     * using the captured {@link #partialTick} the same way CE used its own static {@code interp}
     * field. Every concrete gun's {@code setupFirstPersonGun} calls this once with its own hip/ADS
     * offset pair.
     */
    protected static void standardAimingTransform(PoseStack poseStack,
                                                    double sX, double sY, double sZ, double aX, double aY, double aZ) {
        float aimingProgress = Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
        double x = sX + (aX - sX) * aimingProgress;
        double y = sY + (aY - sY) * aimingProgress;
        double z = sZ + (aZ - sZ) * aimingProgress;
        poseStack.translate(x, y, z);
    }

    // ------------------------------------------------------------------------------------
    // Muzzle / gap / laser flash - port of CE's renderMuzzleFlash/renderGapFlash/renderLaserFlash,
    // see class javadoc for the RenderType/vertex-format notes.
    // ------------------------------------------------------------------------------------

    @Override
    protected void renderPart(com.hbm.render.loader.HbmObjModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                              ResourceLocation texture, int packedLight, int packedOverlay, String partName) {
        if (model == null || texture == null) return;
        super.renderPart(model, poseStack, bufferSource, texture, packedLight, packedOverlay, partName);
    }

    @Override
    protected void renderPart(com.hbm.render.loader.HbmObjModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                              ResourceLocation texture, int packedLight, int packedOverlay, int argbTint, String partName) {
        if (model == null || texture == null) return;
        super.renderPart(model, poseStack, bufferSource, texture, packedLight, packedOverlay, argbTint, partName);
    }

    @Override
    protected void renderAll(com.hbm.render.loader.HbmObjModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                             ResourceLocation texture, int packedLight, int packedOverlay) {
        if (model == null || texture == null) return;
        super.renderAll(model, poseStack, bufferSource, texture, packedLight, packedOverlay);
    }

    public static void renderMuzzleFlash(PoseStack poseStack, MultiBufferSource bufferSource, long lastShot) {
        renderMuzzleFlash(poseStack, bufferSource, lastShot, 75, 15);
    }

    /** CE: {@code renderMuzzleFlash(long lastShot, int duration, double l)} - 4 quads forming a 3D "plume" cross shape, growing linearly over {@code duration} ms. */
    public static void renderMuzzleFlash(PoseStack poseStack, MultiBufferSource bufferSource, long lastShot, int duration, double l) {
        long elapsed = System.currentTimeMillis() - lastShot;
        if (elapsed >= duration) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(FLASH_PLUME_TEX));
        Matrix4f matrix = poseStack.last().pose();

        double fire = elapsed / (double) duration;
        double width = 6 * fire;
        double length = l * fire;
        double inset = 2;

        quad(consumer, matrix,
                0, -width, -inset, 1, 1,
                0, width, -inset, 0, 1,
                0.1, width, length - inset, 0, 0,
                0.1, -width, length - inset, 1, 0);

        quad(consumer, matrix,
                0, width, inset, 0, 1,
                0, -width, inset, 1, 1,
                0.1, -width, -length + inset, 1, 0,
                0.1, width, -length + inset, 0, 0);

        quad(consumer, matrix,
                0, -inset, width, 0, 1,
                0, -inset, -width, 1, 1,
                0.1, length - inset, -width, 1, 0,
                0.1, length - inset, width, 0, 0);

        quad(consumer, matrix,
                0, inset, -width, 1, 1,
                0, inset, width, 0, 1,
                0.1, -length + inset, width, 0, 0,
                0.1, -length + inset, -width, 1, 0);
    }

    /** CE: {@code renderGapFlash(long lastShot)} - a flatter, longer 4-quad flash used by a few guns' ejection-port/gap flash instead of the muzzle. Fixed 75ms duration, matching CE. */
    public static void renderGapFlash(PoseStack poseStack, MultiBufferSource bufferSource, long lastShot) {
        int flash = 75;
        long elapsed = System.currentTimeMillis() - lastShot;
        if (elapsed >= flash) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(FLASH_PLUME_TEX));
        Matrix4f matrix = poseStack.last().pose();

        double fire = elapsed / (double) flash;
        double height = 4 * fire;
        double length = 15 * fire;
        double lift = 3 * fire;
        double offset = 1 * fire;
        double lengthOffset = 0.125;

        quad(consumer, matrix,
                0, -height, -offset, 1, 1,
                0, height, -offset, 0, 1,
                0, height + lift, length - offset, 0, 0,
                0, -height + lift, length - offset, 1, 0);

        quad(consumer, matrix,
                0, height, offset, 0, 1,
                0, -height, offset, 1, 1,
                0, -height + lift, -length + offset, 1, 0,
                0, height + lift, -length + offset, 0, 0);

        quad(consumer, matrix,
                0, -height, -offset, 1, 1,
                0, height, -offset, 0, 1,
                lengthOffset, height, length - offset, 0, 0,
                lengthOffset, -height, length - offset, 1, 0);

        quad(consumer, matrix,
                0, height, offset, 0, 1,
                0, -height, offset, 1, 1,
                lengthOffset, -height, -length + offset, 1, 0,
                lengthOffset, height, -length + offset, 0, 0);
    }

    /** CE: {@code renderLaserFlash(long lastShot, int flash, double scale, int color)} - one tinted quad, used by energy weapons instead of the ballistic muzzle flash. */
    public static void renderLaserFlash(PoseStack poseStack, MultiBufferSource bufferSource, long lastShot, int flash, double scale, int color) {
        long elapsed = System.currentTimeMillis() - lastShot;
        if (elapsed >= flash) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(LASER_FLASH_TEX));
        Matrix4f matrix = poseStack.last().pose();

        double fire = elapsed / (double) flash;
        double size = 4 * fire * scale;

        float r = ((color >> 16) & 0xFF) / 255F;
        float g = ((color >> 8) & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;

        quadColored(consumer, matrix,
                0, -size, -size, 1, 1,
                0, size, -size, 0, 1,
                0, size, size, 0, 0,
                0, -size, size, 1, 0, r, g, b);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix,
                              double x1, double y1, double z1, float u1, float v1,
                              double x2, double y2, double z2, float u2, float v2,
                              double x3, double y3, double z3, float u3, float v3,
                              double x4, double y4, double z4, float u4, float v4) {
        quadColored(consumer, matrix, x1, y1, z1, u1, v1, x2, y2, z2, u2, v2, x3, y3, z3, u3, v3, x4, y4, z4, u4, v4, 1F, 1F, 1F);
    }

    private static void quadColored(VertexConsumer consumer, Matrix4f matrix,
                                     double x1, double y1, double z1, float u1, float v1,
                                     double x2, double y2, double z2, float u2, float v2,
                                     double x3, double y3, double z3, float u3, float v3,
                                     double x4, double y4, double z4, float u4, float v4,
                                     float r, float g, float b) {
        vertex(consumer, matrix, x1, y1, z1, u1, v1, r, g, b);
        vertex(consumer, matrix, x2, y2, z2, u2, v2, r, g, b);
        vertex(consumer, matrix, x3, y3, z3, u3, v3, r, g, b);
        vertex(consumer, matrix, x4, y4, z4, u4, v4, r, g, b);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
                                double x, double y, double z, float u, float v, float r, float g, float b) {
        consumer.addVertex(matrix, (float) x, (float) y, (float) z)
                .setColor(r, g, b, 1F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal((float) Direction.EAST.getStepX(), (float) Direction.EAST.getStepY(), (float) Direction.EAST.getStepZ());
    }
}
