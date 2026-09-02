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

public class ItemRenderFlamer extends ItemRenderGunBase {

	public ResourceLocation texture;

	public ItemRenderFlamer(ResourceLocation texture) {
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
				-1.5F * offset, -1.5F * offset, 2.75F * offset,
				0, -4.625 / 8D, 0.25);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = texture;
		double scale = 0.375D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] rotate = GunAnimationClientState.getRelevantTransformation("ROTATE");

		poseStack.translate(0, 2, -6);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) -equip[0])));
		poseStack.translate(0, -2, 6);

		poseStack.translate(0, 1, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) rotate[2])));
		poseStack.translate(0, -1, 0);

		

		poseStack.pushPose();
		GunAnimationClientState.applyRelevantTransformation(poseStack, "Gun");
		renderPart(GunModels.obj("flamethrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		if(hasShield(stack)) renderPart(GunModels.obj("flamethrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "HeatShield");
		poseStack.popPose();

		poseStack.pushPose();
		GunAnimationClientState.applyRelevantTransformation(poseStack, "Tank");
		renderPart(GunModels.obj("flamethrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Tank");
		poseStack.popPose();

		poseStack.pushPose();
		GunAnimationClientState.applyRelevantTransformation(poseStack, "Gauge");
		poseStack.translate(1.25, 1.25, 0);
		IMagazine mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) (-135 + (mag.getAmount(stack, clientInv()) * 270D / mag.getCapacity(stack))))));
		poseStack.translate(-1.25, -1.25, 0);
		renderPart(GunModels.obj("flamethrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gauge");
		poseStack.popPose();

		
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 1.75D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, -3, 4);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		double scale = 1.25D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25F)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45F)));
		poseStack.translate(-1, 1, 0);
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		
		/* bind */ currentTex = texture;
		renderPart(GunModels.obj("flamethrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("flamethrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Tank");
		renderPart(GunModels.obj("flamethrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gauge");
		if(hasShield(stack)) renderPart(GunModels.obj("flamethrower"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "HeatShield");
		
	}

	public boolean hasShield(ItemStack stack) {
		return stack.getItem() == GunHeavyItems.GUN_FLAMER_DAYBREAKER.get();
	}
}

