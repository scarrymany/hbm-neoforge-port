package com.hbm.inventory.gui.bomb;

import com.hbm.inventory.container.bomb.NukeCustomMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class NukeCustomScreen extends GuiInfoContainer<NukeCustomMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/bomb_generic.png");

    public NukeCustomScreen(NukeCustomMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 84, 160, 10,
                Component.literal(String.format("TNT %.1f  Nuke %.1f  Hydro %.1f", be.tnt, be.nuke, be.hydro)));
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 94, 160, 10,
                Component.literal(String.format("Bale %.1f  Dirty %.1f  Schrab %.1f", be.bale, be.dirty, be.schrab)));
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 104, 160, 10,
                Component.literal(String.format("Sol %.1f  Euph %.1f  Falling %s", be.sol, be.euph, be.isFalling())));
    }
}
