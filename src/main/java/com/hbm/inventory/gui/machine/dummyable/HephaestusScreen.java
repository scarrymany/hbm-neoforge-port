package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineHephaestusBlockEntity;
import com.hbm.inventory.container.machine.dummyable.HephaestusMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Live input/output tanks + geothermal heat. CE was overlay-only. */
public class HephaestusScreen extends GuiInfoContainer<HephaestusMenu> {

    public HephaestusScreen(HephaestusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MachineHephaestusBlockEntity be = this.getMenu().be;
        be.input.renderTank(x + 35, y + 70, 0, 16, 52);
        be.output.renderTank(x + 80, y + 70, 0, 16, 52);
        int hh = Math.min(52, be.bufferedHeat * 52 / Math.max(1, 2_000));
        guiGraphics.fill(x + 125, y + 70 - hh, x + 141, y + 70, 0xFFFF4400);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineHephaestusBlockEntity be = this.getMenu().be;
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 18, 16, 52);
        be.output.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 18, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 125, topPos + 18, 16, 52,
                Component.literal("Heat: " + be.bufferedHeat + " HU"));
    }
}
