package com.hbm.inventory.gui.machine.workshop;

import com.hbm.inventory.container.machine.workshop.ArcWelderMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code GUIMachineArcWelder} on existing {@code gui_arc_welder.png} 176×204.
 * Title {@code xSize/2 - width/2 - 18}. Info panel 78,67. IUpgradeInfoProvider stay skipped —
 * tooltip uses CE {@code getUpgradeInfo} SPEED/POWER/OVERDRIVE 3.
 */
public class ArcWelderScreen extends GuiInfoContainer<ArcWelderMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_arc_welder.png");

    public ArcWelderScreen(ArcWelderMenu menu, Inventory inventory, Component title) {
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
        int p = (int) (be.getPower() * 52 / Math.max(be.getMaxPower(), 1));
        if (p > 0) {
            guiGraphics.blit(TEXTURE, x + 152, y + 70 - p, 176, 52 - p, 16, p);
        }
        int prog = be.getProgressScaled(33);
        if (prog > 0) {
            guiGraphics.blit(TEXTURE, x + 72, y + 37, 192, 0, prog, 14);
        }
        if (be.getPower() >= be.consumption) {
            guiGraphics.blit(TEXTURE, x + 156, y + 4, 176, 52, 9, 12);
        }
        // Exact CE GUIMachineArcWelder.java:60
        drawInfoPanel(guiGraphics, x + 78, y + 67, 8);
        // Exact CE GUIMachineArcWelder.java:61 — horizontal tank at 35,79.
        be.tank.renderTank(x + 35, y + 79, 0, 34, 16, 1);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :38-40 — title shifted -18
        int nameX = this.imageWidth / 2 - this.font.width(this.title) / 2 - 18;
        guiGraphics.drawString(this.font, this.title, nameX, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 18, 16, 52, be.getPower(), be.getMaxPower());
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 63, 34, 16);
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 78, topPos + 67, 8, 8, leftPos + 78, topPos + 67,
                Component.literal(I18nUtil.resolveKey("upgrade.gui.title")),
                Component.literal(I18nUtil.resolveKey("upgrade.gui.speed", 3)),
                Component.literal(I18nUtil.resolveKey("upgrade.gui.power", 3)),
                Component.literal(I18nUtil.resolveKey("upgrade.gui.overdrive", 3)));
    }
}
