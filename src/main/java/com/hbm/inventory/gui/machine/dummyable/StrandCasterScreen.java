package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineStrandCasterBlockEntity;
import com.hbm.client.ClientScreens;
import com.hbm.inventory.container.machine.dummyable.StrandCasterMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.material.Mats;
import com.hbm.main.MainRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIMachineStrandCaster} — {@code gui_strand_caster.png} 176×214. */
public class StrandCasterScreen extends GuiInfoContainer<StrandCasterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_strand_caster.png");

    public StrandCasterScreen(StrandCasterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 214;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineStrandCasterBlockEntity be = this.getMenu().be;
        if (be.amount != 0 && be.type != null) {
            int targetHeight = Math.min(be.amount * 79 / Math.max(1, be.getCapacity()), 92);
            int hex = be.type.moltenColor;
            int r = (hex >> 16) & 0xFF;
            int g = (hex >> 8) & 0xFF;
            int b = hex & 0xFF;
            guiGraphics.setColor(r / 255F, g / 255F, b / 255F, 1F);
            guiGraphics.blit(TEXTURE, x + 17, y + 93 - targetHeight, 176, 89 - targetHeight, 34, targetHeight);
            guiGraphics.setColor(1F, 1F, 1F, 0.3F);
            guiGraphics.blit(TEXTURE, x + 17, y + 93 - targetHeight, 176, 89 - targetHeight, 34, targetHeight);
            guiGraphics.setColor(1F, 1F, 1F, 1F);
        }
        be.water.renderTank(x + 82, y + 38, 0, 16, 24);
        be.steam.renderTank(x + 82, y + 89, 0, 16, 24);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineStrandCasterBlockEntity be = this.getMenu().be;
        Component info = be.type == null
                ? Component.literal("Empty").withStyle(ChatFormatting.RED)
                : Component.translatable(be.type.getDescriptionId()).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(": ")).append(Mats.formatAmount(be.amount, ClientScreens.hasShiftDown()));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 16, topPos + 17, 36, 81, info);
        be.water.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 82, topPos + 14, 16, 24);
        be.steam.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 82, topPos + 65, 16, 24);
    }
}
