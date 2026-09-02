package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.CondenserBlockEntity;
import com.hbm.blockentity.machine.dummyable.CondenserPoweredBlockEntity;
import com.hbm.inventory.container.machine.dummyable.CondenserMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Tank inspect for condenser / cooling towers. */
public class CondenserScreen extends GuiInfoContainer<CondenserMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_fluid.png");

public CondenserScreen(CondenserMenu menu, Inventory inventory, Component title) {
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
        CondenserBlockEntity be = this.getMenu().be;
        be.input.renderTank(x + 62, y + 70, 0, 16, 52);
        be.output.renderTank(x + 98, y + 70, 0, 16, 52);
        if (be instanceof CondenserPoweredBlockEntity powered) {
            long max = Math.max(1L, powered.getMaxPower());
            int ph = (int) (powered.power * 52L / max);
            guiGraphics.fill(x + 26, y + 70 - ph, x + 42, y + 70, 0xFF44CCFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        CondenserBlockEntity be = this.getMenu().be;
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 62, topPos + 18, 16, 52);
        be.output.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 98, topPos + 18, 16, 52);
        if (be instanceof CondenserPoweredBlockEntity powered) {
            drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 26, topPos + 18, 16, 52,
                    Component.literal(powered.power + " / " + powered.getMaxPower() + " HE"));
        }
    }
}
