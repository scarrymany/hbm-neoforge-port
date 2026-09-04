package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineChemicalFactoryBlockEntity;
import com.hbm.inventory.container.machine.dummyable.MachineChemicalFactoryMenu;
import com.hbm.inventory.gui.GUIScreenRecipeSelector;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.chem.ChemPlantRecipes.ChemPlantRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.main.MainRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * CE {@code GUIMachineChemicalFactory} — {@code gui_chemical_factory.png} 248×216 (atlas 256×256).
 * Recipe click {@code 74, 19+i*22} opens {@link GUIScreenRecipeSelector}
 * (CE {@code GUIMachineChemicalFactory.java:63}).
 */
public class MachineChemicalFactoryScreen extends GuiInfoContainer<MachineChemicalFactoryMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_chemical_factory.png");

    public MachineChemicalFactoryScreen(MachineChemicalFactoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 248;
        this.imageHeight = 216;
        this.inventoryLabelX = 26;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 50;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, 248, 116);
        guiGraphics.blit(TEXTURE, x + 18, y + 116, 18, 116, 230, 100);
        MachineChemicalFactoryBlockEntity be = this.getMenu().be;
        if (be.maxPower > 0) {
            int p = (int) (be.power * 68 / be.maxPower);
            if (p > 0) guiGraphics.blit(TEXTURE, x + 224, y + 86 - p, 0, 184 - p, 16, p);
        }
        for (int i = 0; i < 4; i++) {
            GenericRecipe recipe = ChemicalPlantRecipes.INSTANCE.recipeNameMap.get(be.recipes[i]);
            ChemPlantRecipe selected = ChemicalPlantRecipes.byName(be.recipes[i]);
            if (be.progress[i] > 0) {
                int j = (int) Math.ceil(22 * be.progress[i]);
                guiGraphics.blit(TEXTURE, x + 113, y + 29 + i * 22, 0, 216, j, 6);
            }
            if (be.didProcess[i]) {
                guiGraphics.blit(TEXTURE, x + 113, y + 21 + i * 22, 4, 222, 4, 4);
            } else if (recipe != null) {
                guiGraphics.blit(TEXTURE, x + 113, y + 21 + i * 22, 0, 222, 4, 4);
            }
            if (be.didProcess[i]) {
                guiGraphics.blit(TEXTURE, x + 121, y + 21 + i * 22, 4, 222, 4, 4);
            } else if (selected != null && be.power >= selected.power && be.canCool()) {
                guiGraphics.blit(TEXTURE, x + 121, y + 21 + i * 22, 0, 222, 4, 4);
            }
            if (recipe != null) {
                guiGraphics.renderItem(recipe.getIcon(), x + 75, y + 20 + i * 22);
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                be.inputTanks[i + j * 3].renderTank(x + 60 + i * 5, y + 36 + j * 22, 0, 3, 16);
                be.outputTanks[i + j * 3].renderTank(x + 189 + i * 5, y + 36 + j * 22, 0, 3, 16);
            }
        }
        be.water.renderTank(x + 224, y + 177, 0, 7, 52);
        be.lps.renderTank(x + 233, y + 177, 0, 7, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineChemicalFactoryBlockEntity be = this.getMenu().be;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                be.inputTanks[i + j * 3].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 60 + i * 5, topPos + 20 + j * 22, 3, 16);
                be.outputTanks[i + j * 3].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 189 + i * 5, topPos + 20 + j * 22, 3, 16);
            }
        }
        be.water.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 224, topPos + 125, 7, 52);
        be.lps.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 233, topPos + 125, 7, 52);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 224, topPos + 18, 16, 68, be.power, be.maxPower);
        for (int i = 0; i < 4; i++) {
            if (!isHovered(mouseX, mouseY, 74, 19 + i * 22, 18, 18)) continue;
            GenericRecipe recipe = ChemicalPlantRecipes.INSTANCE.recipeNameMap.get(be.recipes[i]);
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
        MachineChemicalFactoryBlockEntity be = this.getMenu().be;
        for (int i = 0; i < 4; i++) {
            if (!isHovered(mouseX, mouseY, 74, 19 + i * 22, 18, 18)) continue;
            click();
            GUIScreenRecipeSelector.openSelector(
                    ChemicalPlantRecipes.INSTANCE,
                    be.getBlockPos(),
                    be.recipes[i],
                    i,
                    ItemBlueprints.grabPool(be.inventory.getStackInSlot(MachineChemicalFactoryBlockEntity.blueprintSlot(i))),
                    this);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
