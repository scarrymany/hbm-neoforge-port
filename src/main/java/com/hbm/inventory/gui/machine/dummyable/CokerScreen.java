package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineCokerBlockEntity;
import com.hbm.inventory.container.machine.dummyable.CokerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineCoker} 176×204 — tanks, progress, heat. */
public class CokerScreen extends GuiInfoContainer<CokerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_coker.png");

    public CokerScreen(CokerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineCokerBlockEntity be = this.getMenu().be;
        int p = be.getProgressScaled(53);
        int h = be.getHeatScaled(52);
        if (p > 0) guiGraphics.fill(x + 61, y + 46, x + 61 + p, y + 54, be.wasOn ? 0xFFFF8800 : 0xFF664400);
        if (h > 0) guiGraphics.fill(x + 61, y + 55, x + 61 + h, y + 63, 0xFFFF2200);
        be.input.renderTank(x + 35, y + 70, 0, 16, 52);
        be.output.renderTank(x + 125, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineCokerBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 61, topPos + 46, 53, 8,
                Component.literal(be.wasOn ? "Coking" : "Idle"),
                Component.literal(be.getProgressScaled(100) + "%"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 61, topPos + 55, 52, 8,
                Component.literal("Heat"),
                Component.literal(be.heat + " / " + MachineCokerBlockEntity.MAX_HEAT + " TU"));
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 18, 16, 52);
        be.output.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 125, topPos + 18, 16, 52);
    }
}
