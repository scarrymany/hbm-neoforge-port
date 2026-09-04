package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterFireboxBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FireboxMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIFirebox} — burn + stored heat. */
public class FireboxScreen extends GuiInfoContainer<FireboxMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_firebox.png");

    public FireboxScreen(FireboxMenu menu, Inventory inventory, Component title) {
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

        HeaterFireboxBlockEntity be = this.getMenu().be;
        int bh = be.maxBurnTime <= 0 ? 0 : be.burnTime * 14 / be.maxBurnTime;
        if (bh > 0) guiGraphics.blit(TEXTURE, x + 80, y + 54 + (14 - bh), 176, 14 - bh, 14, bh);
        int hh = be.heatEnergy * 52 / HeaterFireboxBlockEntity.maxHeatEnergy;
        guiGraphics.fill(x + 143, y + 70 - hh, x + 159, y + 70, 0xFFFF4400);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        HeaterFireboxBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 54, 14, 14,
                Component.literal("Burn: " + be.burnTime + " / " + be.maxBurnTime));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 143, topPos + 18, 16, 52,
                Component.literal(be.heatEnergy + " TU"),
                Component.literal(be.burnHeat + " TU/t"));
    }
}
