package com.hbm.inventory.gui.bomb;

import com.hbm.inventory.container.bomb.NukeManMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class NukeManScreen extends GuiInfoContainer<NukeManMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/weapon/fatmanschematic.png");

    public NukeManScreen(NukeManMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 194;
        this.imageHeight = 152;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }
}
