package com.hbm.inventory.gui.machine.rbmk;

import com.hbm.inventory.container.machine.rbmk.RBMKOutgasserMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * CE {@code GUIRBMKOutgasser} — flux progress + gas tank readout. Fill-panel like sibling RBMK screens
 * (no CE texture in this tree).
 */
public class RBMKOutgasserScreen extends GuiInfoContainer<RBMKOutgasserMenu> {

    public RBMKOutgasserScreen(RBMKOutgasserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xC0C6C6C6);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        var be = getMenu().be;
        guiGraphics.drawString(this.font,
                String.format("Flux %.1f / %d", be.progress, be.duration),
                leftPos + 8, topPos + 20, 0x404040, false);
        guiGraphics.drawString(this.font,
                "Gas: " + be.gas.getFill() + "/" + be.gas.getMaxFill() + "mB " + be.gas.getTankType().getName(),
                leftPos + 8, topPos + 32, 0x404040, false);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
