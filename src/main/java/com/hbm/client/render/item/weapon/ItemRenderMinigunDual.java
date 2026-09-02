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

public class ItemRenderMinigunDual extends ItemRenderGunBase {

    @Override public boolean isAkimbo() { return true; }

    @Override
    protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
        return  fov * (1 - aimingProgress * 0.33F);
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0.0F, 0.0F, 0.875F);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

        float offset = 0.8F;

        for (int i = -1; i <= 1; i += 2) {
            int index = i == -1 ? 0 : 1;
            /* bind */ currentTex = GunModels.tex("minigun_dual_tex");

            poseStack.pushPose();
            standardAimingTransform(poseStack, -2.75F * offset * i, -1.75F * offset, 2.5F * offset, 0, 0, 0);

            double scale = 0.375D;
            poseStack.scale((float)(scale), (float)(scale), (float)(scale));

            double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP", index);
            double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL", index);
            double[] rotate = GunAnimationClientState.getRelevantTransformation("ROTATE", index);

            poseStack.translate(0.0F, 3.0F, -6.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
            poseStack.translate(0.0F, -3.0F, 6.0F);

            poseStack.translate(0.0F, 0.0F, (float) recoil[2]);

            

            renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, index == 0 ? "GunDual" : "Gun");

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees((float) ((float) (rotate[2] * i))));
            renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrels");
            poseStack.popPose();

            

            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 12.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (90.0F)));

            poseStack.mulPose(Axis.XP.rotationDegrees((float) (gun.shotRand * 90.0F)));
            poseStack.scale((float)(1.5F), (float)(1.5F), (float)(1.5F));
            renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[index], 75, 5);
            poseStack.popPose();

            poseStack.popPose();
        }
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        double scale = 1.75D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.translate(-1.0F, -3.5F, 8.0F);
    }

    @Override
    public void setupThirdPersonAkimbo(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        double scale = 1.75D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.translate(2.0F, -3.5F, 8.0F);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        poseStack.scale((float)(1.0F), (float)(1.0F), (float)(-1.0F));
        poseStack.translate(8.0F, 8.0F, 0.0F);
        double scale = 0.875D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
    }

    public void renderEquipped(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        
        /* bind */ currentTex = GunModels.tex("minigun_dual_tex");
        renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
        renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrels");
        
    }

    public void renderEquippedAkimbo(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        
        /* bind */ currentTex = GunModels.tex("minigun_dual_tex");
        renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "GunDual");
        renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrels");
        
    }

    public void renderModTable(ItemStack stack, int index, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        

        
        /* bind */ currentTex = GunModels.tex("minigun_dual_tex");
        renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, index == 0 ? "GunDual" : "Gun");
        renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrels");
        
    }

    public void renderInv(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        
        

        /* bind */ currentTex = GunModels.tex("minigun_dual_tex");

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (225.0F)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90.0F)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45.0F)));
        renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "GunDual");
        renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrels");
        poseStack.popPose();

        poseStack.translate(0.0F, 0.0F, 8.0F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (225.0F)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (-90.0F)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (-90.0F)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (-45.0F)));
        renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
        renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrels");
        poseStack.popPose();

        
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        

        
        /* bind */ currentTex = GunModels.tex("minigun_dual_tex");
        renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
        renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrels");
        
    }

    public boolean hasSilencer(ItemStack stack, int cfg) {
        return XWeaponModManager.hasUpgrade(stack, cfg, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "silencer"));
    }

    public boolean isSaturnite(ItemStack stack, int cfg) {
        return XWeaponModManager.hasUpgrade(stack, cfg, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "uzi_saturnite"));
    }
}
