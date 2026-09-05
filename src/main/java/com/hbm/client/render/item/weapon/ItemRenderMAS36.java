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

public class ItemRenderMAS36 extends ItemRenderGunBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) {
		return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
	}

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
		return  fov * (1 - aimingProgress * (isScoped(stack) ? 0.66F : 0.33F));
	}

	@Override
	protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
		poseStack.translate(0, 0, 0.875);

		float offset = 0.8F;
		if(isScoped(stack)) {
			standardAimingTransform(poseStack,
					-1.5F * offset, -1.25F * offset, 1.75F * offset,
					-0.2, -5.875 / 8D, 1.125);
		} else {
			standardAimingTransform(poseStack,
					-1.5F * offset, -1.25F * offset, 1.75F * offset,
					0, -4.6825 / 8D, 0.75);
		}
	}

        // TODO(CE:1.12 leftover) private static DoubleBuffer buf = null;

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		boolean isScoped = isScoped(stack);
		if(isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;
        // TODO(CE:1.12 leftover) if(buf == null) buf = GLAllocation.createDirectByteBuffer(8*4).asDoubleBuffer();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = GunModels.tex("mas36_tex");
		double scale = 0.375D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT");
		double[] stock = GunAnimationClientState.getRelevantTransformation("STOCK");
		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] boltTurn = GunAnimationClientState.getRelevantTransformation("BOLT_TURN");
		double[] boltPull = GunAnimationClientState.getRelevantTransformation("BOLT_PULL");
		double[] bullet = GunAnimationClientState.getRelevantTransformation("BULLET");
		double[] showClip = GunAnimationClientState.getRelevantTransformation("SHOW_CLIP");
		double[] clip = GunAnimationClientState.getRelevantTransformation("CLIP");
		double[] bullets = GunAnimationClientState.getRelevantTransformation("BULLETS");
		double[] stab = GunAnimationClientState.getRelevantTransformation("STAB");

		poseStack.translate(0, -3, -3);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) equip[0])));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) lift[0])));
		poseStack.translate(0, 3, 3);

		poseStack.translate(stab[0], stab[1], stab[2]);

		poseStack.translate(0, 0, recoil[2]);

		
		renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		if(hasBayonet(stack))  renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bayonet");

		poseStack.pushPose();
		poseStack.translate(0, 0.3125, -2.125);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) stock[0])));
		poseStack.translate(0, -0.3125, 2.125);
		renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Stock");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, 1.125, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) boltTurn[2])));
		poseStack.translate(0, -1.125, 0);
		poseStack.translate(0, 0, boltPull[2]);
		renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bolt");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(bullet[0], bullet[1], bullet[2]);
		renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bullet");
		poseStack.popPose();

		if(isScoped) renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Scope");

		if(showClip[0] != 0) {
			poseStack.pushPose();
			poseStack.translate(clip[0], clip[1], clip[2]);
			renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Clip");
			poseStack.popPose();
			poseStack.pushPose();
			// TODO(CE:ItemRenderMAS36.java:49) GL clip-plane for stripper clip bullets not ported.
			poseStack.translate(bullets[0], bullets[1], bullets[2]);
			renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bullets");
			poseStack.popPose();
		}

		double smokeScale = 0.25;

		poseStack.pushPose();
		poseStack.translate(0, 1.125, 8);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
		/* TODO(CE:ItemGunBaseNT.java:329) smokeNodes not ported */
		poseStack.popPose();

		

		poseStack.pushPose();
		poseStack.translate(0, 1, 8);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
		poseStack.scale((float)(0.5), (float)(0.5), (float)(0.5));
		renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 7.5);
		poseStack.popPose();
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 1.5D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 0.5, 3);
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

	public void renderModTable(ItemStack stack, int index, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		
		/* bind */ currentTex = GunModels.tex("mas36_tex");
		renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Stock");
		renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bolt");
		if(isScoped(stack)) renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Scope");
		if(hasBayonet(stack)) renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bayonet");
		
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		
		/* bind */ currentTex = GunModels.tex("mas36_tex");
		renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Stock");
		renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bolt");
		if(isScoped(stack)) renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Scope");
		poseStack.translate(0, -1, -6);
		if(hasBayonet(stack)) renderPart(GunModels.obj("mas36"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bayonet");
		
	}

	public boolean isScoped(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "scope"));
	}

	public boolean hasBayonet(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "id_mas_bayonet"));
	}
}

