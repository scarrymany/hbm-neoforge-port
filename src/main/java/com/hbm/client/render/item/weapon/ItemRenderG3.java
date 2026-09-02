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

public class ItemRenderG3 extends ItemRenderGunBase {

	public ResourceLocation texture;

	public ItemRenderG3(ResourceLocation texture) {
		this.texture = texture;
	}

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
		return fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
		poseStack.translate(0, 0, 0.875);
		boolean isScoped = this.isScoped(stack);
		float offset = 0.8F;
		standardAimingTransform(poseStack,
				-1.25F * offset, -1F * offset, 2.75F * offset,
				0, isScoped ? (-5.53125 / 8D) : (-3.5625 / 8D), isScoped ? 1.46875 : 1.75);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		boolean isScoped = this.isScoped(stack);
		if(isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = GunModels.tex("g3_tex");
		double scale = 0.375D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT");
		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] mag = GunAnimationClientState.getRelevantTransformation("MAG");
		double[] speen = GunAnimationClientState.getRelevantTransformation("SPEEN");
		double[] bolt = GunAnimationClientState.getRelevantTransformation("BOLT");
		double[] plug = GunAnimationClientState.getRelevantTransformation("PLUG");
		double[] handle = GunAnimationClientState.getRelevantTransformation("HANDLE");
		double[] bullet = GunAnimationClientState.getRelevantTransformation("BULLET");

		poseStack.translate(0, -2, -6);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (equip[0])));
		poseStack.translate(0, 2, 6);

		poseStack.translate(0, 0, -4);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (lift[0])));
		poseStack.translate(0, 0, 4);

		poseStack.translate(0, 0, recoil[2]);

		

		renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Rifle");
		if(hasStock(stack)) renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Stock");
		boolean silenced = hasSilencer(stack);
		if(!silenced) renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Flash_Hider");
		renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Trigger");

		poseStack.pushPose();
		poseStack.translate(mag[0], mag[1], mag[2]);
		poseStack.translate(0, -1.75, -0.5);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) (speen[2])));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (speen[1])));
		poseStack.translate(0, 1.75, 0.5);
		renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
		if(bullet[0] == 0) renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Bullet");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, 0, bolt[2]);
		renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Guide_And_Bolt");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, 0.625, plug[2]);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) (handle[2])));
		poseStack.translate(0, -0.625, 0);
		renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Plug");

		poseStack.translate(0, 0.625, 5.25);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) (22.5)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (handle[1])));
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) (-22.5)));
		poseStack.translate(0, -0.625, -5.25);
		renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Handle");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0, -0.875, -3.5);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (-30 * (1 - ItemGunBaseNT.getMode(stack, 0)))));
		poseStack.translate(0, 0.875, 3.5);
		renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Selector");
		poseStack.popPose();

		if(silenced || isScoped) {
			/* bind */ currentTex = GunModels.tex("g3_attachments");
			if(silenced) renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Silencer");
			if(isScoped) renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Scope");
		}

		if(!silenced) {
			double smokeScale = 0.75;

			poseStack.pushPose();
			poseStack.translate(0, 0, 13);
			poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
			poseStack.scale((float)(smokeScale), (float)(smokeScale), (float)(smokeScale));
			/* TODO(CE:ItemRenderG3:smokeNodes) smokeNodes not ported */
			poseStack.popPose();

			

			poseStack.pushPose();
			poseStack.translate(0, 0, 12);
			poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
			poseStack.mulPose(Axis.XP.rotationDegrees((float) (-25 + gun.shotRand * 10)));
			poseStack.scale((float)(0.75), (float)(0.75), (float)(0.75));
			renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[0], 75, 10);
			poseStack.popPose();
		}
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 1D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 2, 4);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		if(hasStock(stack)) {
			double scale = 0.875D;
			poseStack.scale((float)(scale), (float)(scale), (float)(scale));
			poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
			poseStack.mulPose(Axis.YP.rotationDegrees((float) (hasSilencer(stack) ? 50 : 45)));
			poseStack.translate(hasSilencer(stack) ? 0.75 : -0.5, 0.5, 0);
		} else {
			double scale = 1.125D;
			poseStack.scale((float)(scale), (float)(scale), (float)(scale));
			poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
			poseStack.mulPose(Axis.YP.rotationDegrees((float) (hasSilencer(stack) ? 55 : 45))); //preserves proportions whilst limiting size
			poseStack.translate(2.5, 0.5, 0);
		}
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		boolean silenced = hasSilencer(stack);
		boolean isScoped = this.isScoped(stack);

		
        // TODO(CE:1.12 leftover) Minecraft.getMinecraft().renderEngine.bindTexture(getTexture(stack));
		renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Rifle");
		if(hasStock(stack)) renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Stock");
		renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Magazine");
		if(!silenced)renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Flash_Hider");
		renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Guide_And_Bolt");
		renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Handle");
		renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Trigger");

		poseStack.pushPose();
		poseStack.translate(0, -0.875, -3.5);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (-30)));
		poseStack.translate(0, 0.875, 3.5);
		renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Selector");
		poseStack.popPose();

		if(silenced || isScoped) {
			/* bind */ currentTex = GunModels.tex("g3_attachments");
			if(silenced) renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Silencer");
			if(isScoped) renderPart(GunModels.obj("g3"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Scope");
		}
		
	}

	public boolean hasStock(ItemStack stack) {
		return !XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "no_stock"));
	}

	public boolean hasSilencer(ItemStack stack) {
		return stack.getItem() == GunRifleItems.GUN_G3_ZEBRA.get() || XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "silencer"));
	}

	public boolean isScoped(ItemStack stack) {
		return stack.getItem() == GunRifleItems.GUN_G3_ZEBRA.get() || XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "scope"));
	}

	public ResourceLocation getTexture(ItemStack stack) {
		if(XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "furniture_green"))) return GunModels.tex("g3_green_tex");
		if(XWeaponModManager.hasUpgrade(stack, 0, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "furniture_black"))) return GunModels.tex("g3_black_tex");
		return texture;
	}
}

