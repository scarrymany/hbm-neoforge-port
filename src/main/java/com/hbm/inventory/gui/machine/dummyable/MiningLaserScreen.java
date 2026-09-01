package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineMiningLaserBlockEntity;
import com.hbm.inventory.container.machine.dummyable.MiningLaserMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * CE {@code GUIMachineMiningLaser} — {@code gui_laser_miner.png} 176×222.
 */
public class MiningLaserScreen extends GuiInfoContainer<MiningLaserMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_laser_miner.png");

    public MiningLaserScreen(MiningLaserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.titleLabelY = 4;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineMiningLaserBlockEntity be = this.getMenu().be;
        if (be.isOn) {
            guiGraphics.blit(TEXTURE, x + 61, y + 17, 200, 0, 18, 18);
        }
        int power = be.getPowerScaled(88);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, x + 8, y + 106 - power, 176, 88 - power, 16, power);
        }
        int progress = be.getProgressScaled(34);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, x + 66, y + 36, 192, 0, 8, progress);
        }
        drawInfoPanel(guiGraphics, x + 87, y + 31, 8);
        be.tank.renderTank(x + 35, y + 124, 0, 7, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        MachineMiningLaserBlockEntity be = this.getMenu().be;
        String name = be.getDisplayName().getString();
        guiGraphics.drawString(this.font, name, (this.imageWidth - this.font.width(name)) / 2, 4, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        String width = Integer.toString(be.getWidth());
        guiGraphics.drawString(this.font, width, 43 - this.font.width(width) / 2, 26, 0xFFFFFF, false);

        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 18, 16, 88, be.getPower(), be.getMaxPower());
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 87, topPos + 31, 8, 8, leftPos + 141, topPos + 55,
                Component.literal("Acceptable upgrades:"),
                Component.literal(" -Speed (stacks to level 12)"),
                Component.literal(" -Effectiveness (stacks to level 12)"),
                Component.literal(" -Overdrive (stacks to level 3)"),
                Component.literal(" -Fortune (stacks to level 3)"),
                Component.literal(" -Smelter (exclusive)"),
                Component.literal(" -Shredder (exclusive)"),
                Component.literal(" -Centrifuge (exclusive)"),
                Component.literal(" -Crystallizer (exclusive)"),
                Component.literal(" -Scream (4xSpeed, 4xCylces, 20xConsumption)"),
                Component.literal(" -Nullifier"));
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 72, 7, 52);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 61, 17, 18, 18)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, MiningLaserMenu.BUTTON_ON);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
