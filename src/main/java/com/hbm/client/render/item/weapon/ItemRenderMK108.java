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

public class ItemRenderMK108 extends ItemRenderGunBase {

    @Override
    protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

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
                -1F * offset, -1.5F * offset, 2.5F * offset,
                -0.75F, -0.75F, 1.5F);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        /* bind */ currentTex = GunModels.tex("mk108_tex");
        double scale = 0.375D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));

        boolean doesYeet = GunAnimationClientState.getRelevantAnim(0) != null && GunAnimationClientState.getRelevantAnim(0).animation.getBus("GRENH1") != null;
        boolean doesCycle = GunAnimationClientState.getRelevantAnim(0) != null && GunAnimationClientState.getRelevantAnim(0).animation.getBus("CYCLE") != null;
        boolean reloading = GunAnimationClientState.getRelevantAnim(0) != null && GunAnimationClientState.getRelevantAnim(0).animation.getBus("BELT") != null;
        boolean useShellCount = GunAnimationClientState.getRelevantAnim(0) != null && GunAnimationClientState.getRelevantAnim(0).animation.getBus("SHELLS") != null;
        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] cycle = GunAnimationClientState.getRelevantTransformation("CYCLE");
        double[] barrel = GunAnimationClientState.getRelevantTransformation("BARREL");
        double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
        double[] lid = GunAnimationClientState.getRelevantTransformation("LID");
        double[] belt = GunAnimationClientState.getRelevantTransformation("BELT");
        double[] drum = GunAnimationClientState.getRelevantTransformation("DRUM");
        double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT");
        double[] shellCount = GunAnimationClientState.getRelevantTransformation("SHELLS");

        

        if(doesYeet) {
            double[][] horizontal = new double[][] {
                    GunAnimationClientState.getRelevantTransformation("GRENH1"),
                    GunAnimationClientState.getRelevantTransformation("GRENH2"),
                    GunAnimationClientState.getRelevantTransformation("GRENH3"),
            };
            double[][] vertical = new double[][] {
                    GunAnimationClientState.getRelevantTransformation("GRENV1"),
                    GunAnimationClientState.getRelevantTransformation("GRENV2"),
                    GunAnimationClientState.getRelevantTransformation("GRENV3"),
            };
            double[][] spin = new double[][] {
                    GunAnimationClientState.getRelevantTransformation("GRENS1"),
                    GunAnimationClientState.getRelevantTransformation("GRENS2"),
                    GunAnimationClientState.getRelevantTransformation("GRENS3"),
            };

            for(int i = 0; i < 3; i++) {
                if(horizontal[i][0] <= -4) continue;
                poseStack.pushPose();
                poseStack.translate(horizontal[i][0], vertical[i][1], 0);
                poseStack.translate(0, 0, -2.3125);
                poseStack.mulPose(Axis.XP.rotationDegrees((float) (-90)));
                poseStack.mulPose(Axis.YP.rotationDegrees((float) ((float) -spin[i][0])));
                poseStack.translate(0, 0, 2.3125);
                renderPart(GunModels.obj("mk108"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Grenade");
                poseStack.popPose();
            }
        }

        poseStack.translate(0, -1, -8);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
        poseStack.translate(0, 1, 8);

        poseStack.translate(0, 1, -4);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) lift[0])));
        poseStack.translate(0, -1, 4);

        poseStack.translate(0, 0, recoil[2]);

        renderPart(GunModels.obj("mk108"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");

        poseStack.pushPose();
        poseStack.translate(0, 0, barrel[2] * 2);
        renderPart(GunModels.obj("mk108"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0.6875, -1);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) lid[0])));
        poseStack.translate(0, -0.6875, 1);
        renderPart(GunModels.obj("mk108"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lid");
        poseStack.popPose();

        poseStack.pushPose();

        poseStack.translate(drum[0], drum[1], drum[2]);
        renderPart(GunModels.obj("mk108"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Drum");

        double p = 0.0625D;
        double x = p * 22;
        double y = p * -46;
        double angle = 0;
        Vec3NT vec = new Vec3NT(0, 0.53125, 0);

        double[] anglesLoaded = new double[]   {0,   0,  -5,   0,   -5,  60,  45,  -10,   0};
        double[] anglesUnloaded = new double[] {0, -30, -60, -45, -45,   0,   0,   0,   0};
        double[][] shells = new double[anglesLoaded.length][3];
        double reloadProgress = !reloading ? 1D : belt[0];
        double cycleProgress = !doesCycle ? 1 : cycle[0];

        for(int i = 0; i < anglesLoaded.length; i++) {
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

        // draw belt, partialTick used for cycling (shells will transform towards the position/rotation of the next shell)
        for(int i = 0; i < shells.length - 1; i++) {
            double[] prevShell = shells[i];
            double[] nextShell = shells[i + 1];
            renderShell(prevShell[0], nextShell[0], prevShell[1], nextShell[1], prevShell[2], nextShell[2], shells.length - i < shellAmount + 2, cycleProgress,
                    poseStack, bufferSource, currentTex, packedLight, packedOverlay);
        }
        poseStack.popPose();

        

        poseStack.pushPose();
        poseStack.translate(0, 0, 8.125);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (90 * gun.shotRand))));
        renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 50, 5);
        poseStack.popPose();
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        double scale = 2.0D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.translate(1, -2.5, 4);
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        double scale = 1.375D;
        poseStack.scale((float)(scale), (float)(scale), (float)(scale));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
        poseStack.translate(0, 0.5, 0.25);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
        
        
        /* bind */ currentTex = GunModels.tex("mk108_tex");
        renderPart(GunModels.obj("mk108"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
        renderPart(GunModels.obj("mk108"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
        renderPart(GunModels.obj("mk108"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lid");
        renderPart(GunModels.obj("mk108"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Drum");

        poseStack.pushPose();

        double p = 0.0625D;
        double x = p * 22;
        double y = p * -46;
        double angle = 0;
        Vec3NT vec = new Vec3NT(0, 0.53125, 0);

        double[] anglesLoaded = new double[] { 0, 0, -5, 0, -5, 60, 45, -10, 0 };
        double[][] shells = new double[anglesLoaded.length][3];

        for(int i = 0; i < anglesLoaded.length; i++) {
            shells[i][0] = x;
            shells[i][1] = y;
            shells[i][2] = angle - 90;
            double delta = anglesLoaded[i];
            angle += delta;
            vec.rotateAroundZDeg(-delta);
            x += vec.x;
            y += vec.y;
        }

        // draw belt, partialTick used for cycling (shells will transform towards the position/rotation of the next shell)
        for(int i = 0; i < shells.length - 1; i++) {
            double[] prevShell = shells[i];
            double[] nextShell = shells[i + 1];
            renderShell(prevShell[0], nextShell[0], prevShell[1], nextShell[1], prevShell[2], nextShell[2], true, 0F,
                    poseStack, bufferSource, currentTex, packedLight, packedOverlay);
        }
        poseStack.popPose();

        
    }

    private void renderShell(double x0, double x1, double y0, double y1, double rot0, double rot1, boolean shell, double partialTick,
                             PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation currentTex, int packedLight, int packedOverlay) {
        renderShell(BobMathUtil.partialTick(x0, x1, partialTick), BobMathUtil.partialTick(y0, y1, partialTick),
                BobMathUtil.partialTick(rot0, rot1, partialTick), shell, poseStack, bufferSource, currentTex, packedLight, packedOverlay);
    }

    private void renderShell(double x, double y, double rot, boolean shell,
                             PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation currentTex, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) rot));
        renderPart(GunModels.obj("mk108"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Belt");
        if (shell) renderPart(GunModels.obj("mk108"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Grenade");
        poseStack.popPose();
    }
}
