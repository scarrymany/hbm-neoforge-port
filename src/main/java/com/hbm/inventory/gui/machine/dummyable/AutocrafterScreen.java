package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineAutocrafterBlockEntity;
import com.hbm.inventory.container.machine.dummyable.AutocrafterMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/** Exact CE {@code GUIAutocrafter}: power bar + empty-hand RC mode/recipe tooltips. */
public class AutocrafterScreen extends GuiInfoContainer<AutocrafterMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_autocrafter.png");

    public AutocrafterScreen(AutocrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 240;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // Exact CE GUIAutocrafter.java:37-59 — empty main-hand hover, not cursor
        if (this.minecraft == null || this.minecraft.player == null) return;
        if (!this.minecraft.player.getMainHandItem().isEmpty()) return;

        MachineAutocrafterBlockEntity be = this.getMenu().be;
        Component rc = Component.literal(I18nUtil.resolveKey("desc.rcchange")).withStyle(ChatFormatting.RED);
        for (int i = 0; i < 9; i++) {
            Slot slot = this.menu.getSlot(i);
            if (!this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) || be.modes[i] == null) continue;
            String mode = be.modes[i];
            String label = switch (mode) {
                case MachineAutocrafterBlockEntity.MODE_EXACT -> I18nUtil.resolveKey("desc.exact");
                case MachineAutocrafterBlockEntity.MODE_WILDCARD -> I18nUtil.resolveKey("desc.wildcard");
                default -> I18nUtil.resolveKey("desc.oredictmatch") + " " + mode;
            };
            guiGraphics.renderComponentTooltip(this.font, List.of(rc,
                    Component.literal(label).withStyle(ChatFormatting.YELLOW)), mouseX, mouseY - 30);
            return;
        }

        Slot recipe = this.menu.getSlot(9);
        if (this.isHovering(recipe.x, recipe.y, 16, 16, mouseX, mouseY)
                && !be.getCheckedInventory().getStackInSlot(9).isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, List.of(rc,
                    Component.literal((be.recipeIndex + 1) + " / " + be.recipeCount)
                            .withStyle(ChatFormatting.YELLOW)), mouseX, mouseY - 30);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineAutocrafterBlockEntity be = this.getMenu().be;
        // Exact CE GUIAutocrafter.java:78-79
        if (be.getMaxPower() > 0) {
            int i = (int) (be.getPower() * 52 / be.getMaxPower());
            if (i > 0) {
                guiGraphics.blit(TEXTURE, x + 17, y + 97 - i, 176, 52 - i, 16, i);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        MachineAutocrafterBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 17, topPos + 45, 16, 52, be.getPower(), be.getMaxPower());
    }
}
