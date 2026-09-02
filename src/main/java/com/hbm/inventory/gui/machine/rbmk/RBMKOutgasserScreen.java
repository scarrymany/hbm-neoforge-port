package com.hbm.inventory.gui.machine.rbmk;

import com.hbm.inventory.container.machine.rbmk.RBMKOutgasserMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RBMKOutgasserScreen extends GuiInfoContainer<RBMKOutgasserMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/reactors/gui_rbmk_outgasser.png");

    public RBMKOutgasserScreen(RBMKOutgasserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        var be = getMenu().be;
        guiGraphics.drawString(this.font,
                String.format("Flux %.1f / %d", be.progress, be.duration),
                leftPos + 8, topPos + 20, 0x404040, false);
        guiGraphics.drawString(this.font,
                "Gas: " + be.gas.getFill() + "/" + be.gas.getMaxFill() + "mB " + be.gas.getTankType().getName(),
                leftPos + 8, topPos + 32, 0x404040, false);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
