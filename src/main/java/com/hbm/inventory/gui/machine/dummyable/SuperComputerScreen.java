package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineSuperComputerBlockEntity;
import com.hbm.inventory.container.machine.dummyable.SuperComputerMenu;
import com.hbm.inventory.gui.GUIScreenRecipeSelector;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.recipes.SuperComputerRecipes;
import com.hbm.inventory.recipes.SuperComputerRecipes.SuperComputerRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.main.MainRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * CE {@code GUIMachineSuperComputer} 176×211.
 * Selector {@code (7,80)} — {@code GuiInfoContainerProcessor.getSelectorPositions {{7,80,1}}}.
 */
public class SuperComputerScreen extends GuiInfoContainer<SuperComputerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_supercomputer.png");

    public SuperComputerScreen(SuperComputerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 211;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineSuperComputerBlockEntity be = this.getMenu().be;
        GenericRecipe recipe = SuperComputerRecipes.INSTANCE.recipeNameMap.get(be.recipe);
        SuperComputerRecipe selected = SuperComputerRecipes.byName(be.recipe);

        if (be.maxPower > 0) {
            int p = (int) (be.power * 61 / be.maxPower);
            if (p > 0) guiGraphics.blit(TEXTURE, x + 152, y + 79 - p, 176, 61 - p, 16, p);
        }
        if (selected != null && be.progress > 0) {
            int j = (int) Math.ceil(70D * be.progress / selected.duration);
            if (j > 0) guiGraphics.blit(TEXTURE, x + 62, y + 81, 176, 61, j, 16);
        }

        if (be.didProcess) {
            guiGraphics.blit(TEXTURE, x + 51, y + 76, 195, 0, 3, 6);
            guiGraphics.blit(TEXTURE, x + 56, y + 76, 195, 0, 3, 6);
        } else if (recipe != null) {
            guiGraphics.blit(TEXTURE, x + 51, y + 76, 192, 0, 3, 6);
            if (selected != null && be.power >= selected.power) {
                guiGraphics.blit(TEXTURE, x + 56, y + 76, 192, 0, 3, 6);
            }
        }

        if (recipe != null) {
            guiGraphics.renderItem(recipe.getIcon(), x + 8, y + 81);
        }
        renderGhostInputs(guiGraphics, TEXTURE, recipe, new int[]{2, 3, 4});

        be.input.renderTank(x + 8, y + 70, 0, 52, 16, 1);
        be.output.renderTank(x + 80, y + 70, 0, 52, 16, 1);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineSuperComputerBlockEntity be = this.getMenu().be;
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 54, 52, 16);
        be.output.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 80, topPos + 54, 52, 16);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 18, 16, 61, be.power, be.maxPower);
        if (isHovered(mouseX, mouseY, 7, 80, 18, 18)) {
            GenericRecipe recipe = SuperComputerRecipes.INSTANCE.recipeNameMap.get(be.recipe);
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
        if (isHovered(mouseX, mouseY, 7, 80, 18, 18)) {
            click();
            MachineSuperComputerBlockEntity be = this.getMenu().be;
            GUIScreenRecipeSelector.openSelector(
                    SuperComputerRecipes.INSTANCE,
                    be.getBlockPos(),
                    be.recipe,
                    0,
                    ItemBlueprints.grabPool(be.inventory.getStackInSlot(1)),
                    this);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
