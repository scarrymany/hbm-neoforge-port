package com.hbm.inventory.gui.machine.chem;

import com.hbm.blockentity.machine.chem.ChemPlantBlockEntity;
import com.hbm.inventory.container.machine.chem.ChemPlantMenu;
import com.hbm.inventory.gui.GUIScreenRecipeSelector;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** CE {@code GUIMachineChemicalPlant} selector {@code (7,125)}. */
public class ChemPlantScreen extends GuiInfoContainer<ChemPlantMenu> {

    public ChemPlantScreen(ChemPlantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 216;
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
        for (int i = 0; i < 3; i++) {
            be.inputTanks[i].renderTank(x + 8 + i * 20, y + 140, 0, 16, 54);
            be.outputTanks[i].renderTank(x + 140 + i * 20, y + 140, 0, 16, 54);
        }
        if (ChemicalPlantRecipes.INSTANCE.recipeOrderedList.isEmpty()) {
            ChemicalPlantRecipes.rebuild();
        }
        GenericRecipe recipe = ChemicalPlantRecipes.INSTANCE.recipeNameMap.get(be.recipe);
        if (recipe != null) {
            guiGraphics.renderItem(recipe.getIcon(), x + 8, y + 126);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 6, 160, 12, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 20, 200, 10,
                Component.literal(be.isProcessing ? "Processing: " + be.getActiveRecipeName() : "Idle"),
                Component.literal("Progress: " + be.getProgressScaled(100) + "%"));
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
                    ItemBlueprints.grabPool(ItemStack.EMPTY),
                    this);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
