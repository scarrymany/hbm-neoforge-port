package com.hbm.interfaces;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * Ported from Forge's RenderGameOverlayEvent.Pre + ElementType pair. NeoForge's replacement event,
 * RenderGuiLayerEvent.Pre (net.neoforged.neoforge.client.event, confirmed in Neo Edition's HUD
 * component classes), identifies the layer being drawn via event.getName() instead of an
 * ElementType enum, so that's what gets passed through as "layer" here.
 */
public interface IItemHUD {

	void renderHUD(RenderGuiLayerEvent.Pre event, ResourceLocation layer, Player player, ItemStack stack, InteractionHand hand);

}
