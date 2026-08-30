package com.hbm.inventory.gui;

import com.hbm.blockentity.machine.LaunchpadSoyuzBlockEntity;
import com.hbm.inventory.container.LaunchpadSoyuzMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for {@link LaunchpadSoyuzMenu}, ported from CE's {@code GUILaunchpadSoyuz} at the
 * signature/layout level (see {@link com.hbm.inventory.gui.LaunchPadScreen}'s javadoc for the same
 * caveat - the full 3D crane/carriage animation preview CE's GUI drew is a client-model-renderer
 * concern, Phase 5 scope, not this screen). Shows power, fuel-tank fill, and the current
 * {@link LaunchpadSoyuzBlockEntity.SoyuzStatus} as plain text.
 */
public class LaunchpadSoyuzScreen extends GuiInfoContainer<LaunchpadSoyuzMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/machine/gui_launchpad_soyuz.png");

    private final LaunchpadSoyuzBlockEntity pad;

    public LaunchpadSoyuzScreen(LaunchpadSoyuzMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.pad = menu.be;
        this.imageWidth = 194;
        this.imageHeight = 240;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        pad.tanks[0].renderTank(this.leftPos + 152, this.topPos + 96 + 48, 0, 16, 48);
        pad.tanks[1].renderTank(this.leftPos + 170, this.topPos + 96 + 48, 0, 16, 48);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.drawElectricityInfo(guiGraphics, mouseX, mouseY, this.leftPos + 134, this.topPos + 96, 18, 18, pad.getPower(), pad.getMaxPower());
        pad.tanks[0].renderTankTooltip(guiGraphics, mouseX, mouseY, this.leftPos + 152, this.topPos + 96, 16, 48);
        pad.tanks[1].renderTankTooltip(guiGraphics, mouseX, mouseY, this.leftPos + 170, this.topPos + 96, 16, 48);

        guiGraphics.drawString(this.font, "Status: " + pad.soyuzStatus.name(), this.leftPos + 8, this.topPos + this.imageHeight - 110, 0xFFFFFF, false);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
