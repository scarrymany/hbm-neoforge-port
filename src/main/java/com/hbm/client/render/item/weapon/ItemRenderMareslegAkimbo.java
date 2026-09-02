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

public class ItemRenderMareslegAkimbo extends ItemRenderGunBase {

	public ItemRenderMareslegAkimbo() {}

	@Override
	public boolean isAkimbo() { return true; }

	@Override
	protected float getTurnMagnitude(ItemStack stack) {
		return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
	}

	@Override
	protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack) {
		poseStack.translate(0, 0, 0.875);
	}

	@Override
	protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		float offset = 0.8F;

		for(int i = -1; i <= 1; i += 2) {

			/* bind */ currentTex = GunModels.tex("maresleg_tex");
			poseStack.pushPose();

			int index = i == -1 ? 0 : 1;

			standardAimingTransform(poseStack, -1.5F * offset * i, -1F * offset, 2F * offset, 0, -3.875 / 8D, 1);

			double scale = 0.375D;
			poseStack.scale((float)(scale), (float)(scale), (float)(scale));

			double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP", index);
			double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL", index);
			double[] lever = GunAnimationClientState.getRelevantTransformation("LEVER", index);
			double[] turn = GunAnimationClientState.getRelevantTransformation("TURN", index);
			double[] flip = GunAnimationClientState.getRelevantTransformation("FLIP", index);
			double[] lift = GunAnimationClientState.getRelevantTransformation("LIFT", index);
			double[] shell = GunAnimationClientState.getRelevantTransformation("SHELL", index);
			double[] flag = GunAnimationClientState.getRelevantTransformation("FLAG", index);

			

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
			poseStack.translate(0, 1, 3.75);
			poseStack.mulPose(Axis.ZN.rotationDegrees((float) ((float) turn[2])));
			poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) flip[0])));
			poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
			/* TODO(CE:ItemRenderMareslegAkimbo:smokeNodes) smokeNodes not ported */
			poseStack.popPose();

			renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");

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
			poseStack.translate(0, 1, 3.75);
			poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
			poseStack.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
			renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[index], 75, 5);
			poseStack.popPose();

			poseStack.popPose();
		}
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		double scale = 1.75D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 0.25, 3);
	}

	@Override
	public void setupThirdPersonAkimbo(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonAkimbo(stack, poseStack);
		double scale = 1.75D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
		poseStack.translate(0, 0.25, 3);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
        // TODO(CE:1.12 leftover) GlStateManager.enableAlpha();
		poseStack.scale((float)(1), (float)(1), (float)(-1));
		poseStack.translate(8, 8, 0);
		double scale = 2.5D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
	}

	public void renderInv(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		
		

		/* bind */ currentTex = GunModels.tex("maresleg_tex");

		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) (225)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
		poseStack.translate(-1, 0, 0);
		renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lever");
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) (-225)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (180)));
		poseStack.translate(1.2, 2.25, 0);
		renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lever");
		poseStack.popPose();

		
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		
		/* bind */ currentTex = GunModels.tex("maresleg_tex");
		renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Gun");
		renderPart(GunModels.obj("maresleg"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Lever");
		
	}
}

