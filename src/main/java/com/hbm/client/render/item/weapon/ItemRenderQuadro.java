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

public class ItemRenderQuadro extends ItemRenderGunBase {

    protected static String label = ">> <<";

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(poseStack, -2.5F * offset, -3.5F * offset, 2.5F * offset, -1.5F * offset, -3F * offset, 2.5F * offset);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        // TODO(CE:1.12 leftover) EntityPlayer player = Minecraft.getMinecraft().player;

        double scale = 1.75D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));

        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
        double[] reloadPush = GunAnimationClientState.getRelevantTransformation("RELOAD_PUSH");
        double[] reloadRotate = GunAnimationClientState.getRelevantTransformation("RELOAD_ROTATE");

        poseStack.translate(0, -1, -1);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
        poseStack.translate(0, 1, 1);

        poseStack.translate(0, 0, recoil[2]);

        poseStack.translate(0, -1, -1);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) reloadRotate[2])));
        poseStack.translate(0, 1, 1);


        /* bind */ currentTex = GunModels.tex("quadro_tex");
        renderPart(GunModels.obj("quadro"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Launcher");

        poseStack.pushPose();
        poseStack.translate(0, -1, 0);
        poseStack.translate(0, 3, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (reloadPush[1] * 30))));
        poseStack.translate(0, -3, 0);
        poseStack.translate(0, 0, reloadPush[0] * 3);
        /* bind */ currentTex = GunModels.tex("quadro_rocket_tex");
        renderPart(GunModels.obj("quadro"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Rockets");
        poseStack.popPose();
        // TODO(CE:ItemRenderQuadro.java:74-99) ADS FontRenderer label is 1.12 immediate-mode.

        poseStack.pushPose();
        poseStack.translate(-1, 0.75, 6.5);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        poseStack.scale((float)(0.75), (float)(0.75), (float)(0.75));
        renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 150, 7.5);
        poseStack.popPose();
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        double scale = 7.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.translate(0, -0.5, -0.25);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        double scale = 4.75D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
        poseStack.translate(0, -1, 0);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        
        /* bind */ currentTex = GunModels.tex("quadro_tex");
        renderPart(GunModels.obj("quadro"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Launcher");

    }
}
