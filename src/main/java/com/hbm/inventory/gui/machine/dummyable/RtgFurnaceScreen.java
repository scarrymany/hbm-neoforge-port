package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineRtgFurnaceBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RtgFurnaceMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RtgFurnaceScreen extends GuiInfoContainer<RtgFurnaceMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_rtg_difurnace.png");

public RtgFurnaceScreen(RtgFurnaceMenu menu, Inventory inventory, Component title) {
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

        MachineRtgFurnaceBlockEntity be = this.getMenu().be;
        int ph = be.progress * 24 / MachineRtgFurnaceBlockEntity.MAX_PROGRESS;
        guiGraphics.fill(x + 79, y + 35, x + 79 + ph, y + 49, 0xFFFFFF55);
        if (be.heat > 0) guiGraphics.fill(x + 56, y + 36, x + 70, y + 38, 0xFFFF4400);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineRtgFurnaceBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 79, topPos + 35, 24, 14,
                Component.literal(be.progress + " / " + MachineRtgFurnaceBlockEntity.MAX_PROGRESS),
                Component.literal("heat " + be.heat));
    }
}
