package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineStrandCasterBlockEntity;
import com.hbm.inventory.container.machine.dummyable.StrandCasterMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.material.Mats;
import com.hbm.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Exact CE {@code GUIMachineStrandCaster} on existing {@code gui_strand_caster.png} 176×214.
 * Molten 17,{@code 93-h} from 176,{@code 89-h}; tanks 82,38 / 82,89.
 */
public class StrandCasterScreen extends GuiInfoContainer<StrandCasterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_strand_caster.png");

    public StrandCasterScreen(StrandCasterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 214;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineStrandCasterBlockEntity be = this.getMenu().be;
        // CE GUIMachineStrandCaster.java:64-76
        if (be.amount != 0 && be.type != null) {
            int targetHeight = Math.min(be.amount * 79 / Math.max(1, be.getCapacity()), 92);
            int hex = be.type.moltenColor;
            float r = ((hex >> 16) & 0xFF) / 255F;
            float g = ((hex >> 8) & 0xFF) / 255F;
            float b = (hex & 0xFF) / 255F;
            guiGraphics.setColor(r, g, b, 1F);
            guiGraphics.blit(TEXTURE, x + 17, y + 93 - targetHeight, 176, 89 - targetHeight, 34, targetHeight);
            RenderSystem.enableBlend();
            guiGraphics.setColor(1F, 1F, 1F, 0.3F);
            guiGraphics.blit(TEXTURE, x + 17, y + 93 - targetHeight, 176, 89 - targetHeight, 34, targetHeight);
            RenderSystem.disableBlend();
            guiGraphics.setColor(1F, 1F, 1F, 1F);
        }
        be.water.renderTank(x + 82, y + 38, 0, 16, 24);
        be.steam.renderTank(x + 82, y + 89, 0, 16, 24);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :49 — inventory label only
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineStrandCasterBlockEntity be = this.getMenu().be;
        List<Component> melt = new ArrayList<>();
        if (be.type == null) {
            melt.add(Component.literal("Empty").withStyle(ChatFormatting.RED));
        } else {
            melt.add(Component.empty()
                    .append(be.type.getName())
                    .append(": ")
                    .append(Mats.formatAmount(be.amount, Screen.hasShiftDown()))
                    .withStyle(ChatFormatting.YELLOW));
        }
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 16, topPos + 17, 36, 81, mouseX, mouseY, melt);
        be.water.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 82, topPos + 14, 16, 24);
        be.steam.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 82, topPos + 65, 16, 24);
    }
}
