package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineHydrotreaterBlockEntity;
import com.hbm.inventory.container.machine.dummyable.HydrotreaterMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Exact CE {@code GUIMachineHydrotreater} on existing {@code gui_hydrotreater.png} 176×238.
 * Power 17,{@code 70-j} from 176,{@code 52-j} (span 54); tanks 35/53/125/143,70.
 * Invented power {@code fill()} removed.
 */
public class HydrotreaterScreen extends GuiInfoContainer<HydrotreaterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_hydrotreater.png");

    public HydrotreaterScreen(HydrotreaterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 238;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineHydrotreaterBlockEntity be = this.getMenu().be;
        // CE GUIMachineHydrotreater.java:64-70
        int j = (int) (be.getPower() * 54 / Math.max(1L, be.getMaxPower()));
        if (j > 0) {
            guiGraphics.blit(TEXTURE, x + 17, y + 70 - j, 176, 52 - j, 16, j);
        }
        be.oil.renderTank(x + 35, y + 70, 0, 16, 52);
        be.hydrogen.renderTank(x + 53, y + 70, 0, 16, 52);
        be.out1.renderTank(x + 125, y + 70, 0, 16, 52);
        be.out2.renderTank(x + 143, y + 70, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :53 — title white, y=5
        int nameX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        guiGraphics.drawString(this.font, this.title, nameX, 5, 0xffffff, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineHydrotreaterBlockEntity be = this.getMenu().be;
        be.oil.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 70 - 52, 16, 52);
        be.hydrogen.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 53, topPos + 70 - 52, 16, 52);
        be.out1.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 125, topPos + 70 - 52, 16, 52);
        be.out2.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 143, topPos + 70 - 52, 16, 52);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 17, topPos + 70 - 52, 16, 52, be.getPower(), be.getMaxPower());

        // CE getSlot(10) is stale H₂-slot leftover; catalyst is container slot 8 (CE :58).
        Slot catalyst = this.menu.getSlot(8);
        if (this.menu.getCarried().isEmpty() && !catalyst.hasItem()
                && isHovered(mouseX, mouseY, catalyst.x, catalyst.y, 16, 16)) {
            Item converter = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "catalytic_converter"));
            if (converter != Items.AIR) {
                guiGraphics.renderTooltip(this.font, new ItemStack(converter), mouseX, mouseY);
            }
        }
    }
}
