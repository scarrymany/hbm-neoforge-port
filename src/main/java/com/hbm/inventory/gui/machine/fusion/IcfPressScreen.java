package com.hbm.inventory.gui.machine.fusion;

import com.hbm.inventory.container.machine.fusion.IcfPressMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class IcfPressScreen extends GuiInfoContainer<IcfPressMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/reactors/gui_icf.png");

    public IcfPressScreen(IcfPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 156;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        be.tanks[0].renderTank(x + 62, y + 18, 0, 16, 34);
        be.tanks[1].renderTank(x + 134, y + 18, 0, 16, 34);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 70, 90, 12, Component.literal("Muon: " + be.muon));
    }
}
