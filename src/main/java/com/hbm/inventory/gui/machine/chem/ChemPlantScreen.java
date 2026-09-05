package com.hbm.inventory.gui.machine.chem;

import com.hbm.blockentity.machine.chem.ChemPlantBlockEntity;
import com.hbm.inventory.container.machine.chem.ChemPlantMenu;
import com.hbm.inventory.gui.GUIScreenRecipeSelector;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.main.MainRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Exact CE {@code GUIMachineChemicalPlant}: {@code gui_chemplant.png} 176×256.
 * Power 152,18 16×61 / progress 62,126 / tanks 8+i*18,18 and 80+i*18,18 /
 * selector (7,125). Ghost inputs CE {@code GuiInfoContainerProcessor}.
 */
public class ChemPlantScreen extends GuiInfoContainer<ChemPlantMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_chemplant.png");

    public ChemPlantScreen(ChemPlantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        ChemPlantBlockEntity be = this.getMenu().be;
        if (be.maxPower > 0) {
            int p = (int) (be.power * 61 / be.maxPower);
            if (p > 0) {
                guiGraphics.blit(TEXTURE, x + 152, y + 79 - p, 176, 61 - p, 16, p);
            }
        }
        if (be.progress > 0) {
            int j = (int) Math.ceil(70 * be.progress);
            guiGraphics.blit(TEXTURE, x + 62, y + 126, 176, 61 + (be.restrictedMode ? 16 : 0), j, 16);
        }

        if (ChemicalPlantRecipes.INSTANCE.recipeOrderedList.isEmpty()) {
            ChemicalPlantRecipes.rebuild();
        }
        GenericRecipe recipe = ChemicalPlantRecipes.INSTANCE.recipeNameMap.get(be.recipe);
        if (be.didProcess) {
            guiGraphics.blit(TEXTURE, x + 51, y + 121, 195, 0, 3, 6);
            guiGraphics.blit(TEXTURE, x + 56, y + 121, 195, 0, 3, 6);
        } else if (recipe != null) {
            guiGraphics.blit(TEXTURE, x + 51, y + 121, 192, 0, 3, 6);
            if (be.power >= recipe.power) {
                guiGraphics.blit(TEXTURE, x + 56, y + 121, 192, 0, 3, 6);
            }
        }
        if (recipe != null) {
            guiGraphics.renderItem(recipe.getIcon(), x + 8, y + 126);
        }
        renderGhostInputs(guiGraphics, TEXTURE, recipe, new int[]{
                ChemPlantBlockEntity.ITEM_IN_START,
                ChemPlantBlockEntity.ITEM_IN_START + 1,
                ChemPlantBlockEntity.ITEM_IN_START + 2
        });

        for (int i = 0; i < 3; i++) {
            be.inputTanks[i].renderTank(x + 8 + i * 18, y + 52, 0, 16, 34);
            be.outputTanks[i].renderTank(x + 80 + i * 18, y + 52, 0, 16, 34);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Exact CE GUIMachineChemicalPlant.java:50 — title x=70
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, 70 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        ChemPlantBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 18, 16, 61, be.power, be.maxPower);
        for (int i = 0; i < 3; i++) {
            be.inputTanks[i].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 8 + i * 18, topPos + 18, 16, 34);
            be.outputTanks[i].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 80 + i * 18, topPos + 18, 16, 34);
        }
        if (isHovered(mouseX, mouseY, 7, 125, 18, 18)) {
            GenericRecipe recipe = ChemicalPlantRecipes.INSTANCE.recipeNameMap.get(be.recipe);
            if (recipe != null) {
                guiGraphics.renderComponentTooltip(this.font, recipe.print(), mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(this.font,
                        Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW),
                        mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 7, 125, 18, 18)) {
            click();
            ChemPlantBlockEntity be = this.getMenu().be;
            ChemicalPlantRecipes.rebuild();
            GUIScreenRecipeSelector.openSelector(
                    ChemicalPlantRecipes.INSTANCE,
                    be.getBlockPos(),
                    be.recipe,
                    0,
                    ItemBlueprints.grabPool(be.inventory.getStackInSlot(ChemPlantBlockEntity.BLUEPRINT_SLOT)),
                    this);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
