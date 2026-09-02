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

public class ItemRenderPanzerschreck extends ItemRenderGunBase {

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
				-2.75F * offset, -2F * offset, 2.5F * offset,
				-0.9375, -9.25 / 8D, 0.25);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = GunModels.tex("panzerschreck_tex");
		double scale = 1.25D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] reload = GunAnimationClientState.getRelevantTransformation("RELOAD");
		double[] rocket = GunAnimationClientState.getRelevantTransformation("ROCKET");

		poseStack.translate(0, -1, -1);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
		poseStack.translate(0, 1, 1);

		poseStack.translate(0, -4, -3);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) reload[0])));
		poseStack.translate(0, 4, 3);

		

		renderPart(GunModels.obj("panzerschreck"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Tube");
		if(hasShield(stack)) renderPart(GunModels.obj("panzerschreck"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Shield");

		poseStack.pushPose();
		poseStack.translate(rocket[0], rocket[1], rocket[2]);
		renderPart(GunModels.obj("panzerschreck"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Rocket");
		poseStack.popPose();

		

		poseStack.pushPose();
		poseStack.translate(0, 0, 6.5);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
		poseStack.scale((float)(0.75), (float)(0.75), (float)(0.75));
		renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 150, 7.5);
		poseStack.popPose();
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 3D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 0.5, 1);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		double scale = 1.5D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
		poseStack.translate(-0.5, 0.5, 0);
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		
		/* bind */ currentTex = GunModels.tex("panzerschreck_tex");
		renderPart(GunModels.obj("panzerschreck"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Tube");
		if(hasShield(stack)) renderPart(GunModels.obj("panzerschreck"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Shield");
		
	}

	public boolean hasShield(ItemStack stack) {
		return !XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "no_shield"));
	}
}

