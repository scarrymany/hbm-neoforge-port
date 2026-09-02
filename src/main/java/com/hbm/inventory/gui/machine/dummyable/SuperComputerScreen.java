package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineSuperComputerBlockEntity;
import com.hbm.inventory.container.machine.dummyable.SuperComputerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineSuperComputer} 176×211. */
public class SuperComputerScreen extends GuiInfoContainer<SuperComputerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_supercomputer.png");

    public SuperComputerScreen(SuperComputerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 211;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineSuperComputerBlockEntity be = this.getMenu().be;
        be.input.renderTank(x + 8, y + 106, 0, 16, 52);
        be.output.renderTank(x + 80, y + 106, 0, 16, 52);
        int ph = (int) (be.power * 61L / MachineSuperComputerBlockEntity.MAX_POWER);
        guiGraphics.fill(x + 152, y + 79 - ph, x + 168, y + 79, 0xFF44CCFF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineSuperComputerBlockEntity be = this.getMenu().be;
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 54, 16, 52);
        be.output.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 54, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 18, 16, 61,
                Component.literal(be.power + " / " + MachineSuperComputerBlockEntity.MAX_POWER + " HE"));
    }
}
