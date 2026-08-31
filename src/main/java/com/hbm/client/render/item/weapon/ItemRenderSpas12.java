package com.hbm.client.render.item.weapon;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Port of CE's {@code com.hbm.render.item.weapon.sedna.ItemRenderSPAS12} (140 lines, read in full)
 * - the {@code gun_spas12} first-person/third-person renderer. Every transform value, part name,
 * and animation-bus name below is copied verbatim from that class; only the API surface changed
 * (see {@link ItemRenderGunBase}'s own javadoc for the general CE-&gt;1.21.1 deltas this class
 * relies on). Cross-checked structurally against {@code upstream/neo-edition}'s own real, compiling
 * {@code ItemRenderSPAS12.java} (which independently arrived at the same bus names/part names from
 * the same CE source) for the {@code RenderContext}-&gt;{@code PoseStack} idiom only, per this
 * task's ground rules - every number below is CE's, not Neo Edition's own (Neo Edition's port
 * itself has small first-person-offset differences from CE that were not carried over here).
 *
 * <p>Registered via {@link GunAnimationRegistration}, not here - see that class's own javadoc.
 */
public class ItemRenderSpas12 extends ItemRenderGunBase {

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
    }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        // CE reads its own per-frame-interpolated `interp` field here; this hook has no caller yet
        // in this port (see ItemRenderGunBase's own javadoc, "Aim-driven FOV zoom" - no
        // ViewportEvent.ComputeFov listener exists to feed a real partial-tick through), so the
        // un-interpolated current value is used instead - correct once a caller exists to interpolate.
        return fov * (1 - ItemGunBaseNT.aimingProgress * 0.33F);
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0.0, 0.0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(poseStack,
                -1.25F * offset, -1.75F * offset, -0.5F * offset,
                0, 0, 0);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        HbmObjModel model = GunModels.spas12();
        if (model == null) return;

        float scale = 0.5F;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F));

        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        poseStack.mulPose(Axis.XP.rotationDegrees((float) equip[0]));

        GunAnimationClientState.applyRelevantTransformation(poseStack, "MainBody");
        renderPart(model, poseStack, bufferSource, GunModels.SPAS12_TEX, packedLight, packedOverlay, "MainBody");

        poseStack.pushPose();
        GunAnimationClientState.applyRelevantTransformation(poseStack, "PumpGrip");
        renderPart(model, poseStack, bufferSource, GunModels.SPAS12_TEX, packedLight, packedOverlay, "PumpGrip");
        poseStack.popPose();

        poseStack.pushPose();
        GunAnimationClientState.applyRelevantTransformation(poseStack, "Shell");
        // Spent-casing tint not ported (SpentCasing/IMagazine.getCasing don't exist in this port
        // yet) - render at CE's own fixed brass default instead of the real per-ammo-type color.
        // See ItemRenderGunBase's own javadoc, "Spent-casing tint".
        int brass = 0xFF000000 | COLOR_CASE_BRASS;
        renderPart(model, poseStack, bufferSource, GunModels.CASINGS_TEX, packedLight, packedOverlay, brass, "Shell");
        renderPart(model, poseStack, bufferSource, GunModels.CASINGS_TEX, packedLight, packedOverlay, brass, "ShellFore");

        // Smoke-node trail not ported - see ItemRenderGunBase's own javadoc, "Smoke-node trail rendering".

        poseStack.pushPose();
        poseStack.translate(0.0, 1.5, -11.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90F * (float) gun.shotRand));
        renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 7.5);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        float scale = 1.75F;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0, -0.75, 0.0);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        float scale = 2F;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(25F));
        poseStack.mulPose(Axis.YP.rotationDegrees(45F));
        poseStack.translate(4.25, -0.5, 0.0);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay) {
        HbmObjModel model = GunModels.spas12();
        if (model == null) return;

        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
        renderPart(model, poseStack, bufferSource, GunModels.SPAS12_TEX, packedLight, packedOverlay, "MainBody");
        renderPart(model, poseStack, bufferSource, GunModels.SPAS12_TEX, packedLight, packedOverlay, "PumpGrip");
    }
}
