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

public class ItemRenderAtlas extends ItemRenderGunBase {

	public ResourceLocation texture;

	public ItemRenderAtlas(ResourceLocation texture) {
		this.texture = texture;	}

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
				-1.0F * offset, -0.75F * offset, 1F * offset,
				0, -3.125 / 8D, 0.25);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = texture;
		double scale = 0.125D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		
		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] reloadMove = GunAnimationClientState.getRelevantTransformation("RELOAD_MOVE");
		double[] reloadRot = GunAnimationClientState.getRelevantTransformation("RELOAD_ROT");
		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");

		poseStack.translate(recoil[0], recoil[1], recoil[2]);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (recoil[2] * 10))));

		poseStack.translate(0, 0, -7);
		poseStack.mulPose(Axis.XN.rotationDegrees((float) ((float) equip[0])));
		poseStack.translate(0, 0, 7);

		poseStack.pushPose();
		poseStack.translate(0, 1.5, 9.25);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (-recoil[2] * 10))));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
		/* TODO(CE:ItemRenderAtlas:smokeNodes) smokeNodes not ported */
		poseStack.popPose();

		poseStack.translate(reloadMove[0], reloadMove[1], reloadMove[2]);

		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) reloadRot[0])));
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) reloadRot[2])));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) ((float) reloadRot[1])));
		renderPart(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Grip");

		poseStack.pushPose(); /// FRONT PUSH ///
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) GunAnimationClientState.getRelevantTransformation("FRONT")[2])));
		renderPart(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
		poseStack.pushPose(); /// LATCH PUSH ///
		poseStack.translate(0, 2.3125, -0.875);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) GunAnimationClientState.getRelevantTransformation("LATCH")[2])));
		poseStack.translate(0, -2.3125, 0.875);
		renderPart(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Latch");
		poseStack.popPose(); /// LATCH POP ///

		poseStack.pushPose(); /// DRUM PUSH ///
		poseStack.translate(0, 1, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) (GunAnimationClientState.getRelevantTransformation("DRUM")[2] * 60))));
		poseStack.translate(0, -1, 0);
		poseStack.translate(0, 0, GunAnimationClientState.getRelevantTransformation("DRUM_PUSH")[2]);
		renderPart(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Drum");
		poseStack.popPose(); /// DRUM POP ///

		poseStack.popPose(); /// FRONT POP ///

		poseStack.pushPose(); /// HAMMER ///
		poseStack.translate(0, 0, -4.5);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (-45 + 45 * GunAnimationClientState.getRelevantTransformation("HAMMER")[2]))));
		poseStack.translate(0, 0, 4.5);
		renderPart(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hammer");
		poseStack.popPose();

		

		poseStack.pushPose();
		poseStack.translate(0, 1.5, 9.25);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
		renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 7.5);
		poseStack.popPose();
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 0.75D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 1, 3);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		double scale = 1.125D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25F)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45F)));
		poseStack.translate(-0.5, 1.5, 0);
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		
		/* bind */ currentTex = texture;
		renderAll(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
		
	}
}

