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

public class ItemRenderDoubleBarrel extends ItemRenderGunBase {

	protected ResourceLocation texture;

	public ItemRenderDoubleBarrel(ResourceLocation texture) {
		this.texture = texture;	}

	@Override
	protected float getTurnMagnitude(ItemStack stack) {
		return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
	}

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
				-1.25F * offset, -1F * offset, 2F * offset,
				0, -2 / 8D, 1);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = texture;
		double scale = 0.375D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] turn = GunAnimationClientState.getRelevantTransformation("TURN");
		double[] barrel = GunAnimationClientState.getRelevantTransformation("BARREL");
		double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT");
		double[] shells = GunAnimationClientState.getRelevantTransformation("SHELLS");
		double[] shellFlip = GunAnimationClientState.getRelevantTransformation("SHELL_FLIP");
		double[] lever = GunAnimationClientState.getRelevantTransformation("LEVER");
		double[] buckle = GunAnimationClientState.getRelevantTransformation("BUCKLE");
		double[] no_ammo = GunAnimationClientState.getRelevantTransformation("NO_AMMO");

		

		poseStack.translate(recoil[0] * 3, recoil[1], recoil[2]);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (recoil[2] * 10))));

		poseStack.translate(0, 0, -4);
		poseStack.mulPose(Axis.XN.rotationDegrees((float) ((float) equip[0])));
		poseStack.translate(0, 0, 4);

		poseStack.translate(0, 0, -4);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) ((float) turn[1])));
		poseStack.translate(0, 0, 4);

		poseStack.translate(0, 0, -4);
		poseStack.mulPose(Axis.XN.rotationDegrees((float) ((float) lift[0])));
		poseStack.translate(0, 0, 4);

		renderPart(GunModels.obj("double_barrel"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Stock");

		poseStack.pushPose();

		poseStack.translate(0, -0.4375, -0.875);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) barrel[0])));
		poseStack.translate(0, 0.4375, 0.875);

		renderPart(GunModels.obj("double_barrel"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "BarrelShort");
		if(!isSawedOff(stack)) renderPart(GunModels.obj("double_barrel"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");

		poseStack.pushPose();
		poseStack.translate(0.75, 0, -0.6875);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) ((float) buckle[1])));
		poseStack.translate(-0.75, 0, 0.6875);
		renderPart(GunModels.obj("double_barrel"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Buckle");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(-0.3125, 0.3125, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) lever[2])));
		poseStack.translate(0.3125, -0.3125, 0);
		renderPart(GunModels.obj("double_barrel"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lever");
		poseStack.popPose();

		if(no_ammo[0] == 0) {
			poseStack.pushPose();
			poseStack.translate(shells[0], shells[1], shells[2]);
			poseStack.translate(0, 0, -1);
			poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) shellFlip[0])));
			poseStack.translate(0, 0, 1);
			renderPart(GunModels.obj("double_barrel"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Shells");
			poseStack.popPose();
		}

		poseStack.popPose();

		

		poseStack.pushPose();
		poseStack.translate(0, 0, 8);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (90F * gun.shotRand)));
		poseStack.scale((float)(2), (float)(2), (float)(2));
		renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 5);
		poseStack.popPose();
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 1.75D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 1, 3);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		if(isSawedOff(stack)) {
			double scale = 2D;
			poseStack.scale((float)(scale), (float)(scale), (float)(scale));
			poseStack.mulPose(Axis.XP.rotationDegrees((float) (25F)));
			poseStack.mulPose(Axis.YP.rotationDegrees((float) (45F)));
			poseStack.translate(-2, 0.5, 0);
		} else {
			double scale = 1.375D;
			poseStack.scale((float)(scale), (float)(scale), (float)(scale));
			poseStack.mulPose(Axis.XP.rotationDegrees((float) (25F)));
			poseStack.mulPose(Axis.YP.rotationDegrees((float) (45F)));
			poseStack.translate(0, 0.5, 0);
		}
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		
		/* bind */ currentTex = texture;
		renderPart(GunModels.obj("double_barrel"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Stock");
		renderPart(GunModels.obj("double_barrel"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "BarrelShort");
		if(!isSawedOff(stack)) renderPart(GunModels.obj("double_barrel"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
		renderPart(GunModels.obj("double_barrel"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Buckle");
		renderPart(GunModels.obj("double_barrel"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lever");
		renderPart(GunModels.obj("double_barrel"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Shells");
		
	}

	public boolean isSawedOff(ItemStack stack) {
		return stack.getItem() == GunShotgunItems.GUN_DOUBLE_BARREL_SACRED_DRAGON.get();
	}
}

