package com.hbm.inventory.gui.bomb;

import com.hbm.inventory.container.bomb.NukeCustomMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Flat-panel GUI for {@link NukeCustomMenu} - shows the 8 live-computed yield categories as a tooltip-free readout. */
public class NukeCustomScreen extends GuiInfoContainer<NukeCustomMenu> {

    public NukeCustomScreen(NukeCustomMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 84, 160, 10,
                Component.literal(String.format("TNT %.1f  Nuke %.1f  Hydro %.1f", be.tnt, be.nuke, be.hydro)));
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 94, 160, 10,
                Component.literal(String.format("Bale %.1f  Dirty %.1f  Schrab %.1f", be.bale, be.dirty, be.schrab)));
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 104, 160, 10,
                Component.literal(String.format("Sol %.1f  Euph %.1f  Falling %s", be.sol, be.euph, be.isFalling())));
    }
}
