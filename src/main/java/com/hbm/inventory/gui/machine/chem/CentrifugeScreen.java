package com.hbm.inventory.gui.machine.chem;

import com.hbm.inventory.container.machine.chem.CentrifugeMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * CE {@code GUIMachineCentrifuge}. Texture is the unmodified CE png
 * ({@code textures/gui/processing/gui_centrifuge.png}, 182×189).
 */
public class CentrifugeScreen extends GuiInfoContainer<CentrifugeMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/processing/gui_centrifuge.png");

    public CentrifugeScreen(CentrifugeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 182;
        this.imageHeight = 189;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = this.imageWidth / 2 - 16;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        if (be.hasPower()) {
            int i1 = (int) be.getPowerRemainingScaled(37);
            if (i1 > 0) {
                guiGraphics.blit(TEXTURE, x + 8, y + 55 - i1, 182, 37 - i1, 16, i1);
            }
        }

        if (be.isProcessing()) {
            int p = be.getCentrifugeProgressScaled(145);
            for (int i = 0; i < 4; i++) {
                int h = Math.min(p, 36);
                if (h > 0) {
                    guiGraphics.blit(TEXTURE, x + 72 + i * 20, y + 57 - h, 182, 73 - h, 12, h);
                }
                p -= h;
                if (p <= 0) break;
            }
        }

        drawInfoPanel(guiGraphics, x + 160, y + 16, 8);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 18, 16, 37, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 160, topPos + 16, 8, 8,
                Component.literal(I18nUtil.resolveKey("desc.gui.upgrade")),
                Component.literal(I18nUtil.resolveKey("desc.gui.upgrade.speed")),
                Component.literal(I18nUtil.resolveKey("desc.gui.upgrade.power")),
                Component.literal(I18nUtil.resolveKey("desc.gui.upgrade.overdrive")));
    }
}
