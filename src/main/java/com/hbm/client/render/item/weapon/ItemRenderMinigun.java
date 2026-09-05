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

public class ItemRenderMinigun extends ItemRenderGunBase {

	protected ResourceLocation texture;

	public ItemRenderMinigun(ResourceLocation texture) {
		this.texture = texture;
	}

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
				-1.75F * offset, -1.75F * offset, 3.5F * offset,
				0, -6.25 / 8D, 1);
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
		double[] rotate = GunAnimationClientState.getRelevantTransformation("ROTATE");

		poseStack.translate(0, 3, -6);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
		poseStack.translate(0, -3, 6);

		poseStack.translate(0, 0, recoil[2]);

		

		renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Grip");

		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) rotate[2])));
		renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrels");
		poseStack.popPose();

		double smokeScale = 0.5;

		poseStack.pushPose();
		poseStack.translate(-2, 1.25, -3.5);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
		poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
		/* TODO(CE:ItemRenderMinigun:smokeNodes) smokeNodes not ported */
		poseStack.popPose();

		

		poseStack.pushPose();
		poseStack.translate(0, 0, 12);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		if(stack.getItem() == GunRifleItems.GUN_MINIGUN_LACUNAE.get()) {
			renderLaserFlash(poseStack, bufferSource, gun.lastShot[0], 50, 1D, 0xff00ff);
			poseStack.translate(0, 0, -0.25);
			renderLaserFlash(poseStack, bufferSource, gun.lastShot[0], 50, 0.5D, 0xff0080);
		} else {
			poseStack.mulPose(Axis.XP.rotationDegrees((float) (gun.shotRand * 90)));
			poseStack.scale((float)(1.5), (float)(1.5), (float)(1.5));
			renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 5);
		}
		poseStack.popPose();
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 1.75D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(1, -3.5, 8);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		double scale = 0.875D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
		poseStack.translate(-0.25, 0.5, 0);
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		
		/* bind */ currentTex = texture;
		renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Grip");
		renderPart(GunModels.obj("minigun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrels");
		
	}
}

