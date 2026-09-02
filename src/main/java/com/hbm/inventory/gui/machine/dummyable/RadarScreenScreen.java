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
        String link = be.linkedValid
                ? be.linked.getX() + " " + be.linked.getY() + " " + be.linked.getZ()
                : "unlinked";
        guiGraphics.drawString(this.font, link, 8, 20, 0x80FF80, false);
    }
}
