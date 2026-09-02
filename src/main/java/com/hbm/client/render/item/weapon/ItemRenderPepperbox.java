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

public class ItemRenderPepperbox extends ItemRenderGunBase {

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
		poseStack.translate(0, 0, 1.5);

		float offset = 0.8F;
		standardAimingTransform(poseStack,
				-1.25F * offset, -0.75F * offset, 1F * offset,
				0, -2.5 / 8D, 0.5);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		double scale = 0.25D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		

		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] cylinder = GunAnimationClientState.getRelevantTransformation("ROTATE");
		double[] hammer = GunAnimationClientState.getRelevantTransformation("HAMMER");
		double[] trigger = GunAnimationClientState.getRelevantTransformation("TRIGGER");
		double[] translate = GunAnimationClientState.getRelevantTransformation("TRANSLATE");
		double[] loader = GunAnimationClientState.getRelevantTransformation("LOADER");
		double[] shot = GunAnimationClientState.getRelevantTransformation("SHOT");

		poseStack.translate(translate[0], translate[1], translate[2]);

		poseStack.translate(0, 0, -5);
		poseStack.mulPose(Axis.XN.rotationDegrees((float) ((float) recoil[0])));
		poseStack.translate(0, 0, 5);

		poseStack.pushPose();
		poseStack.translate(0, 0.5, 7);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		/* TODO(CE:ItemRenderPepperbox:smokeNodes) smokeNodes not ported */
		poseStack.popPose();

		/* bind */ currentTex = GunModels.tex("pepperbox_tex");

		if(loader[0] != 0 || loader[1] != 0 || loader[2] != 0) {
			poseStack.pushPose();
			poseStack.translate(loader[0], loader[1], loader[2]);
			renderPart(GunModels.obj("pepperbox"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Speedloader");
			if(shot[0] != 0) renderPart(GunModels.obj("pepperbox"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Shot");
			poseStack.popPose();
		}

		renderPart(GunModels.obj("pepperbox"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Grip");

		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) cylinder[0])));
		renderPart(GunModels.obj("pepperbox"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Cylinder");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, 0.375, -1.875);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) hammer[0])));
		poseStack.translate(0, -0.375, 1.875);
		renderPart(GunModels.obj("pepperbox"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hammer");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, 0, -trigger[0] * 0.5);
		renderPart(GunModels.obj("pepperbox"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Trigger");
		poseStack.popPose();

		

		poseStack.pushPose();
		poseStack.translate(0, 0.5, 7);
		poseStack.scale((float)(0.5), (float)(0.5), (float)(0.5));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
		renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0]);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (45)));
		renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0]);
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
		poseStack.translate(0.5, 0.5, 0);
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		

		
		/* bind */ currentTex = GunModels.tex("pepperbox_tex");
		renderPart(GunModels.obj("pepperbox"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Grip");
		renderPart(GunModels.obj("pepperbox"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Cylinder");
		renderPart(GunModels.obj("pepperbox"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hammer");
		renderPart(GunModels.obj("pepperbox"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Trigger");
		
	}
}

