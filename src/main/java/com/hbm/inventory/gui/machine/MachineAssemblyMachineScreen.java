package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineAssemblyMachineMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Ported (visually, from CE's {@code GUIMachineAssemblyMachine}) as a plain panel - see {@link MachineRTGScreen}'s javadoc for the no-texture-yet rationale. */
public class MachineAssemblyMachineScreen extends GuiInfoContainer<MachineAssemblyMachineMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_assembler.png");

    public MachineAssemblyMachineScreen(MachineAssemblyMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 202;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        int progress = this.getMenu().be.getProgressScaled(24);
        if (progress > 0) guiGraphics.blit(TEXTURE, x + 66, y + 45, 176, 0, progress, 6);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 152, 63, 18, 18, this.getMenu().be.getPower(), this.getMenu().be.getMaxPower());
    }
}
