package com.hbm.inventory.gui;

import com.hbm.blockentity.bomb.LaunchPadBaseBlockEntity;
import com.hbm.inventory.container.LaunchPadMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for {@link LaunchPadMenu} (shared by the small and large launch pad, matching CE's own
 * {@code TileEntityLaunchPadBase.provideGUI} always returning {@code GUILaunchPadLarge}). Ported
 * from CE's {@code GUILaunchPadLarge} at the signature/layout level (per {@code docs/phase3/
 * missile_launch_infra.md}'s Deferred scope, which surveyed the {@code Container} pair but not the
 * full GUI render code) - power bar plus the two fuel/oxidizer tank bars, using
 * {@link com.hbm.inventory.fluid.tank.FluidTankNTM#renderTank}/{@code renderTankTooltip} directly,
 * matching this port's own established convention (see {@link GuiInfoContainer}'s class javadoc).
 */
public class LaunchPadScreen extends GuiInfoContainer<LaunchPadMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/bomb/gui_launch_pad.png");

    private final LaunchPadBaseBlockEntity pad;

    public LaunchPadScreen(LaunchPadMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.pad = menu.be;
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    // FluidTankNTM#renderTank draws via a raw Tesselator quad in absolute screen space (no
    // GuiGraphics pose-stack integration - confirmed by reading its body), so this passes
    // this.leftPos/this.topPos-relative absolute coordinates directly rather than a pushed
    // transform. Per its own javadoc, "y" is the tank's BOTTOM edge, not its top.
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 48;
    private static final int TANK0_X = 125;
    private static final int TANK1_X = 143;
    private static final int TANK_TOP_Y = 60;

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        pad.tanks[0].renderTank(this.leftPos + TANK0_X, this.topPos + TANK_TOP_Y + TANK_HEIGHT, 0, TANK_WIDTH, TANK_HEIGHT);
        pad.tanks[1].renderTank(this.leftPos + TANK1_X, this.topPos + TANK_TOP_Y + TANK_HEIGHT, 0, TANK_WIDTH, TANK_HEIGHT);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.drawElectricityInfo(guiGraphics, mouseX, mouseY, this.leftPos + 107, this.topPos + 90, 18, 18, pad.getPower(), pad.getMaxPower());
        pad.tanks[0].renderTankTooltip(guiGraphics, mouseX, mouseY, this.leftPos + TANK0_X, this.topPos + TANK_TOP_Y, TANK_WIDTH, TANK_HEIGHT);
        pad.tanks[1].renderTankTooltip(guiGraphics, mouseX, mouseY, this.leftPos + TANK1_X, this.topPos + TANK_TOP_Y, TANK_WIDTH, TANK_HEIGHT);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
