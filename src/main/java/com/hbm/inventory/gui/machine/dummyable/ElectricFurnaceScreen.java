package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineElectricFurnaceBlockEntity;
import com.hbm.inventory.container.machine.dummyable.ElectricFurnaceMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ElectricFurnaceScreen extends GuiInfoContainer<ElectricFurnaceMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_electric_furnace.png");

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineElectricFurnaceBlockEntity be = this.getMenu().be;
        long max = Math.max(1L, be.getMaxPower());
        int ph = (int) (be.power * 52L / max);
        if (ph > 0) guiGraphics.blit(TEXTURE, x + 26, y + 70 - ph, 176, 52 - ph, 16, ph);
        int prog = be.progress * 24 / Math.max(1, MachineElectricFurnaceBlockEntity.MAX_PROGRESS);
        if (prog > 0) guiGraphics.blit(TEXTURE, x + 79, y + 35, 192, 0, prog, 14);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineElectricFurnaceBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 26, topPos + 18, 16, 52,
                Component.literal(be.power + " / " + be.getMaxPower() + " HE"),
                Component.literal(MachineElectricFurnaceBlockEntity.CONSUMPTION + " HE/t"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 79, topPos + 35, 24, 14,
                Component.literal(be.progress + " / " + MachineElectricFurnaceBlockEntity.MAX_PROGRESS));
    }
}
