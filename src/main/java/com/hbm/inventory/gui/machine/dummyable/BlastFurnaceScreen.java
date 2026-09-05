package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineBlastFurnaceBlockEntity;
import com.hbm.inventory.container.machine.dummyable.BlastFurnaceMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * Exact CE {@code GUIBlastFurnace} on existing {@code gui_blast_furnace.png} 176×222.
 * Fuel/progress stack 62,{@code 106-fuel}; fire 81,64; tanks tooltip 25,71 / 25,17.
 * Invented 16px fuel blit + side {@code renderTank} columns removed.
 * {@code GUIElements.drawSmoothGauge} stay skipped (helper not in this port).
 */
public class BlastFurnaceScreen extends GuiInfoContainer<BlastFurnaceMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_blast_furnace.png");

    public BlastFurnaceScreen(BlastFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineBlastFurnaceBlockEntity be = this.getMenu().be;
        // CE GUIBlastFurnace.java:65-73
        int fuel = (int) Math.round((double) be.fuel * 26D / (double) MachineBlastFurnaceBlockEntity.MAX_FUEL);
        int prog = (int) Math.round(be.progress * (88D - fuel));
        if (prog > 0) {
            guiGraphics.blit(TEXTURE, x + 62, y + 106 - prog - fuel, 176, 102 - prog - fuel, 56, prog);
        }
        if (fuel > 0) {
            guiGraphics.blit(TEXTURE, x + 62, y + 106 - fuel, 176, 128 - fuel, 56, fuel);
        }
        if (be.isProgressing) {
            guiGraphics.blit(TEXTURE, x + 81, y + 64, 176, 0, 14, 14);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :55 — title centered
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineBlastFurnaceBlockEntity be = this.getMenu().be;
        if (this.menu.getCarried().isEmpty()) {
            Slot fuel = this.menu.getSlot(0);
            if (isHovered(mouseX, mouseY, fuel.x, fuel.y, 16, 16) && !fuel.hasItem()) {
                List<String> bonuses = be.burnModule.getHeatDesc();
                if (!bonuses.isEmpty()) {
                    List<Component> lines = new ArrayList<>(bonuses.size());
                    for (String line : bonuses) {
                        lines.add(Component.literal(line));
                    }
                    guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                }
            }
        }
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 79, topPos + 62, 18, 18, mouseX, mouseY,
                Component.literal("Speed: " + (int) (be.speed * 100) + "%"));
        be.airblast.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 25, topPos + 71, 18, 18);
        be.flue.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 25, topPos + 17, 18, 18);
    }
}
