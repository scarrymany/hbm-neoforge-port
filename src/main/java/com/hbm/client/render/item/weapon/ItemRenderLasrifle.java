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

public class ItemRenderLasrifle extends ItemRenderGunBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) {
		return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
	}

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
		return fov * (1 - aimingProgress * (hasScope(stack) ? 0.75F : 0.66F));
	}

	@Override
	protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
		poseStack.translate(0, 0, 0.875);

		float offset = 0.8F;
		if(hasScope(stack)) {
			standardAimingTransform(poseStack,
					-1.5F * offset, -1.5F * offset, 2.5F * offset,
					0, -7.375 / 8D, 0.75);
		} else {
			standardAimingTransform(poseStack,
					-1.5F * offset, -1.5F * offset, 2.5F * offset,
					0, -5.25 / 8D, 1);
		}
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		if(hasScope(stack) && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = GunModels.tex("lasrifle_tex");
		double scale = 0.3125D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] lever = GunAnimationClientState.getRelevantTransformation("LEVER");
		double[] mag = GunAnimationClientState.getRelevantTransformation("MAG");

		poseStack.translate(0, -1, -6);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
		poseStack.translate(0, 1, 6);

		poseStack.translate(0, 0, recoil[2]);

		

		renderPart(GunModels.obj("lasrifle"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("lasrifle"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
		renderPart(GunModels.obj("lasrifle"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Stock");
		if(hasScope(stack)) renderPart(GunModels.obj("lasrifle"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Scope");

		poseStack.pushPose();
		poseStack.translate(0, -0.375, 2.375);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) lever[0])));
		poseStack.translate(0, 0.375, -2.375);
		renderPart(GunModels.obj("lasrifle"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lever");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(mag[0], mag[1], mag[2]);
		renderPart(GunModels.obj("lasrifle"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Battery");
		poseStack.popPose();

		if(!hasShotgun(stack)) renderPart(GunModels.obj("lasrifle"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
		/* bind */ currentTex = GunModels.tex("lasrifle_mods_tex");
		if(hasShotgun(stack)) renderPart(GunModels.obj("lasrifle_mods"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "BarrelShotgun");
		if(hasCapacitor(stack)) renderPart(GunModels.obj("lasrifle_mods"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "UnderBarrel");

		poseStack.pushPose();
		poseStack.translate(0, 1.5, 12);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		renderLaserFlash(poseStack, bufferSource, gun.lastShot[0], 150, 1.5D, 0xff0000);
		poseStack.translate(0, 0, -0.25);
		renderLaserFlash(poseStack, bufferSource, gun.lastShot[0], 150, 0.75D, 0xff8000);
		poseStack.popPose();

		
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 1.25D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 0, 4);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		double scale = 1.03125D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
		poseStack.translate(0.75, 0, 0);
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		
		/* bind */ currentTex = GunModels.tex("lasrifle_tex");
		renderPart(GunModels.obj("lasrifle"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("lasrifle"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Stock");
		if(hasScope(stack)) renderPart(GunModels.obj("lasrifle"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Scope");
		renderPart(GunModels.obj("lasrifle"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lever");
		renderPart(GunModels.obj("lasrifle"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Battery");
		if(!hasShotgun(stack)) renderPart(GunModels.obj("lasrifle"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
		/* bind */ currentTex = GunModels.tex("lasrifle_mods_tex");
		if(hasShotgun(stack)) renderPart(GunModels.obj("lasrifle_mods"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "BarrelShotgun");
		if(hasCapacitor(stack)) renderPart(GunModels.obj("lasrifle_mods"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "UnderBarrel");
		
	}

	public boolean hasScope(ItemStack stack) {
		return !XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "las_auto"));
	}

	public boolean hasShotgun(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "las_shotgun"));
	}

	public boolean hasCapacitor(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "las_capacitor"));
	}
}

