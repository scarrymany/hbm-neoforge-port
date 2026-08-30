package com.hbm.inventory.gui;

import com.hbm.blockentity.machine.BatteryBlockEntity;
import com.hbm.inventory.container.BatteryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;

/**
 * Single-block battery screen, ported from CE's {@code com.hbm.inventory.gui.GUIMachineBattery} (read
 * in full) - power-fill bar (via {@link #drawElectricityInfo}) plus a hover tooltip; CE's own
 * redstone-mode/priority icon buttons are dropped, matching {@link BatteryBlockEntity}'s own
 * documented "no GUI mode-toggle buttons" scope note (no server-bound GUI-button packet
 * infrastructure exists yet in this port - the fields those buttons would cycle still work correctly
 * with their CE-matching defaults, just not adjustable from this screen yet).
 *
 * <p><b>Texture note</b>: see {@link GuiInfoContainer}'s own javadoc - this port has no
 * {@code assets/hbm/textures/**} tree yet, so this renders NeoForge's missing-texture placeholder.
 */
public class BatteryScreen extends GuiInfoContainer<BatteryMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/storage/gui_battery.png");

    private final BatteryBlockEntity battery;

    public BatteryScreen(BatteryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.battery = menu.be;
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.drawElectricityInfo(guiGraphics, mouseX, mouseY, this.leftPos + 71, this.topPos + 17, 34, 52,
                battery.getPower(), battery.getMaxPower());
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (battery.getPower() > 0) {
            int filled = (int) battery.getPowerRemainingScaled(52);
            guiGraphics.blit(TEXTURE, this.leftPos + 71, this.topPos + 69 - filled, 176, 52 - filled, 34, filled);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
