package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineAssemblyFactoryBlockEntity;
import com.hbm.inventory.container.machine.dummyable.MachineAssemblyFactoryMenu;
import com.hbm.inventory.gui.GUIScreenRecipeSelector;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.recipes.AssemblerRecipe;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.main.MainRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * CE {@code GUIMachineAssemblyFactory} — {@code gui_assembly_factory.png} 256×240 (atlas 256×256).
 * Recipe click {@code 6+(i%2)*109, 53+(i/2)*56} opens {@link GUIScreenRecipeSelector}
 * (CE {@code GUIMachineAssemblyFactory.java:62}). Ghost inputs
 * {@code GUIMachineAssemblyFactory.java:112-130}.
 */
public class MachineAssemblyFactoryScreen extends GuiInfoContainer<MachineAssemblyFactoryMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_assembly_factory.png");

    public MachineAssemblyFactoryScreen(MachineAssemblyFactoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 240;
        this.inventoryLabelX = 33;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 60;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, 256, 140);
        guiGraphics.blit(TEXTURE, x + 25, y + 140, 25, 140, 231, 100);
        MachineAssemblyFactoryBlockEntity be = this.getMenu().be;
        if (be.maxPower > 0) {
            int p = (int) (be.power * 92 / be.maxPower);
            if (p > 0) guiGraphics.blit(TEXTURE, x + 234, y + 110 - p, 0, 232 - p, 16, p);
        }
        if (this.minecraft != null && this.minecraft.level != null
                && AssemblyMachineRecipes.INSTANCE.recipeOrderedList.isEmpty()) {
            AssemblyMachineRecipes.rebuild(this.minecraft.level.getRecipeManager());
        }
        for (int i = 0; i < 4; i++) {
            GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.recipeNameMap.get(be.recipes[i]);
            AssemblerRecipe selected = AssemblyMachineRecipes.byName(
                    this.minecraft != null ? this.minecraft.level : null, be.recipes[i]);
            if (be.progress[i] > 0) {
                int j = (int) Math.ceil(37 * be.progress[i]);
                guiGraphics.blit(TEXTURE, x + 45 + (i % 2) * 109, y + 63 + (i / 2) * 56, 0, 240, j, 6);
            }
            if (be.didProcess[i]) {
                guiGraphics.blit(TEXTURE, x + 45 + (i % 2) * 109, y + 55 + (i / 2) * 56, 4, 236, 4, 4);
            } else if (recipe != null) {
                guiGraphics.blit(TEXTURE, x + 45 + (i % 2) * 109, y + 55 + (i / 2) * 56, 0, 236, 4, 4);
            }
            if (be.didProcess[i]) {
                guiGraphics.blit(TEXTURE, x + 53 + (i % 2) * 109, y + 55 + (i / 2) * 56, 4, 236, 4, 4);
            } else if (selected != null && be.power >= selected.getPower() && be.canCool()) {
                guiGraphics.blit(TEXTURE, x + 53 + (i % 2) * 109, y + 55 + (i / 2) * 56, 0, 236, 4, 4);
            }
            if (recipe != null) {
                guiGraphics.renderItem(recipe.getIcon(), x + 7 + (i % 2) * 109, y + 54 + (i / 2) * 56);
            }
            int start = MachineAssemblyFactoryBlockEntity.inputStart(i);
            renderGhostInputs(guiGraphics, TEXTURE, recipe, new int[]{
                    start, start + 1, start + 2, start + 3, start + 4, start + 5,
                    start + 6, start + 7, start + 8, start + 9, start + 10, start + 11
            });
        }
        for (int j = 0; j < 4; j++) {
            be.inputTanks[j].renderTank(x + 105 + (j % 2) * 109, y + 52 + (j / 2) * 56, 0, 5, 32);
            be.outputTanks[j].renderTank(x + 105 + (j % 2) * 109, y + 70 + (j / 2) * 56, 0, 5, 16);
        }
        be.water.renderTank(x + 232, y + 201, 0, 7, 52);
        be.lps.renderTank(x + 241, y + 201, 0, 7, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        MachineAssemblyFactoryBlockEntity be = this.getMenu().be;
        for (int j = 0; j < 4; j++) {
            be.inputTanks[j].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 105 + (j % 2) * 109, topPos + 20 + (j / 2) * 56, 3, 16);
            be.outputTanks[j].renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 105 + (j % 2) * 109, topPos + 54 + (j / 2) * 56, 3, 16);
        }
        be.water.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 232, topPos + 149, 7, 52);
        be.lps.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 241, topPos + 149, 7, 52);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 234, topPos + 18, 16, 92, be.power, be.maxPower);
        for (int i = 0; i < 4; i++) {
            if (!isHovered(mouseX, mouseY, 6 + (i % 2) * 109, 53 + (i / 2) * 56, 18, 18)) continue;
            GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.recipeNameMap.get(be.recipes[i]);
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
        MachineAssemblyFactoryBlockEntity be = this.getMenu().be;
        for (int i = 0; i < 4; i++) {
            if (!isHovered(mouseX, mouseY, 6 + (i % 2) * 109, 53 + (i / 2) * 56, 18, 18)) continue;
            click();
            if (Minecraft.getInstance().level != null) {
                AssemblyMachineRecipes.rebuild(Minecraft.getInstance().level.getRecipeManager());
            }
            GUIScreenRecipeSelector.openSelector(
                    AssemblyMachineRecipes.INSTANCE,
                    be.getBlockPos(),
                    be.recipes[i],
                    i,
                    ItemBlueprints.grabPool(be.inventory.getStackInSlot(MachineAssemblyFactoryBlockEntity.blueprintSlot(i))),
                    this);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
