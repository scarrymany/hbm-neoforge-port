package com.hbm.items.weapon.sedna.hud;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * Ported from CE's {@code com.hbm.items.weapon.sedna.hud.IHUDComponent} (11 lines, full) - a single
 * stackable widget in a Sedna gun's {@code GunConfig#hud(IHUDComponent...)} list (ammo counter,
 * durability bar, ...), rendered by {@code ItemGunBaseNT#renderHUD} in the order supplied, each one
 * offset downward by the running sum of every earlier component's {@link #getComponentHeight}.
 * <p>
 * 1.21.1 API swap (cross-confirmed against Neo Edition's own compiling
 * {@code com.hbm.items.weapon.sedna.hud.IHUDComponent}, same package/shape, at this port's exact
 * {@code neo_version}): CE's {@code (RenderGameOverlayEvent.Pre, ElementType, EntityPlayer, ...)}
 * collapses to {@code (RenderGuiLayerEvent.Pre, Player, ...)} - the vanilla-layer identity CE read
 * off a separate {@code ElementType} enum parameter is read directly off the event itself now
 * ({@link RenderGuiLayerEvent.Pre#getName()}), so implementations filter on
 * {@code event.getName().equals(VanillaGuiLayers.HOTBAR)} instead of a type-equality check.
 */
public interface IHUDComponent {

    int getComponentHeight(Player player, ItemStack stack);

    void renderHUDComponent(RenderGuiLayerEvent.Pre event, Player player, ItemStack stack, int bottomOffset, int gunIndex);
}
