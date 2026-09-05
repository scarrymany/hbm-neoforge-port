package com.hbm.inventory.gui.machine.reprocess;

import com.hbm.inventory.container.machine.reprocess.LiquefactorMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Exact CE {@code GUILiquefactor}: power + progress + output tank. */
public class LiquefactorScreen extends GuiInfoContainer<LiquefactorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_liquefactor.png");

    public LiquefactorScreen(LiquefactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        int p = (int) (be.getPower() * 52 / Math.max(be.getMaxPower(), 1));
        if (p > 0) {
            guiGraphics.blit(TEXTURE, x + 134, y + 70 - p, 176, 52 - p, 16, p);
            guiGraphics.blit(TEXTURE, x + 138, y + 4, 176, 52, 9, 12);
        }
        int prog = be.getProgressScaled(42);
        if (prog > 0) {
            guiGraphics.blit(TEXTURE, x + 42, y + 17, 192, 0, prog, 35);
        }
        // Exact CE GUILiquefactor.java:57 — tank at 71,88 (bottom origin, 16×52).
        be.tank.renderTank(x + 71, y + 88, 0, 16, 52);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 134, topPos + 18, 16, 52, be.getPower(), be.getMaxPower());
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 71, topPos + 36, 16, 52);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
