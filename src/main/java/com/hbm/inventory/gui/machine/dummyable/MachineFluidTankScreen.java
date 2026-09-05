package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineFluidTankBlockEntity;
import com.hbm.inventory.container.machine.dummyable.MachineFluidTankMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineFluidTank} — {@code gui_tank.png} 176×166. Mode button 151,35. */
public class MachineFluidTankScreen extends GuiInfoContainer<MachineFluidTankMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/storage/gui_tank.png");

    public MachineFluidTankScreen(MachineFluidTankMenu menu, Inventory inventory, Component title) {
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
        MachineFluidTankBlockEntity be = this.getMenu().be;
        guiGraphics.blit(TEXTURE, x + 151, y + 34, 176, be.mode * 18, 18, 18);
        be.tank.renderTank(x + 71, y + 69, 0, 34, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        this.getMenu().be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 71, topPos + 17, 34, 52);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 151, 35, 18, 18)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, MachineFluidTankMenu.BUTTON_CYCLE);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
