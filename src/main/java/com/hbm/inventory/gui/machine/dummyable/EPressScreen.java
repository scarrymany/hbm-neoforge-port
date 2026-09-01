package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineEPressBlockEntity;
import com.hbm.inventory.container.machine.dummyable.EPressMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineEPress} — power + stamp progress. */
public class EPressScreen extends GuiInfoContainer<EPressMenu> {

    public EPressScreen(EPressMenu menu, Inventory inventory, Component title) {
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

        MachineEPressBlockEntity be = this.getMenu().be;
        int ph = (int) (be.getPower() * 52 / Math.max(1, be.getMaxPower()));
        guiGraphics.fill(x + 152, y + 52 - ph, x + 168, y + 52, 0xFFFFCC00);
        int p = be.progress * 16 / MachineEPressBlockEntity.MAX_PROGRESS;
        guiGraphics.fill(x + 47, y + 35, x + 47 + p, y + 51, 0xFF555555);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineEPressBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 152, 0, 16, 52, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 47, topPos + 35, 16, 16,
                Component.literal("Progress: " + (be.progress * 100 / MachineEPressBlockEntity.MAX_PROGRESS) + "%"));
    }
}
