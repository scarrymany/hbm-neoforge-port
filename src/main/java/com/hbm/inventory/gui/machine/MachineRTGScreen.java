package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineRTGMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported (visually, from CE's {@code GUIMachineRTG}) as a plain panel: no background texture exists
 * in this port yet (see {@link GuiInfoContainer}'s own asset-gap note - texture porting is a
 * separate, later pass), so this draws a flat panel via {@link GuiGraphics#fill} instead of blitting
 * CE's {@code gui_rtg.png}. Passive machine - no buttons, just the pellet grid (from the Menu) and a
 * power/heat tooltip over the panel.
 */
public class MachineRTGScreen extends GuiInfoContainer<MachineRTGMenu> {

    public MachineRTGScreen(MachineRTGMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 187;
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
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 6, 160, 12, this.getMenu().be.getPower(), this.getMenu().be.getMaxPower());
    }
}
