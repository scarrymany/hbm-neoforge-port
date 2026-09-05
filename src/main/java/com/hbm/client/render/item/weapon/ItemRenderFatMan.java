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

public class ItemRenderFatMan extends ItemRenderGunBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) {
		return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
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
				-1.5F * offset, -1.25F * offset, 0.5F * offset,
				-1F * offset, -1.25F * offset, 0F * offset);
	}

	protected static String label = "AUTO";

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = GunModels.tex("fatman_tex");
		double scale = 0.5D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		boolean isLoaded = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) > 0;

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] lid = GunAnimationClientState.getRelevantTransformation("LID");
		double[] nuke = GunAnimationClientState.getRelevantTransformation("NUKE");
		double[] piston = GunAnimationClientState.getRelevantTransformation("PISTON");
		double[] handle = GunAnimationClientState.getRelevantTransformation("HANDLE");
		double[] gauge = GunAnimationClientState.getRelevantTransformation("GAUGE");

		poseStack.translate(0, 1, -2);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
		poseStack.translate(0, -1, 2);

		

		renderPart(GunModels.obj("fatman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Launcher");

		poseStack.pushPose();
		poseStack.translate(0, 0, handle[2]);
		renderPart(GunModels.obj("fatman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Handle");

		poseStack.translate(0.4375, -0.875, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) gauge[2])));
		poseStack.translate(-0.4375, 0.875, 0);
		renderPart(GunModels.obj("fatman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gauge");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0.25, 0.125, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) lid[2])));
		poseStack.translate(-0.25, -0.125, 0);
		renderPart(GunModels.obj("fatman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lid");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, 0, piston[2]);
		if(!isLoaded && piston[2] == 0) poseStack.translate(0, 0, 3);
		renderPart(GunModels.obj("fatman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Piston");
		poseStack.popPose();

		if(isLoaded || nuke[0] != 0 || nuke[1] != 0 || nuke[2] != 0) {
			poseStack.pushPose();
			poseStack.translate(nuke[0], nuke[1], nuke[2]);
			currentTex = GunModels.tex("fatman_mininuke_tex");
			renderPart(GunModels.obj("fatman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "MiniNuke");
			// TODO(CE:ItemRenderFatMan.java:143) balefire glint uses 1.12 RenderMiscEffects.
			poseStack.popPose();
		}

		
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 2.5D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(-0.5, 0.5, -3);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		double scale = 1.375D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25F)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45F)));
		poseStack.translate(0, -0.5, 0);
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		/* bind */ currentTex = GunModels.tex("fatman_tex");

		
		renderPart(GunModels.obj("fatman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Launcher");
		renderPart(GunModels.obj("fatman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Handle");
		renderPart(GunModels.obj("fatman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gauge");
		renderPart(GunModels.obj("fatman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lid");
		renderPart(GunModels.obj("fatman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Piston");
		/* bind */ currentTex = GunModels.tex("fatman_mininuke_tex");
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		if(gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) > 0) {
			renderPart(GunModels.obj("fatman"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "MiniNuke");
		}
		
	}

}

