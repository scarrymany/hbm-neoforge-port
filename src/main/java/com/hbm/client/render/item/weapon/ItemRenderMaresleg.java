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

public class ItemRenderMaresleg extends ItemRenderGunBase {

	public ResourceLocation texture;

	public ItemRenderMaresleg(ResourceLocation texture) {
		this.texture = texture;	}

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
				-1.25F * offset, -1F * offset, 2F * offset,
				0, -3.875 / 8D, 1);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = texture;
		double scale = 0.375D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		boolean shortened = getShort(stack);

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] lever = GunAnimationClientState.getRelevantTransformation("LEVER");
		double[] turn = GunAnimationClientState.getRelevantTransformation("TURN");
		double[] flip = GunAnimationClientState.getRelevantTransformation("FLIP");
		double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT");
		double[] shell = GunAnimationClientState.getRelevantTransformation("SHELL");
		double[] flag = GunAnimationClientState.getRelevantTransformation("FLAG");

		

		poseStack.translate(recoil[0] * 2, recoil[1], recoil[2]);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) recoil[2] * 5)));
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) turn[2])));

		poseStack.translate(0, 0, -4);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) lift[0])));
		poseStack.translate(0, 0, 4);

		poseStack.translate(0, 0, -4);
		poseStack.mulPose(Axis.XN.rotationDegrees((float) ((float) equip[0])));
		poseStack.translate(0, 0, 4);

		poseStack.translate(0, 0, -2);
		poseStack.mulPose(Axis.XN.rotationDegrees((float) ((float) flip[0])));
		poseStack.translate(0, 0, 2);

		poseStack.pushPose();
		poseStack.translate(0, 1, shortened ? 3.75 : 8);
		poseStack.mulPose(Axis.ZN.rotationDegrees((float) ((float) turn[2])));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) flip[0])));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		/* TODO(CE:ItemRenderMaresleg:smokeNodes) smokeNodes not ported */
		poseStack.popPose();

		renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		if(!shortened) {
			renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Stock");
			renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
		}

		poseStack.pushPose();
		poseStack.translate(0, 0.125, -2.875);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) lever[0])));
		poseStack.translate(0, -0.125, 2.875);
		renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lever");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(shell[0], shell[1] - 0.75, shell[2]);
		renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Shell");
		poseStack.popPose();

		if(flag[0] != 0) {
			poseStack.pushPose();
			poseStack.translate(0, -0.5, 0);
			renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Shell");
			poseStack.popPose();
		}

		

		poseStack.pushPose();
		poseStack.translate(0, 1, shortened ? 3.75 : 8);
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

		if(getShort(stack)) {
			double scale = 2.5D;
			poseStack.scale((float)(scale), (float)(scale), (float)(scale));
			poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
			poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
			poseStack.translate(-1, 0, 0);
		} else {
			double scale = 1.4375D;
			poseStack.scale((float)(scale), (float)(scale), (float)(scale));
			poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
			poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
			poseStack.translate(-0.5, 0.5, 0);
		}
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		
		/* bind */ currentTex = texture;
		renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lever");
		if(!getShort(stack)) {
			renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Stock");
			renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
		}
		
	}

	public boolean getShort(ItemStack stack) {
		return stack.getItem() == GunShotgunItems.GUN_MARESLEG_BROKEN.get() || XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "sawed_off"));
	}
}

