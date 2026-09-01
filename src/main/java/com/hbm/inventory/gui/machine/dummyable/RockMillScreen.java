package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineRockMillBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RockMillMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineRockMill} — power + water + progress. */
public class RockMillScreen extends GuiInfoContainer<RockMillMenu> {

    public RockMillScreen(RockMillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 220;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MachineRockMillBlockEntity be = this.getMenu().be;
        int p = be.getProgressScaled(16);
        guiGraphics.fill(x + 62, y + 36, x + 62 + p, y + 52, be.didProcess ? 0xFF00A0FF : 0xFF446688);
        be.inputTank.renderTank(x + 8, y + 88, 0, 16, 52);
        be.outputTank.renderTank(x + 152, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineRockMillBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 152, 72, 16, 16, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 62, topPos + 36, 16, 16,
                Component.literal(be.didProcess ? "Milling" : "Idle"),
                Component.literal("Progress: " + be.getProgressScaled(100) + "%"));
        be.inputTank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 36, 16, 52);
        be.outputTank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 18, 16, 52);
    }
}
