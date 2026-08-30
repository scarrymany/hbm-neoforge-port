package com.hbm.inventory.gui;

import com.hbm.blockentity.machine.FluidTankBlockEntity;
import com.hbm.inventory.container.FluidTankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for {@link FluidTankBlockEntity} - the tank's own {@link com.hbm.inventory.fluid.tank.FluidTankNTM#renderTank}/
 * {@code renderTankTooltip} render themselves (see that class's own javadoc: CE's fluid tanks render
 * their own quad rather than this base class growing a generic tank widget - the same convention
 * {@link GuiInfoContainer}'s own javadoc documents), cross-checked against Neo Edition's real,
 * compiling {@code MachineFluidTankScreen} for the exact call shape. No item slots (see
 * {@link FluidTankBlockEntity}'s own javadoc on its 0-slot inventory) - filling/draining happens
 * through the fluid-handler capability (buckets, pipes, other mods' fluid transport), not a GUI slot.
 *
 * <p><b>Texture note</b>: see {@link GuiInfoContainer}'s own javadoc - this port has no
 * {@code assets/hbm/textures/**} tree yet, so this renders NeoForge's missing-texture placeholder.
 */
public class FluidTankScreen extends GuiInfoContainer<FluidTankMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/storage/gui_tank.png");

    private final FluidTankBlockEntity tank;

    public FluidTankScreen(FluidTankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.tank = menu.be;
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        tank.getTank().renderTankTooltip(guiGraphics, mouseX, mouseY, this.leftPos + 71, this.topPos + 69 - 52, 34, 52);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        guiGraphics.blit(TEXTURE, this.leftPos + 151, this.topPos + 34, 176, tank.getMode() * 18, 18, 18);
        tank.getTank().renderTank(this.leftPos + 71, this.topPos + 69, 0, 34, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
