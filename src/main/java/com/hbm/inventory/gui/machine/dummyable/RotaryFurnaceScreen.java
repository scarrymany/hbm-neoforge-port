package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineRotaryFurnaceBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RotaryFurnaceMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.material.Mats;
import com.hbm.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * Exact CE {@code GUIMachineRotaryFurnace} on existing {@code gui_rotary_furnace.png} 176×186.
 * Progress 63,30 from 176,0; burn 26,{@code 69-b}; molten 98,{@code 70-amt}; tanks 8/134/152.
 * Invented {@code fill()} + wrong tank columns removed.
 */
public class RotaryFurnaceScreen extends GuiInfoContainer<RotaryFurnaceMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_rotary_furnace.png");

    public RotaryFurnaceScreen(RotaryFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineRotaryFurnaceBlockEntity be = this.getMenu().be;
        // CE GUIMachineRotaryFurnace.java:77-103
        int p = (int) Math.ceil(be.progress * 33);
        if (p > 0) {
            guiGraphics.blit(TEXTURE, x + 63, y + 30, 176, 0, p, 10);
        }
        if (be.maxBurnTime > 0) {
            int b = be.burnTime * 14 / be.maxBurnTime;
            if (b > 0) {
                guiGraphics.blit(TEXTURE, x + 26, y + 69 - b, 176, 24 - b, 14, b);
            }
        }
        if (be.output != null && be.output.material != null) {
            int hex = be.output.material.moltenColor;
            int amount = be.output.amount * 52 / MachineRotaryFurnaceBlockEntity.MAX_OUTPUT;
            if (amount > 0) {
                float r = ((hex >> 16) & 0xFF) / 255F;
                float g = ((hex >> 8) & 0xFF) / 255F;
                float bl = (hex & 0xFF) / 255F;
                guiGraphics.setColor(r, g, bl, 1F);
                guiGraphics.blit(TEXTURE, x + 98, y + 70 - amount, 176, 76 - amount, 16, amount);
                RenderSystem.enableBlend();
                guiGraphics.setColor(1F, 1F, 1F, 0.3F);
                guiGraphics.blit(TEXTURE, x + 98, y + 70 - amount, 176, 76 - amount, 16, amount);
                RenderSystem.disableBlend();
                guiGraphics.setColor(1F, 1F, 1F, 1F);
            }
        }
        be.process.renderTank(x + 8, y + 52, 0, 52, 16, 1);
        be.steam.renderTank(x + 134, y + 70, 0, 16, 52);
        be.spent.renderTank(x + 152, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :63-65 — inventory label only
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineRotaryFurnaceBlockEntity be = this.getMenu().be;
        be.process.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 36, 52, 16);
        be.steam.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 134, topPos + 18, 16, 52);
        be.spent.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 18, 16, 52);

        Slot fuel = this.menu.getSlot(4);
        if (isHovered(mouseX, mouseY, fuel.x, fuel.y, 16, 16) && !fuel.hasItem()) {
            List<String> bonuses = MachineRotaryFurnaceBlockEntity.burnModule.getDesc();
            if (!bonuses.isEmpty()) {
                List<Component> lines = new ArrayList<>(bonuses.size());
                for (String line : bonuses) {
                    lines.add(Component.literal(line));
                }
                guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            }
        }

        if (be.output == null || be.output.material == null) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 98, topPos + 18, 16, 52, mouseX, mouseY,
                    Component.literal("Empty").withStyle(ChatFormatting.RED));
        } else {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 98, topPos + 18, 16, 52, mouseX, mouseY,
                    Component.empty()
                            .append(be.output.material.getName())
                            .append(": ")
                            .append(Mats.formatAmount(be.output.amount, Screen.hasShiftDown()))
                            .withStyle(ChatFormatting.YELLOW));
        }
    }
}
