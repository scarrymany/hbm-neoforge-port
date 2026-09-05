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

public class ItemRenderFolly extends ItemRenderGunBase {

    public static long timeAiming;
    public static boolean jingle = false;
    public static boolean wasAiming = false;

    @Override
    protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2F : 2.5F; }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
        return fov * (1 - aimingProgress * 0.33F);
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0, 0, 0.875);

        float offset = 0.8F;
        float aim = 0.75F;
        standardAimingTransform(poseStack,
                -2.5F * offset, -1.5F * offset, 2.75F * offset,
                -2 * aim, -1 * aim, 2.25F * offset);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        // TODO(CE:1.12 leftover) EntityPlayer player = Minecraft.getMinecraft().player;
        /* bind */ currentTex = GunModels.tex("folly_tex");
        double scale = 0.75D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));

        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
        double[] load = GunAnimationClientState.getRelevantTransformation("LOAD");
        double[] shell = GunAnimationClientState.getRelevantTransformation("SHELL");
        double[] screw = GunAnimationClientState.getRelevantTransformation("SCREW");
        double[] breech = GunAnimationClientState.getRelevantTransformation("BREECH");

        poseStack.translate(0, 1, -4);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (-equip[0])));
        poseStack.translate(0, -1, 4);

        poseStack.translate(0, -2, -2);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (load[0])));
        poseStack.translate(0, 2, 2);

        

        renderPart(GunModels.obj("folly"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Cannon");

        poseStack.pushPose();
        poseStack.translate(recoil[0], recoil[1], recoil[2]);
        renderPart(GunModels.obj("folly"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(shell[0], shell[1], shell[2]);
        renderPart(GunModels.obj("folly"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Shell");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(breech[0], breech[1], breech[2]);
        renderPart(GunModels.obj("folly"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Breech");
        poseStack.translate(0, 1, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (screw[2])));
        poseStack.translate(0, -1, 0);
        renderPart(GunModels.obj("folly"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Cog");
        poseStack.popPose();

        boolean isAiming = gun.prevAimingProgress >= 1F && gun.aimingProgress >= 1F;
        if (isAiming & !wasAiming) timeAiming = System.currentTimeMillis();

        if (!isAiming) {
            jingle = false;
        }
        // TODO(CE:ItemRenderFolly.java:98-165) ADS FontRenderer TTY overlay is 1.12 immediate-mode.

        wasAiming = isAiming;

        
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        double scale = 3D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.translate(-0.25, 0.5, 3);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        double scale = 1.25D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
        poseStack.translate(0, -0.5, 0);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        

        
        /* bind */ currentTex = GunModels.tex("folly_tex");
        renderAll(GunModels.obj("folly"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
        
    }

    // TODO(CE:ItemRenderFolly.java:137-180) ADS TTY overlay helpers unused until FontRenderer ports.
    @SuppressWarnings("unused")
    public static String getBootSplash() {
        long now = System.currentTimeMillis();
        if (timeAiming + 5000 < now) return "";
        if (timeAiming + 3000 > now) return "";
        int splashIndex = (int) ((now - timeAiming - 3000) * 35 / 2000) - 10;
        char[] letters = "VStarOS".toCharArray();
        String splash = "";
        for (int i = 0; i < letters.length; i++) {
            if (i < splashIndex - 1) splash += net.minecraft.ChatFormatting.LIGHT_PURPLE;
            if (i == splashIndex - 1) splash += net.minecraft.ChatFormatting.AQUA;
            if (i == splashIndex) splash += net.minecraft.ChatFormatting.WHITE;
            if (i == splashIndex + 1) splash += net.minecraft.ChatFormatting.AQUA;
            if (i == splashIndex + 2) splash += net.minecraft.ChatFormatting.LIGHT_PURPLE;
            if (i > splashIndex + 2) splash += net.minecraft.ChatFormatting.BLACK;
            splash += letters[i];
        }
        return splash;
    }

    @SuppressWarnings("unused")
    public static java.util.List<String> getTTY() {
        return java.util.List.of();
    }
}
