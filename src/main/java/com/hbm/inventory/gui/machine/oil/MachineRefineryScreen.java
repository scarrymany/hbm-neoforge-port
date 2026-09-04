package com.hbm.inventory.gui.machine.oil;

import com.hbm.blockentity.machine.oil.MachineRefineryBlockEntity;
import com.hbm.inventory.container.machine.oil.MachineRefineryMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Painted panel, CE {@code GUIMachineRefinery} size kept 216×230 (port already-painted; CE 210×231).
 * No invented {@code gui_refinery.png}. Tank blit Exact CE {@code GUIMachineRefinery.java:89}/
 * {@code :135-138} so canister columns at x=8 / y=99 stay clear.
 */
public class MachineRefineryScreen extends GuiInfoContainer<MachineRefineryMenu> {

    public MachineRefineryScreen(MachineRefineryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 216;
        this.imageHeight = 230;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        var be = this.getMenu().be;
        // CE :89 — input column (33,130) 16×101, not x=8 (canisters sit there).
        be.tanks.get(0).renderTank(x + 33, y + 130, 0, 16, 101);
        // CE :135-138
        be.tanks.get(1).renderTank(x + 86, y + 95, 0, 16, 52);
        be.tanks.get(2).renderTank(x + 106, y + 95, 0, 16, 52);
        be.tanks.get(3).renderTank(x + 126, y + 95, 0, 16, 52);
        be.tanks.get(4).renderTank(x + 146, y + 95, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 6, 160, 12, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 20, 160, 10,
                Component.literal(be.isOn ? "Refining" : "Idle"),
                Component.literal("Sulfur cycle: " + be.sulfur + "/" + MachineRefineryBlockEntity.MAX_SULFUR));

        // CE :38-42
        be.tanks.get(0).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 30, topPos + 27, 21, 104);
        int[] xs = {86, 106, 126, 146};
        for (int i = 0; i < 4; i++) {
            be.tanks.get(i + 1).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + xs[i], topPos + 42, 16, 52);
        }
    }
}
