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

public class ItemRenderCarbine extends ItemRenderGunBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) {
		return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
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
				-1.5F * offset, -1.5F * offset, 0.875F * offset,
				0, -6.25 / 8D, 0.25);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = GunModels.tex("carbine_tex");
		double scale = 0.5D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] slide = GunAnimationClientState.getRelevantTransformation("SLIDE");
		double[] mag = GunAnimationClientState.getRelevantTransformation("MAG");
		double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT");
		double[] bullet = GunAnimationClientState.getRelevantTransformation("BULLET");
		double[] rel = GunAnimationClientState.getRelevantTransformation("REL");
		double[] stab = GunAnimationClientState.getRelevantTransformation("STAB");

		poseStack.translate(0, -1, -2);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
		poseStack.translate(0, 1, 2);

		poseStack.translate(0, 0, -2);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) lift[0])));
		poseStack.translate(0, 0, 2);

		poseStack.translate(stab[0], stab[1], stab[2]);

		poseStack.translate(0, 0, recoil[2]);

		

		renderPart(GunModels.obj("carbine"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");

		poseStack.pushPose();
		poseStack.translate(0, 0, slide[2]);
		renderPart(GunModels.obj("carbine"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Slide");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(mag[0], mag[1], mag[2]);
		renderPart(GunModels.obj("carbine"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
		poseStack.translate(rel[0], rel[1], rel[2]);
		if (bullet[0] != 1) renderPart(GunModels.obj("carbine"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bullet");
		poseStack.popPose();

		if(hasBayonet(stack)) {
			/* bind */ currentTex = GunModels.tex("carbine_bayonet_tex");
			renderPart(GunModels.obj("carbine"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bayonet");
		}

		poseStack.pushPose();
		poseStack.translate(0, 1, 8);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
		/* TODO(CE:ItemRenderCarbine:smokeNodes) smokeNodes not ported */
		poseStack.popPose();

		

		poseStack.pushPose();
		poseStack.translate(0, 1, 8);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (90F * gun.shotRand)));
		poseStack.scale((float)(0.5), (float)(0.5), (float)(0.5));
		renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 7.5);
		poseStack.popPose();
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 1.375D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 0, 2);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		if(hasBayonet(stack)) {
			double scale = 1.1875D;
			poseStack.scale((float)(scale), (float)(scale), (float)(scale));
			poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
			poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
			poseStack.translate(1.5, 0, 0);
		} else {
			double scale = 1.375D;
			poseStack.scale((float)(scale), (float)(scale), (float)(scale));
			poseStack.mulPose(Axis.XP.rotationDegrees((float) (25F)));
			poseStack.mulPose(Axis.YP.rotationDegrees((float) (45F)));
			poseStack.translate(-0.5, 0, 0);
		}
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		
		/* bind */ currentTex = GunModels.tex("carbine_tex");
		renderPart(GunModels.obj("carbine"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("carbine"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Slide");
		renderPart(GunModels.obj("carbine"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
		if(hasBayonet(stack)) {
			/* bind */ currentTex = GunModels.tex("carbine_bayonet_tex");
			renderPart(GunModels.obj("carbine"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bayonet");
		}
		
	}

	public boolean hasBayonet(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "carbine_bayonet"));
	}
}

