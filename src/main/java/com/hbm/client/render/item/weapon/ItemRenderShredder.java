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

public class ItemRenderShredder extends ItemRenderGunBase {

    protected static String label = "[> <]";
    protected ResourceLocation texture;

    public ItemRenderShredder(ResourceLocation texture) {
        this.texture = texture;    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
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
        standardAimingTransform(poseStack, -1.5F * offset, -1.25F * offset, 1.5F * offset, 0, -6.25 / 8D, 0.5);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        // TODO(CE:1.12 leftover) EntityPlayer player = Minecraft.getMinecraft().player;
        double scale = 0.25D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));

        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT");
        double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
        double[] mag = GunAnimationClientState.getRelevantTransformation("MAG");
        double[] speen = GunAnimationClientState.getRelevantTransformation("SPEEN");
        double[] cycle = GunAnimationClientState.getRelevantTransformation("CYCLE");

        poseStack.translate(0, -2, -6);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
        poseStack.translate(0, 2, 6);

        poseStack.translate(0, 0, -4);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) lift[0])));
        poseStack.translate(0, 0, 4);

        poseStack.translate(0, 0, recoil[2]);

        boolean sexy = stack.getItem() == GunShotgunItems.GUN_AUTOSHOTGUN_SEXY.get();

        // TODO(CE:ItemRenderShredder.java:75-101) ADS FontRenderer label is 1.12 immediate-mode.

        /* bind */ currentTex = texture;
        renderPart(GunModels.obj("shredder"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");

        poseStack.pushPose();
        poseStack.translate(mag[0], mag[1], mag[2]);
        poseStack.translate(0, -1, -0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) speen[0])));
        poseStack.translate(0, 1, 0.5);
        renderPart(GunModels.obj("shredder"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
        poseStack.translate(0, -1, -0.5);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) cycle[2])));
        poseStack.translate(0, 1, 0.5);
        renderPart(GunModels.obj("shredder"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Shells");
        poseStack.popPose();

        double smokeScale = 0.75;

        poseStack.pushPose();
        poseStack.translate(0, 1, 7.5);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
        poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
        /* TODO(CE:ItemGunBaseNT.java:329) smokeNodes not ported */
        poseStack.popPose();

        // Temporarily switch to flat for muzzle flash, then restore to previous shade
                poseStack.pushPose();
        poseStack.translate(0, 1, 7.5);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (gun.shotRand * 90)));
        poseStack.scale((float)(0.75), (float)(0.75), (float)(0.75));
        renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 7.5);
        poseStack.popPose();

        // Restore original shade model
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        double scale = 1.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.translate(0, 0.5, 4);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        double scale = 1.25D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
        poseStack.translate(-1.5, 0, 0);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        
        /* bind */ currentTex = texture;
        renderAll(GunModels.obj("shredder"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
    }
}
