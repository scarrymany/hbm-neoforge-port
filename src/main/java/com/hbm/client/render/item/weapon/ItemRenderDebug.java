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

public class ItemRenderDebug extends ItemRenderGunBase {

    @Override
    protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0, 0, 1);

        float offset = 0.8F;
        standardAimingTransform(poseStack,
                -1.0F * offset, -0.75F * offset, 1F * offset,
                0, -3.875 / 8D, 0);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

        double scale = 0.125D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));

        double[] equipSpin = GunAnimationClientState.getRelevantTransformation("ROTATE");
        double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
        double[] reloadLift = GunAnimationClientState.getRelevantTransformation("RELOAD_LIFT");
        double[] reloadJolt = GunAnimationClientState.getRelevantTransformation("RELOAD_JOLT");
        double[] reloadTilt = GunAnimationClientState.getRelevantTransformation("RELAOD_TILT");
        double[] cylinderFlip = GunAnimationClientState.getRelevantTransformation("RELOAD_CYLINDER");
        double[] reloadBullets = GunAnimationClientState.getRelevantTransformation("RELOAD_BULLETS");

        poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float)equipSpin[0])));

        standardAimingTransform(poseStack, 0, 0, recoil[2], -recoil[2], 0, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float)(recoil[2] * 10))));

        

        poseStack.pushPose();
        poseStack.translate(-9, 2.5, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float)(recoil[2] * -10))));
        /* TODO(CE:ItemRenderDebug:smokeNodes) smokeNodes not ported */
        poseStack.popPose();

        poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float)reloadLift[0])));
        poseStack.translate(reloadJolt[0], 0, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float)reloadTilt[0])));

        /* bind */ currentTex = GunModels.tex("debug_gun_tex");
        renderPart(GunModels.obj("lilmac"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float)cylinderFlip[0])));
        renderPart(GunModels.obj("lilmac"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Pivot");
        poseStack.translate(0, 1.75, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float)(GunAnimationClientState.getRelevantTransformation("DRUM")[2] * -60))));
        poseStack.translate(0, -1.75, 0);
        renderPart(GunModels.obj("lilmac"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Cylinder");
        poseStack.translate(reloadBullets[0], reloadBullets[1], reloadBullets[2]);
        if(GunAnimationClientState.getRelevantTransformation("RELOAD_BULLETS_CON")[0] != 1)
            renderPart(GunModels.obj("lilmac"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bullets");
        renderPart(GunModels.obj("lilmac"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Casings");
        poseStack.popPose();

        poseStack.pushPose(); /// HAMMER ///
        poseStack.translate(4, 1.25, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float)(-30 + 30 * GunAnimationClientState.getRelevantTransformation("HAMMER")[2]))));
        poseStack.translate(-4, -1.25, 0);
        renderPart(GunModels.obj("lilmac"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hammer");
        poseStack.popPose();

        

        poseStack.pushPose();
        poseStack.translate(0.125, 2.5, 0);
        renderGapFlash(poseStack, bufferSource, gun.lastShot[0]);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-9.5, 2.5, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (90 * gun.shotRand))));
        //renderMuzzleFlash(poseStack, bufferSource, gun.lastShot);
        poseStack.popPose();
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        poseStack.scale((float)(0.75), (float)(0.75), (float)(0.75));
        poseStack.translate(0, 1, 3);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        double scale = 1.25D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
        
        // TODO(CE:1.12 leftover) GlStateManager.enableAlpha();

        
        /* bind */ currentTex = GunModels.tex("debug_gun_tex");
        renderPart(GunModels.obj("lilmac"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
        renderPart(GunModels.obj("lilmac"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Cylinder");
        renderPart(GunModels.obj("lilmac"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bullets");
        renderPart(GunModels.obj("lilmac"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Casings");
        renderPart(GunModels.obj("lilmac"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Pivot");
        renderPart(GunModels.obj("lilmac"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hammer");
        
    }
}
