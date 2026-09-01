package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterFireboxBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FireboxMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIFirebox} — burn + stored heat. */
public class FireboxScreen extends GuiInfoContainer<FireboxMenu> {

    public FireboxScreen(FireboxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        HeaterFireboxBlockEntity be = this.getMenu().be;
        int bh = be.maxBurnTime <= 0 ? 0 : be.burnTime * 14 / be.maxBurnTime;
        guiGraphics.fill(x + 80, y + 54 + (14 - bh), x + 94, y + 68, 0xFFFF6622);
        int hh = be.heatEnergy * 52 / HeaterFireboxBlockEntity.MAX_HEAT;
        guiGraphics.fill(x + 143, y + 70 - hh, x + 159, y + 70, 0xFFFF4400);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        HeaterFireboxBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 54, 14, 14,
                Component.literal("Burn: " + be.burnTime + " / " + be.maxBurnTime));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 143, topPos + 18, 16, 52,
                Component.literal(be.heatEnergy + " TU"),
                Component.literal(be.burnHeat + " TU/t"));
    }
}
