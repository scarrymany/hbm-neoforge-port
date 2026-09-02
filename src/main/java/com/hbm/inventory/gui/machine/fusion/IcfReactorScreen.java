package com.hbm.inventory.gui.machine.fusion;

import com.hbm.blockentity.machine.fusion.IcfReactorBlockEntity;
import com.hbm.inventory.container.machine.fusion.IcfReactorMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class IcfReactorScreen extends GuiInfoContainer<IcfReactorMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/reactors/gui_icf.png");

    public IcfReactorScreen(IcfReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        var be = this.getMenu().be;
        be.tanks[0].renderTank(x + 8, y + 108, 0, 16, 54);
        be.tanks[1].renderTank(x + 28, y + 108, 0, 16, 54);
        be.tanks[2].renderTank(x + 48, y + 108, 0, 16, 54);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, 70, 40, 90, 12,
                Component.literal("Heat: " + Library.getShortNumber(be.heat) + " / " + Library.getShortNumber(IcfReactorBlockEntity.MAX_HEAT)));
        drawCustomInfo(guiGraphics, mouseX, mouseY, 70, 54, 90, 12,
                Component.literal("Laser: " + Library.getShortNumber(be.laser)));
    }
}
