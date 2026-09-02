package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineAutocrafterBlockEntity;
import com.hbm.inventory.container.machine.dummyable.AutocrafterMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** CE {@code GUIAutocrafter}. */
public class AutocrafterScreen extends GuiInfoContainer<AutocrafterMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_autocrafter.png");

    public AutocrafterScreen(AutocrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 240;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineAutocrafterBlockEntity be = this.getMenu().be;
        long max = Math.max(1L, be.getMaxPower());
        int ph = (int) (be.power * 52L / max);
        if (ph > 0) guiGraphics.blit(TEXTURE, x + 8, y + 88 - ph, 176, 52 - ph, 8, ph);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineAutocrafterBlockEntity be = this.getMenu().be;
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 36, 8, 52,
                Component.literal(be.power + " / " + be.getMaxPower() + " HE"),
                Component.literal(MachineAutocrafterBlockEntity.CONSUMPTION + " HE/craft"),
                Component.literal("Recipe " + (be.recipeCount == 0 ? 0 : be.recipeIndex + 1) + "/" + be.recipeCount));
    }
}
