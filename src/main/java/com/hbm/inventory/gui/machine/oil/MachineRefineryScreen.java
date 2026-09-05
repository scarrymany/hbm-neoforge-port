package com.hbm.inventory.gui.machine.oil;

import com.hbm.blockentity.machine.oil.MachineRefineryBlockEntity;
import com.hbm.inventory.container.machine.oil.MachineRefineryMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.main.MainRegistry;
import com.hbm.util.Tuple;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Exact CE {@code GUIMachineRefinery} on existing {@code gui_refinery.png} 350×256, canvas 210×231.
 * Power 186,69-j / input overlay 33,130 / pipes 52,63+32+24 / 36,16 / outs 86..146,95.
 */
public class MachineRefineryScreen extends GuiInfoContainer<MachineRefineryMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/gui_refinery.png");
    private static final int TEX_W = 350;
    private static final int TEX_H = 256;

    public MachineRefineryScreen(MachineRefineryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 210;
        this.imageHeight = 231;
        this.inventoryLabelY = this.imageHeight - 96 + 4;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        blit350(guiGraphics, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineRefineryBlockEntity be = this.getMenu().be;
        // CE GUIMachineRefinery.java:75-76
        int j = (int) be.getPowerScaled(50);
        if (j > 0) {
            blit350(guiGraphics, x + 186, y + 69 - j, 210, 52 - j, 16, j);
        }

        FluidTankNTM input = be.tanks.get(0);
        if (input.getFill() != 0) {
            int targetHeight = input.getFill() * 101 / input.getMaxFill();
            tint(guiGraphics, input.getTankType().getColor());
            blit350(guiGraphics, x + 33, y + 130 - targetHeight, 226, 101 - targetHeight, 16, targetHeight);
            guiGraphics.setColor(1F, 1F, 1F, 1F);
        }

        Tuple.Quintet<FluidStack, FluidStack, FluidStack, FluidStack, ItemStack> recipe =
                RefineryRecipes.getRefinery(input.getTankType());
        if (recipe == null) {
            // CE :102-105
            blit350(guiGraphics, x + 52, y + 63, 247, 1, 33, 48);
            blit350(guiGraphics, x + 52, y + 32, 247, 50, 66, 52);
            blit350(guiGraphics, x + 52, y + 24, 247, 145, 86, 35);
            blit350(guiGraphics, x + 36, y + 16, 211, 119, 122, 25);
        } else {
            // CE :108-128
            tint(guiGraphics, recipe.getV().type.getColor());
            blit350(guiGraphics, x + 52, y + 63, 247, 1, 33, 48);
            tint(guiGraphics, recipe.getW().type.getColor());
            blit350(guiGraphics, x + 52, y + 32, 247, 50, 66, 52);
            tint(guiGraphics, recipe.getX().type.getColor());
            blit350(guiGraphics, x + 52, y + 24, 247, 145, 86, 35);
            tint(guiGraphics, recipe.getY().type.getColor());
            blit350(guiGraphics, x + 36, y + 16, 211, 119, 122, 25);
            guiGraphics.setColor(1F, 1F, 1F, 1F);
        }

        be.tanks.get(1).renderTank(x + 86, y + 95, 0, 16, 52);
        be.tanks.get(2).renderTank(x + 106, y + 95, 0, 16, 52);
        be.tanks.get(3).renderTank(x + 126, y + 95, 0, 16, 52);
        be.tanks.get(4).renderTank(x + 146, y + 95, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :52-55
        int nameX = this.imageWidth / 2 - 34 / 2 - this.font.width(this.title) / 2;
        guiGraphics.drawString(this.font, this.title, nameX, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineRefineryBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 186, topPos + 18, 16, 52, be.getPower(), be.getMaxPower());
        be.tanks.get(0).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 30, topPos + 27, 21, 104);
        be.tanks.get(1).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 86, topPos + 42, 16, 52);
        be.tanks.get(2).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 106, topPos + 42, 16, 52);
        be.tanks.get(3).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 126, topPos + 42, 16, 52);
        be.tanks.get(4).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 146, topPos + 42, 16, 52);
    }

    private static void blit350(GuiGraphics guiGraphics, int x, int y, int u, int v, int w, int h) {
        guiGraphics.blit(TEXTURE, x, y, (float) u, (float) v, w, h, TEX_W, TEX_H);
    }

    private static void tint(GuiGraphics guiGraphics, int color) {
        guiGraphics.setColor(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 1F);
    }
}
