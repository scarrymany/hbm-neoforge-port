package com.hbm.inventory.gui.machine.reprocess;

import com.hbm.inventory.container.machine.reprocess.SolidifierMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUISolidifier}: power + progress + input tank. */
public class SolidifierScreen extends GuiInfoContainer<SolidifierMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_solidifier.png");

    public SolidifierScreen(SolidifierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        // CE GUISolidifier.java:52-63
        int i = (int) (be.getPower() * 52 / Math.max(1L, be.getMaxPower()));
        if (i > 0) {
            guiGraphics.blit(TEXTURE, x + 134, y + 70 - i, 176, 52 - i, 16, i);
            guiGraphics.blit(TEXTURE, x + 138, y + 4, 176, 52, 9, 12);
        }
        int j = be.getProgressScaled(42);
        if (j > 0) {
            guiGraphics.blit(TEXTURE, x + 42, y + 17, 192, 0, j, 35);
        }
        be.tank.renderTank(x + 35, y + 88, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :40 — title at x=70, color 0xC7C1A3
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, 70 - this.font.width(name) / 2, 6, 0xC7C1A3, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 134, topPos + 18, 16, 52, be.getPower(), be.getMaxPower());
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 36, 16, 52);
    }
}
