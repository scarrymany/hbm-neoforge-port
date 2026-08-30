package com.hbm.inventory.gui;

import com.hbm.blockentity.machine.CrateBlockEntity;
import com.hbm.inventory.container.CrateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Mass storage crate screen, ported from CE's {@code com.hbm.inventory.gui.GUICrateBase} (read in
 * full) - per-{@link CrateBlockEntity.CrateType} background texture/dimensions/label colors, all read
 * straight off the block entity (see that class's {@link CrateBlockEntity.CrateType} table), plus the
 * player-inventory label. Cross-checked against Neo Edition's own real, compiling
 * {@code com.hbm.inventory.screens.CrateScreen} for the exact {@code renderBg}/{@code renderLabels}
 * override shapes.
 *
 * <p><b>Texture note</b>: per {@link GuiInfoContainer}'s own javadoc, this port has no
 * {@code assets/hbm/textures/**} tree yet (texture porting is a separate, later pass) - this renders
 * NeoForge's missing-texture placeholder rather than crashing, same as every other Phase 2 GUI so far.
 */
public class CrateScreen extends GuiInfoContainer<CrateMenu> {

    private final CrateBlockEntity crate;

    public CrateScreen(CrateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.crate = menu.be;
        this.imageWidth = crate.getCrateType().guiWidth;
        this.imageHeight = crate.getCrateType().guiHeight;
        this.inventoryLabelX = crate.getCrateType().inventoryLabelX;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(crate.getCrateType().texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6,
                crate.getCrateType().titleColor, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.imageHeight - 96 + 2,
                crate.getCrateType().inventoryLabelColor, false);
    }
}
