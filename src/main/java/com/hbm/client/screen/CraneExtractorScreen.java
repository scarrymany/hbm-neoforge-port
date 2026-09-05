package com.hbm.client.screen;

import com.hbm.main.MainRegistry;
import com.hbm.menu.CraneExtractorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * NeoForge port of CE's {@code GUICraneExtractor} - simplified without whitelist/maxEject buttons.
 * Displays 9 filter slots + 9 buffer slots + 2 upgrade slots.
 */
public class CraneExtractorScreen extends AbstractContainerScreen<CraneExtractorMenu> {

    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/container/crane_extractor.png");

    public CraneExtractorScreen(CraneExtractorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 185;
        this.imageWidth = 194;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component title = this.title;
        int titleX = (this.imageWidth - this.font.width(title)) / 2 - 18;
        graphics.drawString(this.font, title, titleX, 5, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 26, this.imageHeight - 96 + 2, 0x404040, false);
    }
}
