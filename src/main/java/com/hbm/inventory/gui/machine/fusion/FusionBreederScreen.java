package com.hbm.inventory.gui.machine.fusion;

import com.hbm.blockentity.machine.fusion.FusionBreederBlockEntity;
import com.hbm.inventory.container.machine.fusion.FusionBreederMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.util.BobMathUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIFusionBreeder}. No CE png in tree — fill-rect. */
public class FusionBreederScreen extends GuiInfoContainer<FusionBreederMenu> {

    public FusionBreederScreen(FusionBreederMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
        var be = this.getMenu().be;
        be.tanks[0].renderTank(x + 26, y + 18, 0, 16, 52);
        be.tanks[1].renderTank(x + 134, y + 18, 0, 16, 52);
        int p = (int) Math.ceil(be.progress * 42 / FusionBreederBlockEntity.CAPACITY);
        if (p > 0) guiGraphics.fill(x + 67, y + 48, x + 67 + p, y + 58, 0xFF3C78C8);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 79, topPos + 23, 18, 18,
                Component.literal("-> " + (int) Math.ceil(be.neutronEnergy) + " flux/t"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 67, topPos + 46, 42, 14,
                Component.literal(BobMathUtil.getShortNumber((long) Math.ceil(be.progress)) + " / "
                        + BobMathUtil.getShortNumber((long) Math.ceil(FusionBreederBlockEntity.CAPACITY)) + " flux"));
    }
}
