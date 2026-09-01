package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineRtgFurnaceBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RtgFurnaceMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RtgFurnaceScreen extends GuiInfoContainer<RtgFurnaceMenu> {

    public RtgFurnaceScreen(RtgFurnaceMenu menu, Inventory inventory, Component title) {
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

        MachineRtgFurnaceBlockEntity be = this.getMenu().be;
        int ph = be.progress * 24 / MachineRtgFurnaceBlockEntity.MAX_PROGRESS;
        guiGraphics.fill(x + 79, y + 35, x + 79 + ph, y + 49, 0xFFFFFF55);
        if (be.heat > 0) guiGraphics.fill(x + 56, y + 36, x + 70, y + 38, 0xFFFF4400);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineRtgFurnaceBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 79, topPos + 35, 24, 14,
                Component.literal(be.progress + " / " + MachineRtgFurnaceBlockEntity.MAX_PROGRESS),
                Component.literal("heat " + be.heat));
    }
}
