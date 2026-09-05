package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineEPressBlockEntity;
import com.hbm.inventory.container.machine.dummyable.EPressMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineEPress} — power + stamp progress. */
public class EPressScreen extends GuiInfoContainer<EPressMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_electric_press.png");

    public EPressScreen(EPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineEPressBlockEntity be = this.getMenu().be;
        int ph = (int) (be.getPower() * 52 / Math.max(1, be.getMaxPower()));
        if (ph > 0) guiGraphics.blit(TEXTURE, x + 152, y + 52 - ph, 176, 52 - ph, 16, ph);
        int p = be.progress * 16 / MachineEPressBlockEntity.MAX_PROGRESS;
        if (p > 0) guiGraphics.blit(TEXTURE, x + 47, y + 35, 192, 0, p, 16);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineEPressBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 152, 0, 16, 52, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 47, topPos + 35, 16, 16,
                Component.literal("Progress: " + (be.progress * 100 / MachineEPressBlockEntity.MAX_PROGRESS) + "%"));
    }
}
