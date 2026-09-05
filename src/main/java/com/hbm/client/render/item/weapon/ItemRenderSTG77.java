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

public class ItemRenderSTG77 extends ItemRenderGunBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 0.5F : -0.25F; }

	@Override
	protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
		poseStack.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(poseStack,
				-1.5F * offset, -1F * offset, 2.5F * offset,
				0, -5.75 / 8D, 2);
	}

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
		return  fov * (1 - aimingProgress * 0.66F);
	}

	protected float getBaseFOV(ItemStack stack) {
		float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
		return 70F - aimingProgress * 65;
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		if(ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = GunModels.tex("stg77_tex");
		double scale = 0.5D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT");
		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] bolt = GunAnimationClientState.getRelevantTransformation("BOLT");
		double[] handle = GunAnimationClientState.getRelevantTransformation("HANDLE");
		double[] safety = GunAnimationClientState.getRelevantTransformation("SAFETY");

		double[] inspectGun = GunAnimationClientState.getRelevantTransformation("INSPECT_GUN");
		double[] inspectBarrel = GunAnimationClientState.getRelevantTransformation("INSPECT_BARREL");
		double[] inspectMove = GunAnimationClientState.getRelevantTransformation("INSPECT_MOVE");
		double[] inspectLever = GunAnimationClientState.getRelevantTransformation("INSPECT_LEVER");

		poseStack.translate(0, -1, -4);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float)equip[0])));
		poseStack.translate(0, 1, 4);

		poseStack.translate(0, 0, -4);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float)lift[0])));
		poseStack.translate(0, 0, 4);

		poseStack.translate(0, 0, recoil[2]);

		

		poseStack.pushPose();

		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float)inspectGun[2])));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float)inspectGun[0])));

		GunAnimationClientState.applyRelevantTransformation(poseStack, "Gun");
		renderPart(GunModels.obj("stg77"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");

		poseStack.pushPose();
		GunAnimationClientState.applyRelevantTransformation(poseStack, "Magazine");
		renderPart(GunModels.obj("stg77"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float)inspectLever[2])));
		GunAnimationClientState.applyRelevantTransformation(poseStack, "Lever");
		renderPart(GunModels.obj("stg77"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lever");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, 0, bolt[2]);
		poseStack.pushPose();
		GunAnimationClientState.applyRelevantTransformation(poseStack, "Breech");
		renderPart(GunModels.obj("stg77"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Breech");
		poseStack.popPose();
		poseStack.translate(0.125, 0, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float)handle[2])));
		poseStack.translate(-0.125, 0, 0);
		GunAnimationClientState.applyRelevantTransformation(poseStack, "Handle");
		renderPart(GunModels.obj("stg77"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Handle");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(safety[0], 0, 0);
		GunAnimationClientState.applyRelevantTransformation(poseStack, "Safety");
		renderPart(GunModels.obj("stg77"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Safety");
		poseStack.popPose();

		poseStack.popPose();

		poseStack.pushPose();

		poseStack.translate(inspectMove[0], inspectMove[1], inspectMove[2]);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float)inspectBarrel[0])));
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float)inspectBarrel[2])));
		GunAnimationClientState.applyRelevantTransformation(poseStack, "Gun");
		GunAnimationClientState.applyRelevantTransformation(poseStack, "Barrel");
		renderPart(GunModels.obj("stg77"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
		poseStack.popPose();

		double smokeScale = 0.75;

		poseStack.pushPose();
		poseStack.translate(0, 0, 8);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
		/* TODO(CE:ItemRenderSTG77:smokeNodes) smokeNodes not ported */
		poseStack.popPose();

		

		poseStack.pushPose();
		poseStack.translate(0, 0, 7.5);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		poseStack.scale((float)(0.25), (float)(0.25), (float)(0.25));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (-5 + gun.shotRand * 10)));
		renderGapFlash(poseStack, bufferSource, gun.lastShot[0]);
		poseStack.popPose();
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 1.5D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 1, 2);

	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		double scale = 1.375D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
		poseStack.translate(-0.5, 0.5, 0);
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		
		/* bind */ currentTex = GunModels.tex("stg77_tex");
		renderPart(GunModels.obj("stg77"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("stg77"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
		renderPart(GunModels.obj("stg77"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lever");
		renderPart(GunModels.obj("stg77"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
		renderPart(GunModels.obj("stg77"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Safety");
		renderPart(GunModels.obj("stg77"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Handle");
		renderPart(GunModels.obj("stg77"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Breech");
		
	}
}

