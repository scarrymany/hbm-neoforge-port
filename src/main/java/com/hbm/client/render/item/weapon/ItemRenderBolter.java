package com.hbm.client.render.item.weapon;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.content.GunEnergyItems;
import com.hbm.items.weapon.sedna.content.GunHeavyItems;
import com.hbm.items.weapon.sedna.content.GunLauncherItems;
import com.hbm.items.weapon.sedna.content.GunPistolItems;
import com.hbm.items.weapon.sedna.content.GunRifleItems;
import com.hbm.items.weapon.sedna.content.GunShotgunItems;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.MainRegistry;
import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ItemRenderBolter extends ItemRenderGunBase {

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
        return fov * (1 - aimingProgress * 0.33F);
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(poseStack, -1.5F * offset, -2F * offset, 2.5F * offset, 0, -10.5 / 8D, 1.25);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        /* bind */ currentTex = GunModels.tex("bolter_tex");
        double scale = 0.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (180F)));
        double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (recoil[0] * 5))));
        poseStack.translate(0, 0, recoil[0]);

        double[] tilt = GunAnimationClientState.getRelevantTransformation("TILT");
        poseStack.translate(0, tilt[0], 3);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (tilt[0] * 35))));
        poseStack.translate(0, 0, -3);

        renderPart(GunModels.obj("bolter"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Body");

        double[] mag = GunAnimationClientState.getRelevantTransformation("MAG");
        poseStack.pushPose();
        poseStack.translate(0, 0, 5);
        poseStack.mulPose(Axis.XN.rotationDegrees((float) ((float) (mag[0] * 60 * (mag[2] == 1 ? 2.5 : 1)))));
        poseStack.translate(0, 0, -5);
        renderPart(GunModels.obj("bolter"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Mag");
        if (mag[2] != 1) renderPart(GunModels.obj("bolter"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bullet");
        poseStack.popPose();

        // TODO(CE:ItemRenderBolter.java:68-85) ammo FontRenderer overlay is 1.12 immediate-mode.

    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        double scale = 2.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.translate(0, -0.75, 1.25);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        double scale = 2.75D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25F)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45F)));
        poseStack.translate(-0.25, -0.5, 0);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

                poseStack.mulPose(Axis.YP.rotationDegrees((float) (180F)));

        /* bind */ currentTex = GunModels.tex("bolter_tex");
        renderAll(GunModels.obj("bolter"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);

            }
}
