package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineTurbofanBlockEntity;
import com.hbm.inventory.container.machine.dummyable.TurbofanMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineTurbofan} — kerosene tank + power + afterburn. */
public class TurbofanScreen extends GuiInfoContainer<TurbofanMenu> {

    public TurbofanScreen(TurbofanMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MachineTurbofanBlockEntity be = this.getMenu().be;
        int ph = (int) (be.getPower() * 52 / Math.max(1, be.getMaxPower()));
        guiGraphics.fill(x + 143, y + 69 - ph, x + 159, y + 69, 0xFFFFCC00);
        be.tank.renderTank(x + 62, y + 69, 0, 16, 52);
        if (be.wasOn) guiGraphics.fill(x + 80, y + 17, x + 96, y + 25, 0xFF44CC44);
        if (be.afterburner > 0) guiGraphics.fill(x + 80, y + 29, x + 80 + be.afterburner * 8, y + 37, 0xFFFF6622);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineTurbofanBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 143, 17, 16, 52, be.getPower(), be.getMaxPower());
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 62, topPos + 17, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 17, 16, 8,
                Component.literal(be.wasOn ? "SPINNING" : "IDLE"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 29, 24, 8,
                Component.literal("Afterburn ×" + be.afterburner));
    }
}
