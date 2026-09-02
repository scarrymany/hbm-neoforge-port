package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineRotaryFurnaceBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RotaryFurnaceMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineRotaryFurnace} slot/tank layout, painted panel. */
public class RotaryFurnaceScreen extends GuiInfoContainer<RotaryFurnaceMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_rotary_furnace.png");

public RotaryFurnaceScreen(RotaryFurnaceMenu menu, Inventory inventory, Component title) {
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

        MachineRotaryFurnaceBlockEntity be = this.getMenu().be;
        int p = (int) (be.progress * 36);
        guiGraphics.fill(x + 62, y + 20, x + 62 + p, y + 28, be.isProgressing ? 0xFFFF8800 : 0xFF664400);
        if (be.maxBurnTime > 0) {
            int h = be.burnTime * 14 / be.maxBurnTime;
            guiGraphics.fill(x + 44, y + 72 - h, x + 58, y + 72, 0xFFFF2200);
        }
        be.process.renderTank(x + 80, y + 70, 0, 16, 52);
        be.steam.renderTank(x + 98, y + 70, 0, 16, 52);
        be.spent.renderTank(x + 116, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineRotaryFurnaceBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 62, topPos + 20, 36, 8,
                Component.literal(be.isProgressing ? "Smelting" : "Idle"),
                Component.literal(String.format("%.0f%%", be.progress * 100)));
        be.process.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 18, 16, 52);
        be.steam.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 98, topPos + 18, 16, 52);
        be.spent.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 18, 16, 52);
    }
}
