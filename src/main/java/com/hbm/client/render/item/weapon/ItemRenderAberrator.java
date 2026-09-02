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

public class ItemRenderAberrator extends ItemRenderGunBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
		return  fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
		poseStack.translate(0, 0, 1);
		
		float offset = 0.8F;
		standardAimingTransform(poseStack,
				-1.0F * offset, -1.25F * offset, 1.25F * offset,
				0, -5.25 / 8D, 0.125);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = GunModels.tex("aberrator_tex");
		double scale = 0.25D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] rise = GunAnimationClientState.getRelevantTransformation("RISE");
		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] slide = GunAnimationClientState.getRelevantTransformation("SLIDE");
		double[] bullet = GunAnimationClientState.getRelevantTransformation("BULLET");
		double[] hammer = GunAnimationClientState.getRelevantTransformation("HAMMER");
		double[] roll = GunAnimationClientState.getRelevantTransformation("ROLL");
		double[] mag = GunAnimationClientState.getRelevantTransformation("MAG");
		double[] magroll = GunAnimationClientState.getRelevantTransformation("MAGROLL");
		double[] sight = GunAnimationClientState.getRelevantTransformation("SIGHT");

		poseStack.translate(0, rise[1], 0);

		poseStack.translate(0, 1, -2.25);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
		poseStack.translate(0, -1, 2.25);

		poseStack.translate(0, -1, -4);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) recoil[0])));
		poseStack.translate(0, 1, 4);

		poseStack.translate(0, 1, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) roll[2])));
		poseStack.translate(0, -1, 0);

		

		renderPart(GunModels.obj("aberrator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");

		poseStack.pushPose();
		poseStack.translate(0, 2.4375, -1.9375);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) sight[0])));
		poseStack.translate(0, -2.4375, 1.9375);
		renderPart(GunModels.obj("aberrator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Sight");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(mag[0], mag[1], mag[2]);
		poseStack.translate(0, 1, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) magroll[2])));
		poseStack.translate(0, -1, 0);
		renderPart(GunModels.obj("aberrator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
		poseStack.translate(bullet[0], bullet[1], bullet[2]);
		renderPart(GunModels.obj("aberrator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bullet");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, 0, slide[2]);
		renderPart(GunModels.obj("aberrator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Slide");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, 1.25, -3.625);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (-45 + hammer[0]))));
		poseStack.translate(0, -1.25, 3.625);
		renderPart(GunModels.obj("aberrator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hammer");
		poseStack.popPose();

		double smokeScale = 0.5;

		poseStack.pushPose();
		poseStack.translate(0, 2, 4);
		poseStack.mulPose(Axis.XN.rotationDegrees((float) ((float) recoil[0])));
		poseStack.mulPose(Axis.ZN.rotationDegrees((float) ((float) roll[2])));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
		poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
		/* TODO(CE:ItemRenderAberrator:smokeNodes) smokeNodes not ported */
		poseStack.popPose();

		

		poseStack.pushPose();
		poseStack.translate(0, 2, 4);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (90F * gun.shotRand)));
		poseStack.scale((float)(0.75), (float)(0.75), (float)(0.75));
		renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 7.5);
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, 2, -1.5);
		poseStack.scale((float)(0.5), (float)(0.5), (float)(0.5));
		renderFireball(poseStack, bufferSource, gun.lastShot[0]);
		poseStack.popPose();

        // TODO(CE:ItemRenderAberrator.java:135) golden-sword IBakedModel orbit quads are 1.12 Tessellator.
	}


	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		poseStack.translate(0, -1, 4);
		double scale = 1.5D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		double scale = 2.5D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25F)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45F)));
		poseStack.translate(-0.5, -1, 0);
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		
        // TODO(CE:1.12 leftover) GlStateManager.enableAlpha();

		
		/* bind */ currentTex = GunModels.tex("aberrator_tex");
		renderPart(GunModels.obj("aberrator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("aberrator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hammer");
		renderPart(GunModels.obj("aberrator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
		renderPart(GunModels.obj("aberrator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Slide");
		renderPart(GunModels.obj("aberrator"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Sight");
		
	}

	// TODO(CE:ItemRenderAberrator.java:213) 1.12 Tessellator fireball; CE gap-flash plume is the 1.21 stand-in.
	public static void renderFireball(PoseStack poseStack, MultiBufferSource bufferSource, long lastShot) {
		renderGapFlash(poseStack, bufferSource, lastShot);
	}
}
