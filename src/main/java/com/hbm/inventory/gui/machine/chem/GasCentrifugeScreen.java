package com.hbm.inventory.gui.machine.chem;

import com.hbm.inventory.container.machine.chem.GasCentrifugeMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Ported (visually, from CE's {@code GUIMachineGasCent}) as a plain panel. */
public class GasCentrifugeScreen extends GuiInfoContainer<GasCentrifugeMenu> {

    public GasCentrifugeScreen(GasCentrifugeMenu menu, Inventory inventory, Component title) {
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
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 6, 160, 12, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 36, 80, 20,
                Component.literal(be.isProgressing ? "Enriching" : "Idle"),
                Component.literal("Stage: " + be.inputTank.getTankType().name),
                Component.literal("Feed: " + be.tank.getFill() + "/" + be.tank.getMaxFill() + "mB"));
    }
}
