package com.hbm.inventory.gui.machine.rbmk;

import com.hbm.inventory.container.machine.rbmk.RBMKRodMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.items.machine.ItemRBMKRod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Fuel rod channel screen. No {@code assets/hbm/textures/**} tree exists yet in this port (see
 * {@link GuiInfoContainer}'s own javadoc) - background is a plain filled panel rather than a texture
 * blit, functional but not pixel-styled; a later texture-asset pass can swap {@link #renderBg} for a
 * real blit without touching layout logic.
 */
public class RBMKRodScreen extends GuiInfoContainer<RBMKRodMenu> {

    public RBMKRodScreen(RBMKRodMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xC0C6C6C6);
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
