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

public class ItemRenderGreasegun extends ItemRenderGunBase {

    @Override
    protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
        return  fov * (1 - aimingProgress * 0.33F);
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(poseStack,
                -1.5F * offset, -1F * offset, 1.75F * offset,
                0, -2.625 / 8D, 1.125);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        // TODO(CE:1.12 leftover) Minecraft.getMinecraft().renderEngine.bindTexture(isRefurbished(stack) ? GunModels.tex("greasegun_clean_tex") : GunModels.tex("greasegun_tex"));
        double scale = 0.375D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));

        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] stock = GunAnimationClientState.getRelevantTransformation("STOCK");
        double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
        double[] flap = GunAnimationClientState.getRelevantTransformation("FLAP");
        double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT");
        double[] handle = GunAnimationClientState.getRelevantTransformation("HANDLE");
        double[] mag = GunAnimationClientState.getRelevantTransformation("MAG");
        double[] turn = GunAnimationClientState.getRelevantTransformation("TURN");
        double[] bullet = GunAnimationClientState.getRelevantTransformation("BULLET");

        poseStack.translate(0, -3, -3);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (equip[0])));
        poseStack.translate(0, 3, 3);

        poseStack.translate(0, -3, -3);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (lift[0])));
        poseStack.translate(0, 3, 3);

        if(ItemGunBaseNT.aimingProgress < 1F) poseStack.mulPose(Axis.ZP.rotationDegrees((float) (turn[2])));

        poseStack.translate(0, 0, recoil[2]);

        
        renderPart(GunModels.obj("greasegun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");

        poseStack.pushPose();
        poseStack.translate(0, 0, -4 - stock[2]);
        renderPart(GunModels.obj("greasegun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Stock");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(mag[0], mag[1], mag[2]);
        renderPart(GunModels.obj("greasegun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
        if(bullet[0] != 1) renderPart(GunModels.obj("greasegun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bullet");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, -1.4375, -0.125);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (handle[0])));
        poseStack.translate(0, 1.4375, 0.125);
        renderPart(GunModels.obj("greasegun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Handle");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.53125, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (flap[2])));
        poseStack.translate(0, -0.5125, 0);
        renderPart(GunModels.obj("greasegun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Flap");
        poseStack.popPose();

        double smokeScale = 0.25;

        poseStack.pushPose();
        poseStack.translate(-0.25, 0, 1.5);
        poseStack.mulPose(Axis.ZN.rotationDegrees((float) (turn[2])));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
        poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
        /* TODO(CE:ItemGunBaseNT.java:329) smokeNodes not ported */
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0, 8);
        poseStack.mulPose(Axis.ZN.rotationDegrees((float) (turn[2])));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
        poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
        /* TODO(CE:ItemGunBaseNT.java:329) smokeNodes not ported */
        poseStack.popPose();

        

        poseStack.pushPose();
        poseStack.translate(0, 0, 8);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        poseStack.scale((float)(0.5), (float)(0.5), (float)(0.5));
        renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 7.5);
        poseStack.popPose();
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        poseStack.translate(0, 1, 3);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        double scale = 1.5D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
        poseStack.translate(-0.5, 2, 0);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        

        
        // TODO(CE:1.12 leftover) Minecraft.getMinecraft().renderEngine.bindTexture(isRefurbished(stack) ? GunModels.tex("greasegun_clean_tex") : GunModels.tex("greasegun_tex"));
        renderAll(GunModels.obj("greasegun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
        
    }

    public boolean isRefurbished(ItemStack stack) {
        return XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "id_greasegun_clean"));
    }
}
