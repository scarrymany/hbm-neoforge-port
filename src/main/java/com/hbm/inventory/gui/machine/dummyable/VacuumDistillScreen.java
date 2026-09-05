package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineVacuumDistillBlockEntity;
import com.hbm.inventory.container.machine.dummyable.VacuumDistillMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code GUIMachineVacuumDistill} on existing {@code gui_vacuum_distill.png} 176×238.
 * Power 26,{@code 70-j} from 176,{@code 52-j}; tanks 44/80/98/116/134,70.
 * Invented power {@code fill()} + Refining tooltip removed. Title {@code 0xffffff} y=5.
 */
public class VacuumDistillScreen extends GuiInfoContainer<VacuumDistillMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_vacuum_distill.png");

    public VacuumDistillScreen(VacuumDistillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 238;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineVacuumDistillBlockEntity be = this.getMenu().be;
        // CE GUIMachineVacuumDistill.java:52-58
        if (be.getMaxPower() > 0) {
            int j = (int) (be.getPower() * 54 / be.getMaxPower());
            if (j > 0) {
                guiGraphics.blit(TEXTURE, x + 26, y + 70 - j, 176, 52 - j, 16, j);
            }
        }
        be.input.renderTank(x + 44, y + 70, 0, 16, 52);
        be.heavy.renderTank(x + 80, y + 70, 0, 16, 52);
        be.reformate.renderTank(x + 98, y + 70, 0, 16, 52);
        be.light.renderTank(x + 116, y + 70, 0, 16, 52);
        be.gas.renderTank(x + 134, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :41 — white title y=5
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(name) / 2, 5, 0xffffff, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineVacuumDistillBlockEntity be = this.getMenu().be;
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 70 - 52, 16, 52);
        be.heavy.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 70 - 52, 16, 52);
        be.reformate.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 98, topPos + 70 - 52, 16, 52);
        be.light.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 70 - 52, 16, 52);
        be.gas.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 134, topPos + 70 - 52, 16, 52);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 26, topPos + 70 - 52, 16, 52, be.getPower(), be.getMaxPower());
    }
}
