package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachinePyroOvenBlockEntity;
import com.hbm.inventory.container.machine.dummyable.PyroOvenMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code GUIPyroOven} on existing {@code gui_pyrooven.png} 176×204.
 * Power 152,{@code 70-i} from 176,{@code 64-i}; progress 57,47 from 176,0; tanks 8/116,70; info 108,76.
 * Invented progress {@code fill()} removed.
 */
public class PyroOvenScreen extends GuiInfoContainer<PyroOvenMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_pyrooven.png");

    public PyroOvenScreen(PyroOvenMenu menu, Inventory inventory, Component title) {
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

        MachinePyroOvenBlockEntity be = this.getMenu().be;
        // CE GUIPyroOven.java:51-60
        int i = (int) (be.getPower() * 52 / Math.max(1L, be.getMaxPower()));
        if (i > 0) {
            guiGraphics.blit(TEXTURE, x + 152, y + 70 - i, 176, 64 - i, 16, i);
        }
        int p = (int) (be.progress * 27);
        if (p > 0) {
            guiGraphics.blit(TEXTURE, x + 57, y + 47, 176, 0, p, 12);
        }
        be.input.renderTank(x + 8, y + 70, 0, 16, 52);
        be.output.renderTank(x + 116, y + 70, 0, 16, 52);
        drawInfoPanel(guiGraphics, x + 108, y + 76, 8);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :40 — title shifted -18
        int nameX = this.imageWidth / 2 - this.font.width(this.title) / 2 - 18;
        guiGraphics.drawString(this.font, this.title, nameX, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachinePyroOvenBlockEntity be = this.getMenu().be;
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 18, 16, 52);
        be.output.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 18, 16, 52);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 18, 16, 52, be.getPower(), be.getMaxPower());
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 108, topPos + 76, 8, 8, leftPos + 108, topPos + 76,
                Component.literal(I18nUtil.resolveKey("upgrade.gui.title")),
                Component.literal(I18nUtil.resolveKey("upgrade.gui.speed", 3)),
                Component.literal(I18nUtil.resolveKey("upgrade.gui.power", 3)),
                Component.literal(I18nUtil.resolveKey("upgrade.gui.overdrive", 3)));
    }
}
