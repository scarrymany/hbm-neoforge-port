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

public class ItemRenderLiberator extends ItemRenderGunBase {

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
				-1.5F * offset, -1.25F * offset, 1.25F * offset,
				0, -4.625 / 8D, 0.25);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = GunModels.tex("liberator_tex");
		double scale = 0.375D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT");
		double[] latch = GunAnimationClientState.getRelevantTransformation("LATCH");
		double[] brk = GunAnimationClientState.getRelevantTransformation("BREAK");
		double[] shell1 = GunAnimationClientState.getRelevantTransformation("SHELL1");
		double[] shell2 = GunAnimationClientState.getRelevantTransformation("SHELL2");
		double[] shell3 = GunAnimationClientState.getRelevantTransformation("SHELL3");
		double[] shell4 = GunAnimationClientState.getRelevantTransformation("SHELL4");

		poseStack.translate(0, -1, -3);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
		poseStack.translate(0, 1, 3);

		poseStack.translate(0, -3, -3);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) lift[0])));
		poseStack.translate(0, 3, 3);

		poseStack.translate(recoil[0] * 2, recoil[1], recoil[2]);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (recoil[2] * 10))));

		

		renderPart(GunModels.obj("liberator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");

		poseStack.pushPose();

		poseStack.translate(0, -0.5, 0.75);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) brk[0])));
		poseStack.translate(0, 0.5, -0.75);
		renderPart(GunModels.obj("liberator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");

		poseStack.pushPose();
		poseStack.translate(shell1[0], shell1[1], shell1[2]);
		renderPart(GunModels.obj("liberator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Shell1");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(shell2[0], shell2[1], shell2[2]);
		renderPart(GunModels.obj("liberator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Shell2");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(shell3[0], shell3[1], shell3[2]);
		renderPart(GunModels.obj("liberator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Shell3");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(shell4[0], shell4[1], shell4[2]);
		renderPart(GunModels.obj("liberator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Shell4");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, 1.15625, 0.75);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) latch[0])));
		poseStack.translate(0, -1.15625, -0.75);
		renderPart(GunModels.obj("liberator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Latch");
		poseStack.popPose();
		poseStack.popPose();

		double smokeScale = 0.375;

		com.hbm.items.weapon.sedna.GunConfig cfg = gun.getConfig(stack, 0);

		poseStack.pushPose();
		poseStack.translate(0, 0.25, 7.25);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
		poseStack.translate(0, 0, 0.25 / smokeScale);
		/* TODO(CE:ItemRenderLiberator:smokeNodes) smokeNodes not ported */
		poseStack.translate(0, 0, -0.5 / smokeScale);
		/* TODO(CE:ItemRenderLiberator:smokeNodes) smokeNodes not ported */
		poseStack.translate(0, 0.5 / smokeScale, 0);
		/* TODO(CE:ItemRenderLiberator:smokeNodes) smokeNodes not ported */
		poseStack.translate(0, 0, 0.5 / smokeScale);
		/* TODO(CE:ItemRenderLiberator:smokeNodes) smokeNodes not ported */
		poseStack.popPose();

		

		poseStack.pushPose();
		poseStack.translate(0, 0.5, 8);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
		poseStack.scale((float)(1.5), (float)(1.5), (float)(1.5));
		renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 5);
		poseStack.popPose();
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		poseStack.translate(0, 1, 3);
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
		

		
		/* bind */ currentTex = GunModels.tex("liberator_tex");
		renderAll(GunModels.obj("liberator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
		
	}
}

