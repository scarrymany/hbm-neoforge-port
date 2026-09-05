package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.RadarScreenBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RadarScreenMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Linked-radar inspect. Scan overlay skipped. */
public class RadarScreenScreen extends GuiInfoContainer<RadarScreenMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_radar.png");

public RadarScreenScreen(RadarScreenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        RadarScreenBlockEntity be = this.getMenu().be;
        if (be.linked) {
            guiGraphics.drawString(this.font, be.refX + " " + be.refY + " " + be.refZ, 8, 20, 0x80FF80, false);
            guiGraphics.drawString(this.font, "Range: " + be.range, 8, 32, 0x80FF80, false);
            guiGraphics.drawString(this.font, "Contacts: " + be.entries.size(), 8, 44, 0x80FF80, false);
        } else {
            guiGraphics.drawString(this.font, "unlinked", 8, 20, 0xFF8080, false);
        }
    }
}
