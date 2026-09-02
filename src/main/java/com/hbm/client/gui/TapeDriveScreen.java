package com.hbm.client.gui;

import com.hbm.blockentity.machine.TapeDriveBlockEntity;
import com.hbm.inventory.container.TapeDriveMenu;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Port of CE {@code com.hbm.inventory.gui.GUITapeDrive}.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUITapeDrive.java
 */
public class TapeDriveScreen extends AbstractContainerScreen<TapeDriveMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_tape_drive.png");

    public TapeDriveScreen(TapeDriveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // CE GUITapeDrive.java:41-45: render filled tape overlays
        TapeDriveBlockEntity be = menu.be;
        for (int i = 0; i < 12; i++) {
            if (be.tapes[i] == TapeDriveBlockEntity.SLOT_FILLED_TAPE) {
                int col = i % 6;
                int row = i / 6;
                graphics.blit(TEXTURE, leftPos + 34 + col * 18, topPos + 26 + row * 18, 176, 0, 18, 18);
            }
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // CE GUITapeDrive.java:30-32
        Component name = this.menu.be.getDisplayName();
        graphics.drawString(this.font, name, (this.imageWidth - this.font.width(name)) / 2, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
