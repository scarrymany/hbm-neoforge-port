package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.StorageDrumBlockEntity;
import com.hbm.inventory.container.machine.dummyable.StorageDrumMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIStorageDrum} 176×237 — liquid/gas gauges. */
public class StorageDrumScreen extends GuiInfoContainer<StorageDrumMenu> {

    public StorageDrumScreen(StorageDrumMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 237;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        StorageDrumBlockEntity be = this.getMenu().be;
        be.liquid.renderTank(x + 16, y + 131, 0, 9, 108);
        be.gas.renderTank(x + 151, y + 131, 0, 9, 108);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        StorageDrumBlockEntity be = this.getMenu().be;
        be.liquid.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 16, topPos + 23, 9, 108);
        be.gas.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 151, topPos + 23, 9, 108);
    }
}
