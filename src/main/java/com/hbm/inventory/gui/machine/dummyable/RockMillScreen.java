package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineRockMillBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RockMillMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineRockMill} — power + water + progress. */
public class RockMillScreen extends GuiInfoContainer<RockMillMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_rockmill.png");

    public RockMillScreen(RockMillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 220;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineRockMillBlockEntity be = this.getMenu().be;
        int p = be.getProgressScaled(71);
        if (be.didProcess && p > 0) {
            guiGraphics.blit(TEXTURE, x + 62, y + 90, 176, 71, Math.min(p, 70), 16);
        }
        int pow = be.getMaxPower() <= 0 ? 0 : (int) ((long) be.getPower() * 71 / be.getMaxPower());
        if (pow > 0) guiGraphics.blit(TEXTURE, x + 152, y + 89 - pow, 176, 71 - pow, 16, pow);
        
        be.inputTank.renderTank(x + 8, y + 79, 0, 52, 16);
        be.outputTank.renderTank(x + 80, y + 79, 0, 52, 16);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineRockMillBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 152, 18, 16, 71, be.getPower(), be.getMaxPower());
        be.inputTank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 27, 52, 16);
        be.outputTank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 27, 52, 16);
    }
}
