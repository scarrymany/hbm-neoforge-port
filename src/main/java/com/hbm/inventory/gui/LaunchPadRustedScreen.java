package com.hbm.inventory.gui;

import com.hbm.inventory.container.LaunchPadRustedMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for {@link LaunchPadRustedMenu}, ported from CE's {@code GUILaunchPadRusted} at the
 * signature/layout level (see {@link LaunchPadScreen}'s javadoc for the same caveat). No power/fuel
 * gauges - the rusted pad's unlock condition is item-presence-based
 * ({@code launch_code}/{@code launch_key}), not power/fuel, matching
 * {@link com.hbm.blockentity.bomb.LaunchPadRustedBlockEntity}'s own scope.
 */
public class LaunchPadRustedScreen extends GuiInfoContainer<LaunchPadRustedMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/bomb/gui_launch_pad_rusted.png");

    public LaunchPadRustedScreen(LaunchPadRustedMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
