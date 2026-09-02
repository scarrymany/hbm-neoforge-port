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

public class ItemRenderMissileLauncher extends ItemRenderGunBase {

    protected static String label = "AUTO";

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(poseStack, -1.5F * offset, -1.25F * offset, 0.5F * offset, -1F * offset, -1.25F * offset, 0F * offset);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        // TODO(CE:1.12 leftover) EntityPlayer player = Minecraft.getMinecraft().player;
        /* bind */ currentTex = GunModels.tex("missile_launcher_tex");
        double scale = 0.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));

        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] barrel = GunAnimationClientState.getRelevantTransformation("BARREL");
        double[] open = GunAnimationClientState.getRelevantTransformation("OPEN");
        double[] missile = GunAnimationClientState.getRelevantTransformation("MISSILE");

        poseStack.translate(0, -2, -2);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
        poseStack.translate(0, 2, 2);

        renderPart(GunModels.obj("missile_launcher"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Launcher");

        poseStack.pushPose();

        poseStack.translate(0, 0.25, 1.6875);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) open[0])));
        poseStack.translate(0, -0.25, -1.6875);

        renderPart(GunModels.obj("missile_launcher"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Front");

        poseStack.pushPose();
        poseStack.translate(0, 0, barrel[2]);
        renderPart(GunModels.obj("missile_launcher"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(missile[0], missile[1], missile[2]);
        renderPart(GunModels.obj("missile_launcher"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Missile");
        poseStack.popPose();

        poseStack.popPose();

        // TODO(CE:ItemRenderMissileLauncher.java:78-105) ADS FontRenderer label is 1.12 immediate-mode.


        poseStack.pushPose();
        poseStack.translate(0, 1, 6.75);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (gun.shotRand * 90)));
        poseStack.scale((float)(0.75), (float)(0.75), (float)(0.75));
        renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 7.5);
        poseStack.popPose();
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        double scale = 2.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.translate(0, -0.5, -2);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        double scale = 1.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
        poseStack.translate(0, -0.5, 0);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        

        /* bind */ currentTex = GunModels.tex("missile_launcher_tex");
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();


        renderPart(GunModels.obj("missile_launcher"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Launcher");
        renderPart(GunModels.obj("missile_launcher"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
        renderPart(GunModels.obj("missile_launcher"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Front");
        if (gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) > 0)
            renderPart(GunModels.obj("missile_launcher"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Missile");
    }
}
