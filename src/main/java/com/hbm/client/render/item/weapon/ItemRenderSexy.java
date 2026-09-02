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
import com.hbm.util.BobMathUtil;

public class ItemRenderSexy extends ItemRenderGunBase {

    protected ResourceLocation texture;

    public ItemRenderSexy(ResourceLocation texture) {
        this.texture = texture;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

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
                -1F * offset, -0.75F * offset, 3F * offset,
                -0.5F, -0.5F, 2F);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        double scale = 0.375D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));

        boolean doesCycle = GunAnimationClientState.getRelevantAnim(0) != null && GunAnimationClientState.getRelevantAnim(0).animation.getBus("CYCLE") != null;
        boolean reloading = GunAnimationClientState.getRelevantAnim(0) != null && GunAnimationClientState.getRelevantAnim(0).animation.getBus("BELT") != null;
        boolean useShellCount = GunAnimationClientState.getRelevantAnim(0) != null && GunAnimationClientState.getRelevantAnim(0).animation.getBus("SHELLS") != null;
        boolean girldinner = GunAnimationClientState.getRelevantAnim(0) != null && GunAnimationClientState.getRelevantAnim(0).animation.getBus("BOTTLE") != null;
        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] lower = GunAnimationClientState.getRelevantTransformation("LOWER");
        double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
        double[] cycle = GunAnimationClientState.getRelevantTransformation("CYCLE");
        double[] barrel = GunAnimationClientState.getRelevantTransformation("BARREL");
        double[] hood = GunAnimationClientState.getRelevantTransformation("HOOD");
        double[] lever = GunAnimationClientState.getRelevantTransformation("LEVER");
        double[] belt = GunAnimationClientState.getRelevantTransformation("BELT");
        double[] mag = GunAnimationClientState.getRelevantTransformation("MAG");
        double[] magRot = GunAnimationClientState.getRelevantTransformation("MAGROT");
        double[] shellCount = GunAnimationClientState.getRelevantTransformation("SHELLS");
        double[] bottle = GunAnimationClientState.getRelevantTransformation("BOTTLE");
        double[] sippy = GunAnimationClientState.getRelevantTransformation("SIP");

        

        if (girldinner) {
            poseStack.pushPose();
            poseStack.translate((float) bottle[0], (float) bottle[1], (float) bottle[2]);
            poseStack.translate(0.0F, 2.0F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) sippy[0])));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (90.0F)));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) (-15.0F)));
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale((float)(1.5F), (float)(1.5F), (float)(1.5F));
            /* bind */ currentTex = GunModels.tex("whiskey_tex");
            renderAll(GunModels.obj("whiskey"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
            poseStack.popPose();
        }

        /* bind */ currentTex = texture;

        poseStack.translate(0.0F, -1.0F, -8.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
        poseStack.translate(0.0F, 1.0F, 8.0F);

        poseStack.translate(0.0F, 0.0F, -6.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) lower[0])));
        poseStack.translate(0.0F, 0.0F, 6.0F);

        poseStack.translate(0.0F, 0.0F, (float) recoil[2]);

        renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, (float) barrel[2]);
        renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, -0.375F);
        poseStack.scale((float)(1.0F), (float)(1.0F), (float)(1.0D + 0.457247371D * barrel[2]));
        poseStack.translate(0.0F, 0.0F, 0.375F);
        renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "RecoilSpring");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.4375F, -2.875F);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) hood[0])));
        poseStack.translate(0.0F, -0.4375F, 2.875F);
        renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hood");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.46875F, -6.875F);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (lever[2] * 60.0D))));
        poseStack.translate(0.0F, -0.46875F, 6.875F);
        renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lever");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, -6.75F);
        poseStack.scale((float)(1.0F), (float)(1.0F), (float)(1.0D - lever[2] * 0.25D));
        poseStack.translate(0.0F, 0.0F, 6.75F);
        renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "LockSpring");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate((float) mag[0], (float) mag[1], (float) mag[2]);
        poseStack.translate(0.0F, -1.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) ((float) magRot[2])));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");

        double p = 0.0625D;
        double x = p * 17;
        double y = p * -26;
        double angle = 0;
        Vec3NT vec = new Vec3NT(0, 0.4375, 0);

        double[] anglesLoaded = new double[]   {0,   0,  20,  20,  50, 60, 70};
        double[] anglesUnloaded = new double[] {0, -10, -50, -60, -60,  0,  0};
        double reloadProgress = !reloading ? 1D : belt[0];
        double cycleProgress = !doesCycle ? 1 : cycle[0];

        double[][] shells = new double[anglesLoaded.length][3];

        for (int i = 0; i < anglesLoaded.length; i++) {
            shells[i][0] = x;
            shells[i][1] = y;
            shells[i][2] = angle - 90;
            double delta = BobMathUtil.partialTick(anglesUnloaded[i], anglesLoaded[i], reloadProgress);
            angle += delta;
            vec.rotateAroundZDeg(-delta);
            x += vec.x;
            y += vec.y;
        }

        int shellAmount = useShellCount ? (int) shellCount[0] : gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);

        for (int i = 0; i < shells.length - 1; i++) {
            double[] prevShell = shells[i];
            double[] nextShell = shells[i + 1];
            renderShell(prevShell[0], nextShell[0], prevShell[1], nextShell[1], prevShell[2], nextShell[2], shells.length - i < shellAmount + 2, cycleProgress,
                    poseStack, bufferSource, currentTex, packedLight, packedOverlay);
        }
        poseStack.popPose();

        

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 8.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90.0F)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (90.0F * gun.shotRand)));
        renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 150, 7.5);
        poseStack.popPose();
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        double scale = 1.75D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.translate(1.0F, 1.0F, 6.0F);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        double scale = 1.375D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25.0F)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45.0F)));
        poseStack.translate(0.0F, 0.5F, 0.25F);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        

        
        /* bind */ currentTex = texture;
        renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
        renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
        renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "RecoilSpring");
        renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hood");
        renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lever");
        renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "LockSpring");
        renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");

        double p = 0.0625D;
        renderShell(p *  0, p *  -6,  90, true, poseStack, bufferSource, currentTex, packedLight, packedOverlay);
        renderShell(p *  5, p *   1,  30, true, poseStack, bufferSource, currentTex, packedLight, packedOverlay);
        renderShell(p * 12, p *  -1, -30, true, poseStack, bufferSource, currentTex, packedLight, packedOverlay);
        renderShell(p * 17, p *  -6, -60, true, poseStack, bufferSource, currentTex, packedLight, packedOverlay);
        renderShell(p * 17, p * -13, -90, true, poseStack, bufferSource, currentTex, packedLight, packedOverlay);
        renderShell(p * 17, p * -20, -90, true, poseStack, bufferSource, currentTex, packedLight, packedOverlay);

        
    }

    private void renderShell(double x0, double x1, double y0, double y1, double rot0, double rot1, boolean shell, double partialTick,
                             PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation currentTex, int packedLight, int packedOverlay) {
        renderShell(BobMathUtil.partialTick(x0, x1, partialTick), BobMathUtil.partialTick(y0, y1, partialTick),
                BobMathUtil.partialTick(rot0, rot1, partialTick), shell, poseStack, bufferSource, currentTex, packedLight, packedOverlay);
    }

    private void renderShell(double x, double y, double rot, boolean shell,
                             PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation currentTex, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(x, 0.375 + y, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (rot)));
        poseStack.translate(0, -0.375, 0);
        renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Belt");
        if (shell) renderPart(GunModels.obj("sexy"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Shell");
        poseStack.popPose();
    }
}
