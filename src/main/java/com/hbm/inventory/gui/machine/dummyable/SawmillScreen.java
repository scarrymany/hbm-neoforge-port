package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineSawmillBlockEntity;
import com.hbm.inventory.container.machine.dummyable.SawmillMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Live heat + progress. CE was overlay-only. */
public class SawmillScreen extends GuiInfoContainer<SawmillMenu> {

    public SawmillScreen(SawmillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MachineSawmillBlockEntity be = this.getMenu().be;
        int hh = Math.min(52, be.heat * 52 / 300);
        guiGraphics.fill(x + 143, y + 70 - hh, x + 159, y + 70, 0xFFFF4400);
        int ph = be.progress * 24 / MachineSawmillBlockEntity.PROCESS_TIME;
        guiGraphics.fill(x + 66, y + 36, x + 66 + ph, y + 50, 0xFFFFFF55);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineSawmillBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 143, topPos + 18, 16, 52,
                Component.literal(be.heat + " TU/t"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 66, topPos + 36, 24, 14,
                Component.literal("Progress: " + be.progress + " / " + MachineSawmillBlockEntity.PROCESS_TIME));
    }
}
