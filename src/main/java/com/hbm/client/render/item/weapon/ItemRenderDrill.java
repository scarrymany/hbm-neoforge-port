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
import com.hbm.items.weapon.sedna.mags.IMagazine;

public class ItemRenderDrill extends ItemRenderGunBase {

    @Override
    protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 0F : -0.5F; }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(poseStack,
                -1.25F * offset, -1.75F * offset, 1.75F * offset,
                -1F * offset, -1.75F * offset, 1.25F * offset);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        /* bind */ currentTex = GunModels.tex("drill_tex");
        double scale = 0.375D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));

        IMagazine mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        double gauge = (double) mag.getAmount(stack, null) / (double) mag.getCapacity(stack);

        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] deploy = GunAnimationClientState.getRelevantTransformation("DEPLOY");
        double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT");
        double[] spin = GunAnimationClientState.getRelevantTransformation("SPIN");

        poseStack.mulPose(Axis.YP.rotationDegrees((float) (15 * (1 - deploy[0] * 0.5))));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (-10 * (1 - deploy[0] * 0.5))));

        poseStack.translate(0, 2, -6);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (equip[0] * -45)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (equip[0] * -20)));
        poseStack.translate(0, -2, 6);

        poseStack.mulPose(Axis.XP.rotationDegrees((float) (lift[0])));

        poseStack.translate(0, 0, deploy[0]);

        
        renderPart(GunModels.obj("drill"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Base");

        poseStack.pushPose();
        poseStack.translate(1, 2.0625, -1.75);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (45)));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (-135 + gauge * 270)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (-45)));
        poseStack.translate(-1, -2.0625, 1.75);
        renderPart(GunModels.obj("drill"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gauge");
        poseStack.popPose();

        double rot = spin[0];
        double rot2 = rot * 5;

        poseStack.pushPose();
        poseStack.translate(0, Math.sin(rot2 * Math.PI / 180) * 0.125 - 0.125, 0);
        renderPart(GunModels.obj("drill"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Piston1");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, Math.sin(rot2 * Math.PI / 180 + Math.PI * 2D / 3D) * 0.125 - 0.125, 0);
        renderPart(GunModels.obj("drill"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Piston2");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, Math.sin(rot2 * Math.PI / 180 + Math.PI * 4D / 3D) * 0.125 - 0.125, 0);
        renderPart(GunModels.obj("drill"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Piston3");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZN.rotationDegrees((float) (rot)));
        renderPart(GunModels.obj("drill"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "DrillBack");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (rot)));
        renderPart(GunModels.obj("drill"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "DrillFront");
        poseStack.popPose();

        
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        double scale = 2.25D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.translate(1, -2, 6);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        double scale = 1.25D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
        poseStack.translate(-0.5, 0, 0);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        

        
        /* bind */ currentTex = GunModels.tex("drill_tex");
        renderAll(GunModels.obj("drill"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
        
    }
}

