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

public class ItemRenderTeslaCannon extends ItemRenderGunBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

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
				-1.75F * offset, -0.5F * offset, 1.75F * offset,
				-1.3125F * offset, 0F * offset, -0.5F * offset);
	}

	protected static String label = "AUTO";

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		/* bind */ currentTex = GunModels.tex("tesla_cannon_tex");
		double scale = 0.75D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));

		double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP");
		double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL");
		double[] cycle = GunAnimationClientState.getRelevantTransformation("CYCLE");
		double[] count = GunAnimationClientState.getRelevantTransformation("COUNT");
		double[] yomi = GunAnimationClientState.getRelevantTransformation("YOMI");
		double[] squeeze = GunAnimationClientState.getRelevantTransformation("SQUEEZE");

		poseStack.translate(0, -2, -2);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float)equip[0])));
		poseStack.translate(0, 2, 2);

		poseStack.translate(0, 0, recoil[2]);
		poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float)(recoil[2] * 2))));

		

		int amount = Math.max((int) count[0], gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, clientInv()));

		renderPart(GunModels.obj("tesla_cannon"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("tesla_cannon"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Extension");

		double cogAngle = cycle[2];

		poseStack.pushPose();
		poseStack.translate(0, -1.625, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float)cogAngle)));
		poseStack.translate(0, 1.625, 0);
		renderPart(GunModels.obj("tesla_cannon"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Cog");
		poseStack.popPose();

		poseStack.pushPose();

		poseStack.translate(0, -1.625, 0);
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float)cogAngle)));
		poseStack.translate(0, 1.625, 0);

		for (int i = 0; i < Math.min(amount, 8); i++) {
			renderPart(GunModels.obj("tesla_cannon"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Capacitor");

			if (i < 4) {
				poseStack.translate(0, -1.625, 0);
				poseStack.mulPose(Axis.ZP.rotationDegrees((float) (-22.5F)));
				poseStack.translate(0, 1.625, 0);
			} else {
				if (i == 4) {
					poseStack.translate(0, -1.625, 0);
					poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float)-cogAngle)));
					poseStack.translate(0, 1.625, 0);
					poseStack.translate(-cogAngle * 0.5 / 22.5, 0, 0);
				}
				poseStack.translate(0.5, 0, 0);
			}
		}
		poseStack.popPose();

		

		poseStack.pushPose();
		poseStack.translate(yomi[0], yomi[1], yomi[2]);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (135F)));
		poseStack.scale((float)(squeeze[0]), (float)(squeeze[1]), (float)(squeeze[2]));
/* TODO(CE:ItemRenderTeslaCannon.java:106) RenderPlushie.yomiTex not ported */
		renderAll(GunModels.obj("plushie_yomi"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
		poseStack.popPose();
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 2.75D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 1.5, 1);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		super.setupInventoryGun(stack, poseStack);
		double scale = 1.25D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
		poseStack.translate(0, 0.5, 0);
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		/* bind */ currentTex = GunModels.tex("tesla_cannon_tex");

		
		renderPart(GunModels.obj("tesla_cannon"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("tesla_cannon"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Extension");
		renderPart(GunModels.obj("tesla_cannon"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Cog");

		poseStack.pushPose();
		for (int i = 0; i < 10; i++) {
			renderPart(GunModels.obj("tesla_cannon"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Capacitor");

			if (i < 4) {
				poseStack.translate(0, -1.625, 0);
				poseStack.mulPose(Axis.ZP.rotationDegrees((float) (-22.5F)));
				poseStack.translate(0, 1.625, 0);
			} else {
				poseStack.translate(0.5, 0, 0);
			}
		}
		poseStack.popPose();
		
	}
}

