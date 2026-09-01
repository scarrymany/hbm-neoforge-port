package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.ReactorZirnoxBlockEntity;
import com.hbm.inventory.container.machine.dummyable.ReactorZirnoxMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Arrays;

/**
 * CE {@code GUIReactorZirnox} — {@code gui_zirnox.png} 203×256.
 * TODO(CE: GUIReactorZirnox.java:99-104): GUIElements.drawSmoothLinearGauge / drawSmoothGauge.
 */
public class ReactorZirnoxScreen extends GuiInfoContainer<ReactorZirnoxMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/reactors/gui_zirnox.png");

    public ReactorZirnoxScreen(ReactorZirnoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 203;
        this.imageHeight = 256;
        this.inventoryLabelY = this.imageHeight - 96;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        ReactorZirnoxBlockEntity be = this.getMenu().be;
        fillGauge(guiGraphics, x + 162, y + 114, be.getGaugeScaled(14, 0), 14, 5);
        fillGauge(guiGraphics, x + 144, y + 114, be.getGaugeScaled(14, 1), 14, 5);
        fillGauge(guiGraphics, x + 180, y + 114, be.getGaugeScaled(14, 2), 14, 5);
        fillGauge(guiGraphics, x + 164, y + 37, be.getGaugeScaled(10, 3), 10, 10);
        fillGauge(guiGraphics, x + 182, y + 37, be.getGaugeScaled(10, 4), 10, 10);
        if (be.isOn) {
            for (int ix = 0; ix < 4; ix++) {
                for (int iy = 0; iy < 4; iy++) {
                    guiGraphics.blit(TEXTURE, x + 7 + 36 * ix, y + 15 + 36 * iy, 238, 238, 18, 18);
                }
            }
            for (int ix = 0; ix < 3; ix++) {
                for (int iy = 0; iy < 3; iy++) {
                    guiGraphics.blit(TEXTURE, x + 25 + 36 * ix, y + 33 + 36 * iy, 238, 238, 18, 18);
                }
            }
            guiGraphics.blit(TEXTURE, x + 142, y + 15, 220, 238, 18, 18);
        }
        drawInfoPanel(guiGraphics, x - 16, y + 36, 2);
        drawInfoPanel(guiGraphics, x - 16, y + 52, 3);
        if (be.water.getFill() <= 0) drawInfoPanel(guiGraphics, x - 16, y + 68, 6);
        if (be.carbonDioxide.getFill() <= 4000) drawInfoPanel(guiGraphics, x - 16, y + 84, 6);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        ReactorZirnoxBlockEntity be = this.getMenu().be;
        be.steam.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 160, topPos + 108, 18, 12);
        be.carbonDioxide.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 142, topPos + 108, 18, 12);
        be.water.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 178, topPos + 108, 18, 12);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 160, topPos + 33, 18, 17,
                Component.literal("Temperature:"),
                Component.literal("   " + Math.round(be.heat * 0.00001 * 780 + 20) + "°C"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 178, topPos + 33, 18, 17,
                Component.literal("Pressure:"),
                Component.literal("   " + Math.round(be.pressure * 0.00001 * 30) + " bar"));
        tooltip(guiGraphics, mouseX, mouseY, -16, 36, "desc.gui.zirnox.coolant");
        tooltip(guiGraphics, mouseX, mouseY, -16, 52, "desc.gui.zirnox.pressure");
        if (be.water.getFill() <= 0) tooltip(guiGraphics, mouseX, mouseY, -16, 68, "desc.gui.zirnox.warning1");
        if (be.carbonDioxide.getFill() < 4000) tooltip(guiGraphics, mouseX, mouseY, -16, 84, "desc.gui.zirnox.warning2");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 144, 35, 14, 14)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, ReactorZirnoxMenu.BUTTON_CONTROL);
            return true;
        }
        if (isHovered(mouseX, mouseY, 151, 51, 36, 36)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, ReactorZirnoxMenu.BUTTON_VENT);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void tooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, int left, int top, String key) {
        Component[] lines = Arrays.stream(I18nUtil.resolveKeyArray(key)).map(Component::literal).toArray(Component[]::new);
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + left, topPos + top, 16, 16,
                leftPos + left + 8, topPos + top + 16, lines);
    }

    private static void fillGauge(GuiGraphics guiGraphics, int x, int y, int filled, int max, int width) {
        if (filled <= 0) return;
        guiGraphics.fill(x, y - filled, x + width, y, 0xFF7F0000);
    }
}
