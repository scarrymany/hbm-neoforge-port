package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineBlastFurnaceBlockEntity;
import com.hbm.inventory.container.machine.dummyable.BlastFurnaceMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIBlastFurnace} — fuel/progress/tanks, CE coords. */
public class BlastFurnaceScreen extends GuiInfoContainer<BlastFurnaceMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_blast_furnace.png");

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
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineBlastFurnaceBlockEntity be = this.getMenu().be;
        int fuel = be.fuel * 52 / MachineBlastFurnaceBlockEntity.MAX_FUEL;
        if (fuel > 0) guiGraphics.blit(TEXTURE, x + 44, y + 81, 176, 0, 16, fuel);
        int prog = be.getProgressScaled(16);
        if (prog > 0) guiGraphics.blit(TEXTURE, x + 101, y + 36, 192, 0, prog, 16);
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
