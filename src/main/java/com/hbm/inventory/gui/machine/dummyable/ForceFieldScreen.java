package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineForceFieldBlockEntity;
import com.hbm.inventory.container.machine.dummyable.ForceFieldMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIForceField} — {@code gui_field.png} 176×168. */
public class ForceFieldScreen extends GuiInfoContainer<ForceFieldMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/gui_field.png");

    public ForceFieldScreen(ForceFieldMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineForceFieldBlockEntity be = this.getMenu().be;
        int i = (int) be.getPowerScaled(52);
        guiGraphics.blit(TEXTURE, x + 8, y + 69 - i, 176, 52 - i, 16, i);
        int j = be.getHealthScaled(52);
        guiGraphics.blit(TEXTURE, x + 62, y + 69 - j, 192, 52 - j, 16, j);
        if (be.isOn) {
            guiGraphics.blit(TEXTURE, x + 142, y + 34, 176, 52, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineForceFieldBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 17, 16, 52, be.power, MachineForceFieldBlockEntity.maxPower);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 62, topPos + 17, 16, 52,
                Component.literal(be.health + " / " + be.maxHealth + "HP"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 142, 34, 18, 18)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, ForceFieldMenu.BUTTON_ON);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
