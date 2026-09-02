package com.hbm.inventory.gui.machine.rbmk;

import com.hbm.inventory.container.machine.rbmk.RBMKRodMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.items.machine.ItemRBMKRod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class RBMKRodScreen extends GuiInfoContainer<RBMKRodMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/reactors/gui_rbmk_element.png");

    public RBMKRodScreen(RBMKRodMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        ItemStack stack = getMenu().be.inventory.getStackInSlot(0);
        if (stack.getItem() instanceof ItemRBMKRod) {
            guiGraphics.drawString(this.font,
                    "Enrichment: " + (int) (ItemRBMKRod.getEnrichment(stack) * 100) + "%",
                    leftPos + 8, topPos + 60, 0x404040, false);
            guiGraphics.drawString(this.font,
                    "Xenon: " + (int) ItemRBMKRod.getPoison(stack) + "%",
                    leftPos + 8, topPos + 72, 0x404040, false);
            guiGraphics.drawString(this.font,
                    "Core/Hull: " + (int) ItemRBMKRod.getCoreHeat(stack) + " / " + (int) ItemRBMKRod.getHullHeat(stack) + " C",
                    leftPos + 8, topPos + 84, 0x404040, false);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
