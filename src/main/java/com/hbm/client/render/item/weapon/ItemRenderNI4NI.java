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

public class ItemRenderNI4NI extends ItemRenderGunBase {

    @Override
    protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
        return  fov * (1 - aimingProgress * 0.33F);
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0.0D, 0.0D, 1.0D);

        float offset = 0.8F;
        standardAimingTransform(poseStack,
                -1.0F * offset, -1F * offset, offset,
                0, -5 / 8D, 0.125);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        // TODO(CE:ItemGunNI4NI) per-stack dye/coin NBT not ported; default CE texture.
        currentTex = GunModels.tex("n_i_4_n_i_tex");

        double scale = 0.3125D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));

        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
        double[] drum = GunAnimationClientState.getRelevantTransformation("DRUM");

        poseStack.translate(0.0D, 0.0D, -2.25D);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
        poseStack.translate(0.0D, 0.0D, 2.25D);

        poseStack.translate(0.0D, -1.0D, -6.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) recoil[0])));
        poseStack.translate(0.0D, 1.0D, 6.0D);

        

        poseStack.pushPose();

        // TODO(CE:1.12 leftover) GlStateManager.color(ColorUtil.fr(dark), ColorUtil.fg(dark), ColorUtil.fb(dark));
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "FrameDark");

        // TODO(CE:1.12 leftover) GlStateManager.color(ColorUtil.fr(grip), ColorUtil.fg(grip), ColorUtil.fb(grip));
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Grip");

        // TODO(CE:1.12 leftover) GlStateManager.color(ColorUtil.fr(light), ColorUtil.fg(light), ColorUtil.fb(light));
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "FrameLight");

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.1875D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) ((float) drum[2])));
        poseStack.translate(0.0D, -1.1875D, 0.0D);
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Cylinder");
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "CylinderHighlights");
        poseStack.popPose();

        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
        // TODO(CE:ItemGunNI4NI) coin-count tint uses ItemGunNI4NI + GlStateManager.color.
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Coin1");
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Coin2");
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Coin3");
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Coin4");

        

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.75D, 4.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90.0F)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (90.0F * (float) gun.shotRand)));
        poseStack.scale((float)(0.125D), (float)(0.125D), (float)(0.125D));
        renderLaserFlash(poseStack, bufferSource, gun.lastShot[0], 75, 7.5, 0xFFFFFF);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        poseStack.translate(0.0D, 0.25D, 3.0D);
        double scale = 1.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        double scale = 2.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25.0F)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45.0F)));
        poseStack.translate(0.0D, 0.0D, 0.0D);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = GunModels.tex("n_i_4_n_i_tex");
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "FrameLight");
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Cylinder");
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Grip");
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "FrameDark");
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "CylinderHighlights");
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Coin1");
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Coin2");
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Coin3");
        renderPart(GunModels.obj("n_i_4_n_i"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Coin4");
    }
}
