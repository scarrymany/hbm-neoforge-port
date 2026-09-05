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
import com.hbm.items.weapon.sedna.mags.IMagazine;

public class ItemRenderChemthrower extends ItemRenderGunBase {

	public ItemRenderChemthrower() {}

	@Override
	protected float getTurnMagnitude(ItemStack stack) {
		return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
	}

	@Override
	protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
		poseStack.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(poseStack,
				-2F * offset, -2F * offset, 2.5F * offset,
				0, -4.375 / 8D, 1);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = GunModels.tex("chemthrower_tex");
		double scale = 0.75D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");

		poseStack.translate(0, -2, -4);
		poseStack.mulPose(Axis.XN.rotationDegrees((float) ((float) equip[0])));
		poseStack.translate(0, 2, 4);

		

		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
		renderPart(GunModels.obj("chemthrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("chemthrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hose");
		renderPart(GunModels.obj("chemthrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Nozzle");

		poseStack.translate(0, 0.875, 1.75);
		IMagazine mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
		double d = (double) mag.getAmount(stack, clientInv()) / (double) mag.getCapacity(stack);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (135 - d * 270))));
		poseStack.translate(0, -0.875, -1.75);

		renderPart(GunModels.obj("chemthrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gauge");

		
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 2D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, -2.5, 0.5);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		double scale = 2D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25F)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45F)));
		poseStack.translate(0.875, 0, 0);
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
		
		/* bind */ currentTex = GunModels.tex("chemthrower_tex");
		renderPart(GunModels.obj("chemthrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("chemthrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hose");
		renderPart(GunModels.obj("chemthrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Nozzle");
		renderPart(GunModels.obj("chemthrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gauge");
		
	}
}

