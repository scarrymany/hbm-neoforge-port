package com.hbm.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * Port of CE {@code com.hbm.render.item.ItemRenderFrames17}.
 * Same translate/rotate/scale sequences, applied to a 1.21 {@link PoseStack}
 * in the same compose order as CE {@code BakedModelMatrixUtil.glMatrix}.
 */
public final class ItemRenderFrames17 {

    private ItemRenderFrames17() {
    }

    public static void applyGui(PoseStack pose) {
        pose.translate(0.5, 0.375, 0.0);
        pose.mulPose(Axis.XP.rotationDegrees(30F));
        pose.mulPose(Axis.YP.rotationDegrees(225F));
        float inv16 = 1.0F / 16.0F;
        pose.scale(inv16, inv16, inv16);
        pose.scale(-1F, -1F, -1F);
        pose.mulPose(Axis.YP.rotationDegrees(-45F));
        pose.mulPose(Axis.XP.rotationDegrees(30F));
        pose.translate(-8.0, -10.0, 0.0);
    }

    public static void applyThirdPerson(PoseStack pose) {
        pose.translate(0.4375, 0.375, 1.125);
        pose.mulPose(Axis.YP.rotationDegrees(180F));
        pose.mulPose(Axis.XP.rotationDegrees(90F));
        pose.translate(0.1875, 0.625, -0.125);
        pose.scale(0.375F, 0.375F, 0.375F);
        pose.mulPose(Axis.ZP.rotationDegrees(60F));
        pose.mulPose(Axis.XP.rotationDegrees(-90F));
        pose.mulPose(Axis.ZP.rotationDegrees(20F));
        pose.translate(0.0, -0.3, 0.0);
        pose.scale(1.5F, 1.5F, 1.5F);
        pose.mulPose(Axis.YP.rotationDegrees(50F));
        pose.mulPose(Axis.ZP.rotationDegrees(335F));
        pose.translate(-0.9375, -0.0625, 0.0);
    }

    public static void applyThirdPersonLeft(PoseStack pose) {
        pose.translate(0.5625, 0.375, 1.125);
        pose.mulPose(Axis.YP.rotationDegrees(180F));
        pose.mulPose(Axis.XP.rotationDegrees(90F));
        pose.translate(-0.1875, 0.625, -0.125);
        pose.scale(0.375F, 0.375F, 0.375F);
        pose.mulPose(Axis.ZP.rotationDegrees(-60F));
        pose.mulPose(Axis.XP.rotationDegrees(-90F));
        pose.mulPose(Axis.ZP.rotationDegrees(-20F));
        pose.translate(0.0, -0.3, 0.0);
        pose.scale(1.5F, 1.5F, 1.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-50F));
        pose.mulPose(Axis.ZP.rotationDegrees(-335F));
        pose.translate(0.9375, -0.0625, 0.0);
    }

    public static void applyHead(PoseStack pose) {
        pose.translate(0.5, 0.5, 0.5);
        pose.scale(1.6F, -1.6F, -1.6F);
        pose.mulPose(Axis.YP.rotationDegrees(180F));
        pose.translate(0.0, 0.25, 0.0);
        pose.translate(0.0, -0.3, 0.0);
        pose.scale(1.5F, 1.5F, 1.5F);
        pose.mulPose(Axis.YP.rotationDegrees(50F));
        pose.mulPose(Axis.ZP.rotationDegrees(335F));
        pose.translate(-0.9375, -0.0625, 0.0);
    }

    public static void applyGround(PoseStack pose) {
        pose.translate(0.5, 0.25, 0.5);
        pose.scale(0.5F, 0.5F, 0.5F);
    }

    public static void applyFixed(PoseStack pose) {
        pose.translate(0.5, 0.34, 0.53125);
        pose.mulPose(Axis.YP.rotationDegrees(180F));
    }
}
