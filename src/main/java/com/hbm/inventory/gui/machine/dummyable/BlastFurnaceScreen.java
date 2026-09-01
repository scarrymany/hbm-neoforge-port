package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineBlastFurnaceBlockEntity;
import com.hbm.inventory.container.machine.dummyable.BlastFurnaceMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIBlastFurnace} — fuel/progress/tanks, CE coords. */
public class BlastFurnaceScreen extends GuiInfoContainer<BlastFurnaceMenu> {

    public BlastFurnaceScreen(BlastFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MachineBlastFurnaceBlockEntity be = this.getMenu().be;
        int fuel = be.fuel * 52 / MachineBlastFurnaceBlockEntity.MAX_FUEL;
        guiGraphics.fill(x + 44, y + 81 + (52 - fuel), x + 60, y + 133, 0xFF442200);
        int prog = be.getProgressScaled(16);
        guiGraphics.fill(x + 101, y + 36, x + 101 + prog, y + 52, 0xFFFF6600);
        be.airblast.renderTank(x + 8, y + 70, 0, 16, 52);
        be.flue.renderTank(x + 152, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineBlastFurnaceBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 81, 16, 52,
                Component.literal("Fuel " + be.fuel + " / " + MachineBlastFurnaceBlockEntity.MAX_FUEL),
                Component.literal(be.isProgressing ? "Smelting ×" + String.format("%.1f", be.speed) : "Idle"));
        be.airblast.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 18, 16, 52);
        be.flue.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 18, 16, 52);
    }
}
