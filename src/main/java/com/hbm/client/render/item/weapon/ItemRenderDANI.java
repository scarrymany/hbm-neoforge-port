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

public class ItemRenderDANI extends ItemRenderGunBase {

	@Override
	public boolean isAkimbo() {
		return true;
	}

	@Override
	protected float getTurnMagnitude(ItemStack stack) {
		return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
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

			int index = i == -1 ? 0 : 1;
			/* bind */ currentTex = index == 0 ? GunModels.tex("dani_celestial_tex") : GunModels.tex("dani_lunar_tex");

			poseStack.pushPose();

			standardAimingTransform(poseStack,
					-1.5F * offset * i, -0.75F * offset, 1F * offset,
					0, -3.125 / 8D, 0.25);

			double scale = 0.125D;
			poseStack.scale((float)(scale), (float)(scale), (float)(scale));

			
			double[] recoil = GunAnimationClientState.getRelevantTransformation("RECOIL", index);
			double[] reloadMove = GunAnimationClientState.getRelevantTransformation("RELOAD_MOVE", index);
			double[] reloadRot = GunAnimationClientState.getRelevantTransformation("RELOAD_ROT", index);
			double[] equip = GunAnimationClientState.getRelevantTransformation("EQUIP", index);

			poseStack.translate(recoil[0], recoil[1], recoil[2]);
			poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (recoil[2] * 10))));

			poseStack.translate(0, -2, -2);
			poseStack.mulPose(Axis.XN.rotationDegrees((float) ((float) equip[0])));
			poseStack.translate(0, 2, 2);

			poseStack.pushPose();
			poseStack.translate(0, 1.5, 9.25);
			poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (-recoil[2] * 10))));
			poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
			/* TODO(CE:ItemRenderDANI:smokeNodes) smokeNodes not ported */
			poseStack.popPose();

			poseStack.translate(reloadMove[0], reloadMove[1], reloadMove[2]);

			poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) reloadRot[0])));
			poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) (reloadRot[2] * i))));
			poseStack.mulPose(Axis.YP.rotationDegrees((float) ((float) (reloadRot[1] * i))));
			renderPart(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Grip");

			poseStack.pushPose(); /// FRONT PUSH ///
			poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) GunAnimationClientState.getRelevantTransformation("FRONT", index)[2])));
			renderPart(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Barrel");
			poseStack.pushPose(); /// LATCH PUSH ///
			poseStack.translate(0, 2.3125, -0.875);
			poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) GunAnimationClientState.getRelevantTransformation("LATCH", index)[2])));
			poseStack.translate(0, -2.3125, 0.875);
			renderPart(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Latch");
			poseStack.popPose(); /// LATCH POP ///

			poseStack.pushPose(); /// DRUM PUSH ///
			poseStack.translate(0, 1, 0);
			poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((float) (GunAnimationClientState.getRelevantTransformation("DRUM", index)[2] * 60))));
			poseStack.translate(0, -1, 0);
			poseStack.translate(0, 0, GunAnimationClientState.getRelevantTransformation("DRUM_PUSH", index)[2]);
			renderPart(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Drum");
			poseStack.popPose(); /// DRUM POP ///

			poseStack.popPose(); /// FRONT POP ///

			poseStack.pushPose(); /// HAMMER ///
			poseStack.translate(0, 0, -4.5);
			poseStack.mulPose(Axis.XP.rotationDegrees((float) ((float) (-45 + 45 * GunAnimationClientState.getRelevantTransformation("HAMMER", index)[2]))));
			poseStack.translate(0, 0, 4.5);
			renderPart(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "Hammer");
			poseStack.popPose();

			

			poseStack.pushPose();
			poseStack.translate(0, 1.5, 9.25);
			poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
			renderMuzzleFlash(poseStack, bufferSource, gun.lastShot[index], 75, 7.5);
			poseStack.popPose();

			poseStack.popPose();
		}
	}

	@Override
	protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonGun(stack, poseStack);
		poseStack.translate(0, 1, 3);
	}

	@Override
	public void setupThirdPersonAkimbo(ItemStack stack, PoseStack poseStack) {
		super.setupThirdPersonAkimbo(stack, poseStack);
		poseStack.translate(0, 1, 3);
	}

	@Override
	protected void setupInventoryGun(ItemStack stack, PoseStack poseStack) {
		poseStack.scale((float)(1), (float)(1), (float)(-1));
		poseStack.translate(8, 6, 0);
		double scale = 1.125D;
		poseStack.scale((float)(scale), (float)(scale), (float)(scale));
	}

	public void renderInv(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();

		
		

		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) (225F)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90F)));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25F)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45F)));
		poseStack.translate(2, 0, 0);
		/* bind */ currentTex = GunModels.tex("dani_celestial_tex");
		renderAll(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) (-225)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (90)));
		poseStack.mulPose(Axis.XP.rotationDegrees((float) (25)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (45)));
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (180)));
		poseStack.translate(2, 0, 0);
		/* bind */ currentTex = GunModels.tex("dani_lunar_tex");
		renderAll(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
		poseStack.popPose();

		
	}

	public void renderEquipped(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		
		/* bind */ currentTex = GunModels.tex("dani_lunar_tex");
		renderAll(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
		
	}

	public void renderEquippedAkimbo(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		
		/* bind */ currentTex = GunModels.tex("dani_celestial_tex");
		renderAll(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
		
	}

	public void renderModTable(ItemStack stack, int index, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		

		
		/* bind */ currentTex = index == 1 ? GunModels.tex("dani_celestial_tex") : GunModels.tex("dani_lunar_tex");
		renderAll(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
		
	}

	@Override
	protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation currentTex = this.defaultTex();
		
		/* bind */ currentTex = GunModels.tex("dani_celestial_tex");
		renderAll(GunModels.obj("bio_revolver"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);
		
	}
}

