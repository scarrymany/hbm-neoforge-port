package com.hbm.items.weapon.sedna.hud;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Ported from CE's {@code com.hbm.items.weapon.sedna.hud.HUDComponentAmmoCounter} (71 lines, full).
 * Draws the loaded-magazine icon plus its "current / capacity" text next to the hotbar, offset by
 * the running {@code bottomOffset} of every earlier {@link IHUDComponent} on the same gun.
 * <p>
 * Every pixel offset below is copied verbatim from CE's real numbers (<b>not</b> Neo Edition's own
 * simplified rewrite, which drops the {@code left}/offhand-icon-avoidance term entirely) - CE is
 * this port's sole source of truth for behavior/numbers. {@link #getComponentHeight} is CE's real
 * {@code 24}, not Neo Edition's {@code 17}. CE's own trailing
 * {@code mc.getTextureManager().bindTexture(misc)} (a manual GL-texture-rebind after item rendering,
 * needed under 1.12's immediate-mode pipeline) has no 1.21.1 equivalent need -
 * {@link GuiGraphics}'s buffered/{@code RenderType}-based draw calls manage their own texture state,
 * so it is not ported.
 */
public class HUDComponentAmmoCounter implements IHUDComponent {

    protected final int receiver;
    protected boolean mirrored;
    protected boolean noCounter;

    public HUDComponentAmmoCounter(int receiver) {
        this.receiver = receiver;
    }

    public HUDComponentAmmoCounter mirror() {
        this.mirrored = true;
        return this;
    }

    public HUDComponentAmmoCounter noCounter() {
        this.noCounter = true;
        return this;
    }

    @Override
    public int getComponentHeight(Player player, ItemStack stack) {
        return 24;
    }

    @Override
    public void renderHUDComponent(RenderGuiLayerEvent.Pre event, Player player, ItemStack stack, int bottomOffset, int gunIndex) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        Window window = mc.getWindow();

        // CE shifts the mirrored (offhand-side) counter further left when the offhand slot itself
        // holds an item, so the ammo icon doesn't overlap the vanilla offhand-item HUD icon.
        boolean offhandOccupied = !player.getOffhandItem().isEmpty();

        int pX = window.getGuiScaledWidth() / 2 + (mirrored ? -(62 + 36 + 52 + (offhandOccupied ? 29 : 0)) : (62 + 36)) + (noCounter ? 14 : 0);
        int pZ = window.getGuiScaledHeight() - bottomOffset - 23;

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        IMagazine<?> mag = gun.getConfig(stack, gunIndex).getReceivers(stack)[this.receiver].getMagazine(stack);

        if (!noCounter) {
            guiGraphics.drawString(mc.font, mag.reportAmmoStateForHUD(stack, player), pX + 17, pZ + 6, 0xFFFFFF);
        }

        ItemStack icon = mag.getIconForHUD(stack, player);
        if (!icon.isEmpty()) {
            guiGraphics.renderItem(icon, pX, pZ);
        }
    }
}
