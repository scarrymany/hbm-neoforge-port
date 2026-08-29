package com.hbm.interfaces;

import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Forge's RenderGameOverlayEvent + ScaledResolution + GuiIngame trio has no 1:1 NeoForge 1.21
 * equivalent. The confirmed modern replacement (see Neo Edition's HUDComponentDurabilityBar /
 * HUDComponentAmmoCounter, built against net.neoforged.neoforge.client.event.RenderGuiLayerEvent)
 * draws through a single GuiGraphics, with scaled width/height pulled from
 * Minecraft.getInstance().getWindow() inside the implementation instead of being passed in.
 */
public interface IHoldableWeapon {

	Crosshair getCrosshair();

	@OnlyIn(Dist.CLIENT)
	default boolean hasCustomHudElement() {
		return false;
	}

	@OnlyIn(Dist.CLIENT)
	default void renderHud(GuiGraphics guiGraphics, ItemStack stack, float partialTicks) {
	}
}
