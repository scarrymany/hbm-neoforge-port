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
import com.hbm.weapon.anim.GunAnimationType;

public class ItemRenderCongoLake extends ItemRenderGunBase {

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
        standardAimingTransform(poseStack,
                -1.5F * offset, -2F * offset, 1.25F * offset,
                0, -10 / 8D, 0.25);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        /* bind */ currentTex = GunModels.tex("congolake_tex");
        double scale = 0.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));

        GunAnimationClientState.applyRelevantTransformation(poseStack, "Gun");
        renderPart(GunModels.obj("congolake"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");

        poseStack.pushPose();
        GunAnimationClientState.applyRelevantTransformation(poseStack, "Pump");
        renderPart(GunModels.obj("congolake"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Pump");
        poseStack.popPose();

        poseStack.pushPose();
        float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
        GunAnimationClientState.applyRelevantTransformation(poseStack, "Sight");
        poseStack.translate(0, 2.125, 3);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (aimingProgress * -90)));
        poseStack.translate(0, -2.125, -3);
        renderPart(GunModels.obj("congolake"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Sight");
        poseStack.popPose();

        poseStack.pushPose();
        GunAnimationClientState.applyRelevantTransformation(poseStack, "Loop");
        renderPart(GunModels.obj("congolake"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Loop");
        poseStack.popPose();

        poseStack.pushPose();
        GunAnimationClientState.applyRelevantTransformation(poseStack, "GuardOuter");
        renderPart(GunModels.obj("congolake"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "GuardOuter");

        poseStack.pushPose();
        GunAnimationClientState.applyRelevantTransformation(poseStack, "GuardInner");
        renderPart(GunModels.obj("congolake"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "GuardInner");
        poseStack.popPose();
        poseStack.popPose();

        poseStack.pushPose();
        IMagazine<?> mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        // TODO(CE:ItemRenderCongoLake.java:86) SpentCasing.getColors / IMagazine.getCasing not ported.
        if (ItemGunBaseNT.getLastAnim(stack, 0) != GunAnimationType.INSPECT.ordinal() ||
                mag.getAmount(stack, clientInv()) > 0) {
            currentTex = GunModels.tex("casings_tex");
            GunAnimationClientState.applyRelevantTransformation(poseStack, "Shell");
            int brass = 0xFF000000 | COLOR_CASE_BRASS;
            renderPart(GunModels.obj("congolake"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, brass, "Shell");
            renderPart(GunModels.obj("congolake"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, brass, "ShellFore");
        }
        poseStack.popPose();

        double smokeScale = 0.25;

        
        poseStack.pushPose();
        poseStack.translate(0, 1.75, 4.25);
        double[] transform = GunAnimationClientState.getRelevantTransformation("Gun");
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) -transform[5])));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) ((float) -transform[4])));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) -transform[3])));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
        poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
        /* TODO(CE:ItemRenderCongoLake:smokeNodes) smokeNodes not ported */
        poseStack.popPose();
        

        poseStack.pushPose();
        poseStack.translate(0, 1.75, 4.25);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (90F * gun.shotRand)));
        poseStack.scale((float)(0.5), (float)(0.5), (float)(0.5));
        renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 150, 7.5);
        poseStack.popPose();
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        poseStack.translate(0, -2.5, 4);
        double scale = 2.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        double scale = 2.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25F)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45F)));
        poseStack.translate(0, -1.25, 0);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        

        
        /* bind */ currentTex = GunModels.tex("congolake_tex");
        renderAll(GunModels.obj("congolake"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
        
    }
}

