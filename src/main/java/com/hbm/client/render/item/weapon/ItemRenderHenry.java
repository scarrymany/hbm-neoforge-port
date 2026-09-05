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

public class ItemRenderHenry extends ItemRenderGunBase {

	public ResourceLocation texture;

	public ItemRenderHenry(ResourceLocation texture) {
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
				-1.25F * offset, -1F * offset, 1.75F * offset,
				0, -5 / 8D, 1);

		float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
		double r = -2.5 * aimingProgress;
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) r)));
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = texture;
		double scale = 0.375D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] sight = GunAnimationClientState.getRelevantTransformation("SIGHT");
		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] hammer = GunAnimationClientState.getRelevantTransformation("HAMMER");
		double[] lever = GunAnimationClientState.getRelevantTransformation("LEVER");
		double[] turn = GunAnimationClientState.getRelevantTransformation("TURN");
		double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT");
		double[] twist = GunAnimationClientState.getRelevantTransformation("TWIST");
		double[] bullet = GunAnimationClientState.getRelevantTransformation("BULLET");
		double[] yeet = GunAnimationClientState.getRelevantTransformation("YEET");
		double[] roll = GunAnimationClientState.getRelevantTransformation("ROLL");

		

		poseStack.translate(recoil[0] * 2, recoil[1], recoil[2]);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (recoil[2] * 5))));
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) turn[2])));

		poseStack.translate(yeet[0], yeet[1], yeet[2]);

		poseStack.translate(0, 1, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) roll[2])));
		poseStack.translate(0, -1, 0);

		poseStack.translate(0, -4, 4);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) lift[0])));
		poseStack.translate(0, 4, -4);

		poseStack.translate(0, 2, -4);
		poseStack.mulPose(Axis.XN.rotationDegrees((float) ((float) equip[0])));
		poseStack.translate(0, -2, 4);

		poseStack.pushPose();
		poseStack.translate(0, 1, 8);
		poseStack.mulPose(Axis.ZN.rotationDegrees((float) ((float) turn[2])));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		/* TODO(CE:ItemRenderHenry:smokeNodes) smokeNodes not ported */
		poseStack.popPose();

		renderPart(GunModels.obj("henry"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");

		poseStack.pushPose();
		poseStack.translate(0, 1.25, -0.1875);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) sight[0])));
		poseStack.translate(0, -1.25, 0.1875);
		renderPart(GunModels.obj("henry"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Sight");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, 0.625, -3);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (-30 + hammer[0]))));
		poseStack.translate(0, -0.625, 3);
		renderPart(GunModels.obj("henry"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hammer");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, 0.25, -2.3125);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) lever[0])));
		poseStack.translate(0, -0.25, 2.3125);
		renderPart(GunModels.obj("henry"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lever");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, 1, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) twist[2])));
		poseStack.translate(0, -1, 0);
		renderPart(GunModels.obj("henry"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Front");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(bullet[0], bullet[1], bullet[2] - 1);
		renderPart(GunModels.obj("henry"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bullet");
		poseStack.popPose();

		

		poseStack.pushPose();
		poseStack.translate(0, 1, 8);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
		renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 5);
		poseStack.popPose();
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 1.75D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 0.25, 3);
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
		

		
		/* bind */ currentTex = GunModels.tex("henry_tex");
		renderAll(GunModels.obj("henry"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
		
	}
}

