package com.hbm.inventory.gui.machine.chem;

import com.hbm.blockentity.machine.chem.SilexBlockEntity;
import com.hbm.inventory.container.machine.chem.SilexMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Ported (visually, from CE's {@code GUISILEX}) as a plain panel. No upgrade slots, matching CE. */
public class SilexScreen extends GuiInfoContainer<SilexMenu> {

    public SilexScreen(SilexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 232;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        var be = this.getMenu().be;
        be.tank.renderTank(x + 8, y + 90, 0, 16, 54);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 6, 220, 10,
                Component.literal("Laser: " + be.mode),
                Component.literal("Charge: " + be.currentFill + "/" + SilexBlockEntity.MAX_FILL));
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 90 - 54, 16, 54);
    }
}
