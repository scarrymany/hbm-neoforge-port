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

public class ItemRenderFlaregun extends ItemRenderGunBase {

	public ItemRenderFlaregun() {	}

	@Override
	protected float getTurnMagnitude(ItemStack stack) {
		return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
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
				-1.25F * offset, -1.5F * offset, 2F * offset,
				0, -5.5 / 8D, 0.5);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = GunModels.tex("flaregun_tex");
		double scale = 0.125D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] hammer = GunAnimationClientState.getRelevantTransformation("HAMMER");
		double[] open = GunAnimationClientState.getRelevantTransformation("OPEN");
		double[] shell = GunAnimationClientState.getRelevantTransformation("SHELL");
		double[] flip = GunAnimationClientState.getRelevantTransformation("FLIP");

		poseStack.translate(recoil[0], recoil[1], recoil[2]);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (recoil[2] * 10))));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) flip[0])));

		poseStack.translate(0, 0, -8);
		poseStack.mulPose(Axis.XN.rotationDegrees((float) ((float) equip[0])));
		poseStack.translate(0, 0, 8);

		
		renderPart(GunModels.obj("flaregun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");

		poseStack.pushPose();
		poseStack.translate(0, 1.8125, -4);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (hammer[0] - 15))));
		poseStack.translate(0, -1.8125, 4);
		renderPart(GunModels.obj("flaregun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hammer");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, 2.156, 1.78);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) open[0])));
		poseStack.translate(0, -2.156, -1.78);
		renderPart(GunModels.obj("flaregun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
		poseStack.translate(shell[0], shell[1], shell[2]);
		renderPart(GunModels.obj("flaregun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Flare");
		poseStack.popPose();

		double smokeScale = 0.5;

		poseStack.pushPose();
		poseStack.translate(0, 4, 9);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
		poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
		/* TODO(CE:ItemRenderFlaregun:smokeNodes) smokeNodes not ported */
		poseStack.translate(0, 0, 0.1);
		/* TODO(CE:ItemRenderFlaregun:smokeNodes) smokeNodes not ported */
		poseStack.popPose();

		
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 0.5D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 0.25, 3);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		double scale = 1D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25F)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45F)));
		poseStack.translate(-0.5, 0, 0);
	}

	@Override
	protected void setupEntityGun(ItemStack stack, PoseStack poseStack) {
		super.setupEntityGun(stack, poseStack);
		double scale = 0.5D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		
		/* bind */ currentTex = GunModels.tex("flaregun_tex");
		renderAll(GunModels.obj("flaregun"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
		
	}
}

