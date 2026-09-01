package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FurnaceCombinationBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FurnaceCombinationMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIFurnaceCombo} slot/bar layout, painted panel (CE texture not in this port). */
public class FurnaceCombinationScreen extends GuiInfoContainer<FurnaceCombinationMenu> {

    public FurnaceCombinationScreen(FurnaceCombinationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        FurnaceCombinationBlockEntity be = this.getMenu().be;
        int p = be.progress * 38 / FurnaceCombinationBlockEntity.PROCESS_TIME;
        guiGraphics.fill(x + 45, y + 37, x + 45 + p, y + 42, 0xFFFFAA00);
        int h = be.heat * 37 / FurnaceCombinationBlockEntity.MAX_HEAT;
        guiGraphics.fill(x + 45, y + 46, x + 45 + h, y + 51, 0xFFFF4400);
        be.tank.renderTank(x + 118, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        FurnaceCombinationBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 36, 39, 7,
                Component.literal(String.format("%,d / %,d TU", be.progress, FurnaceCombinationBlockEntity.PROCESS_TIME)));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 45, 39, 7,
                Component.literal(String.format("%,d / %,d TU", be.heat, FurnaceCombinationBlockEntity.MAX_HEAT)));
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 118, topPos + 18, 16, 52);
    }
}
