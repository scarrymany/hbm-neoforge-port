package com.hbm.inventory.gui.machine.oil;

import com.hbm.inventory.container.machine.oil.MachineOilWellMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported (visually, from CE's {@code GUIMachineOilWell}) as a plain panel - see
 * {@code MachineRTGScreen}'s javadoc for the no-texture-yet rationale, shared by all three
 * extractors (matches CE: derrick/pumpjack/fracking tower all open the same GUI/Container pair).
 * Renders the oil and gas tanks plus a plain-text status line for
 * {@link com.hbm.blockentity.machine.oil.OilDrillBaseBlockEntity#indicator} (0 = running, 1 = idle
 * at max drill depth, 2 = insufficient power / hit unbreakable rock, 3 = fracking tower out of
 * solution) - CE used {@link GuiInfoContainer#drawInfoPanel} icon sprites for this, not ported yet
 * (no {@code gui_utility.png} asset exists in this port's resources, see that method's own javadoc).
 */
public class MachineOilWellScreen extends GuiInfoContainer<MachineOilWellMenu> {

    public MachineOilWellScreen(MachineOilWellMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 184;
        this.imageHeight = 202;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        var be = this.getMenu().be;
        // CE GUIMachineOilWell.java:78-82 — tanks sit left of canister slots 94/130.
        be.getOilTank().renderTank(x + 76, y + 74, 0, 16, 52);
        be.getGasTank().renderTank(x + 112, y + 74, 0, 16, 52);
        if (be.tanks.size() >= 3) {
            be.tanks.get(2).renderTank(x + 54, y + 77, 0, 6, 32);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 6, 160, 12, this.getMenu().be.power, this.getMenu().be.getMaxPower());
        var be = this.getMenu().be;
        be.getOilTank().renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 76, topPos + 74 - 52, 16, 52);
        be.getGasTank().renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 112, topPos + 74 - 52, 16, 52);
        if (be.tanks.size() >= 3) {
            be.tanks.get(2).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 54, topPos + 45, 6, 32);
        }
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 20, 160, 10, Component.literal(statusText()));
    }

    private String statusText() {
        return switch (this.getMenu().be.indicator) {
            case 0 -> "Drilling...";
            case 1 -> "Idle - max depth reached";
            case 2 -> "Insufficient power / blocked";
            case 3 -> "Out of fracking solution";
            default -> "";
        };
    }
}
