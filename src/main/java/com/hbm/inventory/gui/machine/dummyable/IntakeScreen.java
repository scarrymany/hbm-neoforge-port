package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineIntakeBlockEntity;
import com.hbm.inventory.container.machine.dummyable.IntakeMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class IntakeScreen extends GuiInfoContainer<IntakeMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_fluid.png");

public IntakeScreen(IntakeMenu menu, Inventory inventory, Component title) {
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
        MachineIntakeBlockEntity be = this.getMenu().be;
        be.compair.renderTank(x + 80, y + 70, 0, 16, 52);
        long max = Math.max(1L, be.getMaxPower());
        int ph = (int) (be.power * 52L / max);
        guiGraphics.fill(x + 44, y + 70 - ph, x + 60, y + 70, 0xFF44CCFF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineIntakeBlockEntity be = this.getMenu().be;
        be.compair.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 18, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 18, 16, 52,
                Component.literal(be.power + " / " + be.getMaxPower() + " HE"));
    }
}
