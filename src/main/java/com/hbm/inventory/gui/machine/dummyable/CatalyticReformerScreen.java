package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineCatalyticReformerBlockEntity;
import com.hbm.inventory.container.machine.dummyable.CatalyticReformerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineCatalyticReformer} 176×238 — 4 tanks + power + catalyst. */
public class CatalyticReformerScreen extends GuiInfoContainer<CatalyticReformerMenu> {

    public CatalyticReformerScreen(CatalyticReformerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 238;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MachineCatalyticReformerBlockEntity be = this.getMenu().be;
        int ph = (int) (be.getPower() * 52 / Math.max(1, be.getMaxPower()));
        guiGraphics.fill(x + 17, y + 70 - ph, x + 33, y + 70, 0xFFFFCC00);
        be.input.renderTank(x + 35, y + 70, 0, 16, 52);
        be.out1.renderTank(x + 107, y + 70, 0, 16, 52);
        be.out2.renderTank(x + 125, y + 70, 0, 16, 52);
        be.out3.renderTank(x + 143, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineCatalyticReformerBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 17, 18, 16, 52, be.getPower(), be.getMaxPower());
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 18, 16, 52);
        be.out1.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 107, topPos + 18, 16, 52);
        be.out2.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 125, topPos + 18, 16, 52);
        be.out3.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 143, topPos + 18, 16, 52);
    }
}
