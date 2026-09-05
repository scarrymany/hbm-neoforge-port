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

public class ItemRenderTau extends ItemRenderGunBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
		poseStack.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(poseStack,
				-1.75F * offset, -1.75F * offset, 3.5F * offset,
				-1.75F * offset, -1.75F * offset, 3.5F * offset);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		/* bind */ currentTex = GunModels.tex("tau_tex");
		double scale = 0.75D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] rotate = GunAnimationClientState.getRelevantTransformation("ROTATE");

		poseStack.translate(0, -1, -4);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float)equip[0])));
		poseStack.translate(0, 1, 4);

		poseStack.translate(0, 0, recoil[2]);

		poseStack.translate(0, 0, -2);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float)(recoil[2] * 5))));
		poseStack.translate(0, 0, 2);

		
		

		renderPart(GunModels.obj("tau"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Body");

		poseStack.pushPose();
		poseStack.translate(0, -0.25, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float)rotate[2])));
		poseStack.translate(0, 0.25, 0);
		renderPart(GunModels.obj("tau"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Rotor");
		poseStack.popPose();

		
		
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 2.5D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 1, 2);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		double scale = 2D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
		poseStack.translate(-0.25, 0.5, 0);
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		
		
		/* bind */ currentTex = GunModels.tex("tau_tex");
		renderAll(GunModels.obj("tau"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
		
		
	}
}

