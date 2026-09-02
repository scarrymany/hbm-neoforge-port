package com.hbm.client.render.item.weapon;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Port of CE {@code ItemRenderStinger}. The 1.12 "Not accurate" font overlay is skipped:
 * TODO(CE:ItemRenderStinger.java:81-100) FontRenderer/OpenGlHelper immediate-mode label.
 */
public class ItemRenderStinger extends ItemRenderGunBase {

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }

    @Override
    public float getViewFOV(ItemStack stack, float fov) {
        float aimingProgress = Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
        return fov * (1 - aimingProgress * 0.5F);
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0, 0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(poseStack, -3.75F * offset, -9F * offset, -3.5F * offset, -2.625F * offset, -6.5, -8.5F);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {
        if (ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        ResourceLocation currentTex = GunModels.tex("stinger_tex");
        float scale = 1.5F;
        poseStack.scale(scale, scale, scale);

        double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
        double[] reload = GunAnimationClientState.getRelevantTransformation("RELOAD");
        double[] rocket = GunAnimationClientState.getRelevantTransformation("ROCKET");

        poseStack.translate(0, -1, -1);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        poseStack.translate(0, 1, 1);

        poseStack.translate(0, -4, -3);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) reload[0]));
        poseStack.translate(0, 4, 3);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
        renderAll(GunModels.obj("stinger"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        currentTex = GunModels.tex("panzerschreck_tex");
        poseStack.translate(rocket[0], rocket[1] + 3.5, rocket[2] - 3);
        renderPart(GunModels.obj("panzerschreck"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Rocket");
        // TODO(CE:ItemRenderStinger.java:81-100) "Not accurate" font overlay
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 0, 6.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(90F));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        poseStack.scale(0.75F, 0.75F, 0.75F);
        renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 150, 7.5);
        poseStack.popPose();
    }

    @Override
    protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
        super.setupThirdPersonGun(stack, poseStack);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.translate(0, -2.5, -3.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
    }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        super.setupInventoryGun(stack, poseStack);
        poseStack.scale(1.0625F, 1.0625F, 1.0625F);
        poseStack.mulPose(Axis.XP.rotationDegrees(25F));
        poseStack.mulPose(Axis.YP.rotationDegrees(225F));
        poseStack.translate(0.25, -2.5, 0);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay) {
        renderAll(GunModels.obj("stinger"), poseStack, bufferSource, GunModels.tex("stinger_tex"), packedLight, packedOverlay);
    }
}
