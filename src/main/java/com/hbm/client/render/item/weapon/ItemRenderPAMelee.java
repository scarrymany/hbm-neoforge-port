package com.hbm.client.render.item.weapon;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * CE {@code ItemRenderPAMelee} without {@code IPAWeaponsProvider} (not ported).
 * TODO(CE:XFactoryPA.java:36) / TODO(CE:ItemRenderPAMelee.java) first-person delegates to IPAMelee.
 * Draws the CE NCR-arm OBJ that the CE class already uses for inv/other.
 */
public class ItemRenderPAMelee extends ItemRenderGunBase {

    @Override public boolean isAkimbo() { return true; }

    @Override protected float getSwayMagnitude(ItemStack stack) { return 2F; }
    @Override protected float getSwayPeriod(ItemStack stack) { return 0.5F; }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }

    @Override
    protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
        poseStack.translate(0.0, 0.0, 0.875);
    }

    @Override
    protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderArms(poseStack, bufferSource, packedLight, packedOverlay);
    }

    @Override protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) { }
    @Override public void setupThirdPersonAkimbo(ItemStack stack, PoseStack poseStack) { }

    @Override
    protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        poseStack.scale(1.0F, 1.0F, -1.0F);
        poseStack.translate(8.0D, 8.0D, 0.0D);
        double scale = 2.5D;
        poseStack.scale((float) scale, (float) scale, (float) scale);
    }

    @Override
    protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderArms(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private void renderArms(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = GunModels.tex("ncrpa_arm");
        poseStack.pushPose();
        double scale = 0.3125D;
        poseStack.scale((float) scale, (float) scale, (float) scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.translate(0.0D, -5.5D, 0.0D);
        poseStack.translate(-2.0D, 0.0D, 0.0D);
        renderPart(GunModels.obj("armor_ncr"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Leftarm");
        poseStack.translate(4.0D, 0.0D, 0.0D);
        renderPart(GunModels.obj("armor_ncr"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "RightArm");
        poseStack.popPose();
    }
}
