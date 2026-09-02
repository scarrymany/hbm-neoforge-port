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

public class ItemRenderCoilgun extends ItemRenderGunBase {

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
				-1.25F * offset, -1.5F * offset, 2.5F * offset,
				0, -7.5 / 8D, 1);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		/* bind */ currentTex = GunModels.tex("flaregun_tex");
		double scale = 0.75D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		poseStack.mulPose(Axis.YP.rotationDegrees((float) (-90F)));

		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		poseStack.translate(-1.5 - recoil[0] * 0.5, 0, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) (recoil[0] * 45))));
		poseStack.translate(1.5, 0, 0);

		double[] reload = GunAnimationClientState.getRelevantTransformation("RELOAD");
		poseStack.translate(-2.5, 0, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) (reload[0] * -45))));
		poseStack.translate(2.5, 0, 0);

		
		/* bind */ currentTex = GunModels.tex("coilgun_tex");
		renderAll(GunModels.obj("coilgun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
		
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 3D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 0.25, 1.25);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		double scale = 4D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25F)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45F)));
		poseStack.translate(-0.25, -0.25, 0);
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		poseStack.mulPose(Axis.YP.rotationDegrees((float) (-90F)));

		
		/* bind */ currentTex = GunModels.tex("coilgun_tex");
		renderAll(GunModels.obj("coilgun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
		
	}
}

