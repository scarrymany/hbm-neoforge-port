package com.hbm.client.render.item.weapon;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.MainRegistry;
import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Port of CE's {@code com.hbm.render.item.weapon.sedna.ItemRenderUzi} (186 lines, read in full) -
 * the {@code gun_uzi}/{@code gun_uzi_akimbo} renderer. Every transform value/part/bus name below is
 * copied verbatim from that class. Unlike SPAS-12/AM-180, the Uzi has <b>no dedicated animation
 * JSON</b> among CE's 12 shipped {@code models/weapons/animations/*.json} files - its animation
 * lambda ({@code XFactory9mm.LAMBDA_UZI_ANIMS}, ported verbatim as
 * {@link GunAnimationRegistration#UZI_ANIM}) builds every {@link com.hbm.render.anim.sedna.BusAnimationSedna}
 * programmatically instead, via {@code BusAnimationSequenceSedna.addPos(...)} calls - see that
 * field's own javadoc for the confirmation this is CE's real, intentional design (not a placeholder).
 *
 * <p>CE's {@code GlStateManager.shadeModel(GL_SMOOTH/GL_FLAT)} calls (fixed-pipeline flat-vs-smooth
 * shading toggles around different part groups) have no 1.21.1 equivalent call and are not ported -
 * modern rendering derives per-fragment lighting from the mesh's own vertex normals
 * ({@link com.hbm.render.loader.HbmObjModel}'s parser already computes/reads these, per that
 * class's own javadoc), which is strictly more correct than a fixed-pipeline shading-model toggle
 * and needs no manual equivalent.
 */
public class ItemRenderUzi extends ItemRenderGunBase {

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        // See ItemRenderSpas12.getViewFOV's own comment - no interpolated-partial-tick caller exists yet.
        return fov * (1 - ItemGunBaseNT.aimingProgress * 0.33F);
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0.0, 0.0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(poseStack,
                -1.75F * offset, -1.5F * offset, 2.5F * offset,
                0, -4.375 / 8D, 1);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        HbmObjModel model = GunModels.uzi();
        if (model == null) return;

        ResourceLocation tex = isSaturnite(stack) ? GunModels.UZI_SATURNITE_TEX : GunModels.UZI_TEX;

        float scale = 0.25F;
        poseStack.scale(scale, scale, scale);

        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] stockFront = GunAnimationClientState.getRelevantTransformation("STOCKFRONT");
        double[] stockBack = GunAnimationClientState.getRelevantTransformation("STOCKBACK");
        double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
        double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT");
        double[] mag = GunAnimationClientState.getRelevantTransformation("MAG");
        double[] bullet = GunAnimationClientState.getRelevantTransformation("BULLET");
        double[] slide = GunAnimationClientState.getRelevantTransformation("SLIDE");
        double[] yeet = GunAnimationClientState.getRelevantTransformation("YEET");
        double[] speen = GunAnimationClientState.getRelevantTransformation("SPEEN");

        poseStack.translate(yeet[0], yeet[1], yeet[2]);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) speen[0]));

        poseStack.translate(0.0, -2.0, -4.0);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        poseStack.translate(0.0, 2.0, 4.0);

        poseStack.translate(0.0, 0.0, -6.0);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) lift[0]));
        poseStack.translate(0.0, 0.0, 6.0);

        poseStack.translate(0.0, 0.0, recoil[2]);

        renderPart(model, poseStack, bufferSource, tex, packedLight, packedOverlay, "Gun");

        boolean silenced = hasSilencer(stack, 0);
        if (silenced) renderPart(model, poseStack, bufferSource, tex, packedLight, packedOverlay, "Silencer");

        poseStack.pushPose();
        poseStack.translate(0.0, 0.3125, -5.75);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (180 - stockFront[0])));
        poseStack.translate(0.0, -0.3125, 5.75);
        renderPart(model, poseStack, bufferSource, tex, packedLight, packedOverlay, "StockFront");

        poseStack.translate(0.0, -0.3125, -3.0);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (-200 - stockBack[0])));
        poseStack.translate(0.0, 0.3125, 3.0);
        renderPart(model, poseStack, bufferSource, tex, packedLight, packedOverlay, "StockBack");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0, 0.0, slide[2]);
        renderPart(model, poseStack, bufferSource, tex, packedLight, packedOverlay, "Slide");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(mag[0], mag[1], mag[2]);
        renderPart(model, poseStack, bufferSource, tex, packedLight, packedOverlay, "Magazine");
        if (bullet[0] == 1) renderPart(model, poseStack, bufferSource, tex, packedLight, packedOverlay, "Bullet");
        poseStack.popPose();

        if (!silenced) {
            // Smoke-node trail not ported - see ItemRenderGunBase's own javadoc.

            poseStack.pushPose();
            poseStack.translate(0.0, 0.75, 8.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(90F));
            poseStack.mulPose(Axis.XP.rotationDegrees(90F * (float) gun.shotRand));
            renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 7.5);
            poseStack.popPose();
        }
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        poseStack.translate(0.0, 1.0, 1.0);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        float scale = 1.5F;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(25F));
        poseStack.mulPose(Axis.YP.rotationDegrees(45F));
        poseStack.translate(0.0, 1.0, 0.0);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay) {
        HbmObjModel model = GunModels.uzi();
        if (model == null) return;

        ResourceLocation tex = isSaturnite(stack) ? GunModels.UZI_SATURNITE_TEX : GunModels.UZI_TEX;
        boolean silenced = hasSilencer(stack, 0);

        if (silenced && ctx == ItemDisplayContext.GUI) {
            float scale = 0.625F;
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0.0, 0.0, -4.0);
        }

        renderPart(model, poseStack, bufferSource, tex, packedLight, packedOverlay, "Gun");
        renderPart(model, poseStack, bufferSource, tex, packedLight, packedOverlay, "StockBack");
        renderPart(model, poseStack, bufferSource, tex, packedLight, packedOverlay, "StockFront");
        renderPart(model, poseStack, bufferSource, tex, packedLight, packedOverlay, "Slide");
        renderPart(model, poseStack, bufferSource, tex, packedLight, packedOverlay, "Magazine");
        if (silenced) renderPart(model, poseStack, bufferSource, tex, packedLight, packedOverlay, "Silencer");
    }

    /** {@code XWeaponModManager}'s silencer mod id ({@code new WeaponModSilencer("silencer")}, registered in that class's static init - see {@code WeaponModItems.ModCaliber} attachment tables) - this port uses namespaced {@link ResourceLocation} ids instead of CE's raw {@code int} constants (see {@code IWeaponMod}'s own javadoc), so there is no {@code XWeaponModManager.ID_SILENCER}-style constant to reference directly. */
    private static final ResourceLocation SILENCER_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "silencer");
    /** {@code new WeaponModUziSaturnite("uzi_saturnite")}'s registered id - see {@link #SILENCER_ID}'s own note. */
    private static final ResourceLocation UZI_SATURNITE_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "uzi_saturnite");

    public boolean hasSilencer(ItemStack stack, int cfg) {
        return XWeaponModManager.hasUpgrade(stack, cfg, SILENCER_ID);
    }

    public boolean isSaturnite(ItemStack stack) {
        return XWeaponModManager.hasUpgrade(stack, 0, UZI_SATURNITE_ID);
    }
}
