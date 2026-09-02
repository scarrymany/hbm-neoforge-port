package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.DeuteriumExtractorBlockEntity;
import com.hbm.inventory.container.machine.dummyable.DeuteriumMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DeuteriumScreen extends GuiInfoContainer<DeuteriumMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_centrifuge.png");

public DeuteriumScreen(DeuteriumMenu menu, Inventory inventory, Component title) {
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
        DeuteriumExtractorBlockEntity be = this.getMenu().be;
        be.water.renderTank(x + 62, y + 70, 0, 16, 52);
        be.heavyWater.renderTank(x + 98, y + 70, 0, 16, 52);
        long max = Math.max(1L, be.getMaxPower());
        int ph = (int) (be.power * 52L / max);
        guiGraphics.fill(x + 26, y + 70 - ph, x + 42, y + 70, 0xFF44CCFF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        DeuteriumExtractorBlockEntity be = this.getMenu().be;
        be.water.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 62, topPos + 18, 16, 52);
        be.heavyWater.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 98, topPos + 18, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 26, topPos + 18, 16, 52,
                Component.literal(be.power + " / " + be.getMaxPower() + " HE"));
    }
}
