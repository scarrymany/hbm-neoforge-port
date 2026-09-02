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

public class ItemRenderHangman extends ItemRenderGunBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

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
				-1.5F * offset, -0.875F * offset, 1.75F * offset,
				0, -1.5 / 8D, 1.25);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = GunModels.tex("hangman_tex");
		float offset = 0.8F;

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] roll = GunAnimationClientState.getRelevantTransformation("ROLL");
		double[] turn = GunAnimationClientState.getRelevantTransformation("TURN");
		double[] smack = GunAnimationClientState.getRelevantTransformation("SMACK");
		double[] lid = GunAnimationClientState.getRelevantTransformation("LID");
		double[] mag = GunAnimationClientState.getRelevantTransformation("MAG");
		double[] bullets = GunAnimationClientState.getRelevantTransformation("BULLETS");

		poseStack.translate(1.5F * offset, 0, -1);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) ((float) turn[1])));
		poseStack.translate(-1.5F * offset, 0, 1);

		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) roll[2])));
		poseStack.translate(smack[0], smack[1], smack[2]);

		double scale = 0.125D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		poseStack.translate(0, -4, -10);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
		poseStack.translate(0, 4, 10);

		poseStack.translate(0, 0, recoil[2]);

		

		renderPart(GunModels.obj("hangman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Rifle");
		renderPart(GunModels.obj("hangman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Internals");

		poseStack.pushPose();
		poseStack.translate(-2.1875, -1.75, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) lid[2])));
		poseStack.translate(2.1875, 1.75, 0);
		renderPart(GunModels.obj("hangman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lid");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(mag[0], mag[1], mag[2]);
		renderPart(GunModels.obj("hangman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
		if (bullets[0] == 0) renderPart(GunModels.obj("hangman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bullets");
		poseStack.popPose();

		double smokeScale = 1.5;

		poseStack.pushPose();
		poseStack.translate(0, 0, 29);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
		/* TODO(CE:ItemRenderHangman:smokeNodes) smokeNodes not ported */
		poseStack.popPose();

		

		poseStack.pushPose();
		poseStack.translate(0, 0, 29);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
		poseStack.scale((float)(2), (float)(2), (float)(2));
		renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 7.5);
		poseStack.popPose();
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 0.5D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 4.25, 11);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		double scale = 0.375D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
		poseStack.translate(-0.5, 2.5, 0);
	}

	@Override
	protected void setupEntityGun(ItemStack stack, PoseStack poseStack) {
		double scale = 0.0625D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		
		/* bind */ currentTex = GunModels.tex("hangman_tex");
		renderAll(GunModels.obj("hangman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
		
	}
}

