package com.hbm.client.gui.screens.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKHeaterBlockEntity;
import com.hbm.inventory.container.machine.rbmk.RBMKHeaterMenu;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge port of CE {@code GUIRBMKHeater}.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUIRBMKHeater.java
 * <p>
 * GUI with two fluid tank overlays: feed (input) and steam (output).
 */
public class RBMKHeaterScreen extends AbstractContainerScreen<RBMKHeaterMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/rbmk/gui_rbmk_heater.png");

    public RBMKHeaterScreen(RBMKHeaterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        RBMKHeaterBlockEntity be = menu.be;

        // CE GUIRBMKHeater.java:49-50: render feed tank (14x58px) at x=68, y=82 (bottom-up fill)
        int feedFill = be.feed.getMaxFill() > 0 ? (int) (be.feed.getFill() * 58 / be.feed.getMaxFill()) : 0;
        if (feedFill > 0) {
            graphics.blit(TEXTURE, leftPos + 68, topPos + 82 - feedFill, 176, 58 - feedFill, 14, feedFill);
        }

        // CE GUIRBMKHeater.java:50: render steam tank (14x58px) at x=126, y=82 (bottom-up fill)
        int steamFill = be.steam.getMaxFill() > 0 ? (int) (be.steam.getFill() * 58 / be.steam.getMaxFill()) : 0;
        if (steamFill > 0) {
            graphics.blit(TEXTURE, leftPos + 126, topPos + 82 - steamFill, 190, 58 - steamFill, 14, steamFill);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // CE GUIRBMKHeater.java:37-39: title centered + inventory label
        graphics.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // CE GUIRBMKHeater.java:29-31: tank tooltips
        RBMKHeaterBlockEntity be = menu.be;
        if (mouseX >= leftPos + 68 && mouseX < leftPos + 68 + 16 && mouseY >= topPos + 24 && mouseY < topPos + 24 + 58) {
            graphics.renderTooltip(this.font, Component.literal("Feed: " + be.feed.getFill() + " / " + be.feed.getMaxFill() + " mB"), mouseX, mouseY);
        }
        if (mouseX >= leftPos + 126 && mouseX < leftPos + 126 + 16 && mouseY >= topPos + 24 && mouseY < topPos + 24 + 58) {
            graphics.renderTooltip(this.font, Component.literal("Steam: " + be.steam.getFill() + " / " + be.steam.getMaxFill() + " mB"), mouseX, mouseY);
        }
    }
}
