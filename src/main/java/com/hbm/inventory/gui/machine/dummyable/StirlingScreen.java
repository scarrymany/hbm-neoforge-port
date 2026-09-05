package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineStirlingBlockEntity;
import com.hbm.inventory.container.machine.dummyable.StirlingMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code MachineStirling} overlay — heat / HE / cog / overspeed. */
public class StirlingScreen extends GuiInfoContainer<StirlingMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/generators/gui_combustion.png");

public StirlingScreen(StirlingMenu menu, Inventory inventory, Component title) {
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

        MachineStirlingBlockEntity be = this.getMenu().be;
        int max = Math.max(1, be.maxHeat());
        int hh = Math.min(52, be.heat * 52 / max);
        guiGraphics.fill(x + 26, y + 70 - hh, x + 42, y + 70, 0xFFFF4400);
        long pmax = Math.max(1L, be.getMaxPower());
        int ph = (int) (be.powerBuffer * 52L / pmax);
        guiGraphics.fill(x + 143, y + 70 - ph, x + 159, y + 70, 0xFF44CCFF);
        if (!be.hasCog) guiGraphics.fill(x + 70, y + 30, x + 106, y + 46, 0xFFAA2222);
        if (be.overspeed > 60) guiGraphics.fill(x + 70, y + 50, x + 106, y + 58, 0xFFFFAA00);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineStirlingBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 26, topPos + 18, 16, 52,
                Component.literal(be.heat + " / " + be.maxHeat() + " TU/t"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 143, topPos + 18, 16, 52,
                Component.literal(be.powerBuffer + " HE/t"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 70, topPos + 30, 36, 16,
                Component.literal(be.hasCog ? "cog OK" : "MISSING GEAR"));
    }
}
