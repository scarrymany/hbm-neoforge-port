package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineCrystallizerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * CE {@code GUICrystallizer}. Texture is the unmodified CE png
 * ({@code textures/gui/processing/gui_crystallizer_alt.png}, 176×204).
 * Fluid-load / fluid-id slots stay trimmed — {@code TODO(CE: ContainerCrystallizer.java:38-42)}.
 */
public class MachineCrystallizerScreen extends GuiInfoContainer<MachineCrystallizerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/processing/gui_crystallizer_alt.png");

    public MachineCrystallizerScreen(MachineCrystallizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = this.imageWidth / 2 - 16;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        int i = (int) (be.getMaxPower() > 0 ? be.getPower() * 52L / be.getMaxPower() : 0);
        if (i > 0) {
            guiGraphics.blit(TEXTURE, x + 152, y + 70 - i, 176, 64 - i, 16, i);
        }
        int j = be.getProgressScaled(28);
        if (j > 0) {
            guiGraphics.blit(TEXTURE, x + 80, y + 47, 176, 0, j, 12);
        }
        drawInfoPanel(guiGraphics, x + 117, y + 22, 8);
        be.tank.renderTank(x + 35, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 17, 16, 52, be.getPower(), be.getMaxPower());
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 18, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 117, topPos + 22, 8, 8,
                Component.literal(I18nUtil.resolveKey("desc.gui.upgrade")),
                Component.literal(I18nUtil.resolveKey("desc.gui.upgrade.speed")),
                Component.literal(I18nUtil.resolveKey("desc.gui.upgrade.effectiveness")),
                Component.literal(I18nUtil.resolveKey("desc.gui.upgrade.overdrive")));
    }
}
