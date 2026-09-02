package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineRadGenBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RadGenMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineRadGen} — burn bar + power. */
public class RadGenScreen extends GuiInfoContainer<RadGenMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/gui_rtg.png");

    public RadGenScreen(RadGenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 184;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineRadGenBlockEntity be = this.getMenu().be;
        int ph = (int) (be.getPower() * 70 / Math.max(1, be.getMaxPower()));
        guiGraphics.fill(x + 70, y + 88 - ph, x + 86, y + 88, 0xFFFFCC00);
        int bh = be.maxBurnTime <= 0 ? 0 : be.burnTime * 70 / be.maxBurnTime;
        guiGraphics.fill(x + 90, y + 88 - bh, x + 98, y + 88, 0xFF66FF66);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineRadGenBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 70, 18, 16, 70, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 90, topPos + 18, 8, 70,
                Component.literal("Burn: " + be.burnTime + " / " + be.maxBurnTime),
                Component.literal(be.production + " HE/t"));
    }
}
