package com.hbm.inventory.gui.machine;

import com.hbm.blockentity.machine.MachineAssemblyMachineBlockEntity;
import com.hbm.inventory.container.machine.MachineAssemblyMachineMenu;
import com.hbm.inventory.gui.GUIScreenRecipeSelector;
import com.hbm.inventory.gui.GuiInfoContainer;
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
 * Ported from CE's {@code GUIMachineAssemblyMachine} (canvas 176x256, from slot coordinates +
 * player inv at 174y). Battery at (152,81), blueprint at (35,126), upgrades at (152,108)/(170,108),
 * 12 input slots 4-col x 3-row grid from (8,18), output at (98,45), player inventory from (8,174).
 * <p>
 * Power bar at (8,89) 16x52 vertical fill (bottom-anchored). Progress bar at (73,54) horizontal
 * fill 0-24px, matching CE's assembly time indicator.
 * Selector {@code (7,125)} — CE {@code GUIMachineAssemblyMachine.getSelectorPositions}.
 */
public class MachineAssemblyMachineScreen extends GuiInfoContainer<MachineAssemblyMachineMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MainRegistry.MODID, "textures/gui/processing/gui_assembler.png");

    public MachineAssemblyMachineScreen(MachineAssemblyMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
        this.inventoryLabelY = this.imageHeight - 82;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Power bar - vertical fill, bottom-anchored at (8, 89) 16x52.
        int power = (int) (52L * this.getMenu().be.getPower() / Math.max(1, this.getMenu().be.getMaxPower()));
        if (power > 0) {
            guiGraphics.blit(TEXTURE, x + 8, y + 89 + (52 - power), 176, 52 - power, 16, power);
        }

        // Progress bar - horizontal fill at (73, 54) 0-24px.
        int progress = this.getMenu().be.getProgressScaled(24);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, x + 73, y + 54, 192, 0, progress, 18);
        }

        MachineAssemblyMachineBlockEntity be = this.getMenu().be;
        if (this.minecraft != null && this.minecraft.level != null
                && AssemblyMachineRecipes.INSTANCE.recipeOrderedList.isEmpty()) {
            AssemblyMachineRecipes.rebuild(this.minecraft.level.getRecipeManager());
        }
        GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.recipeNameMap.get(be.recipe);
        if (recipe != null) {
            guiGraphics.renderItem(recipe.getIcon(), x + 8, y + 126);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 89, 16, 52,
                this.getMenu().be.getPower(), this.getMenu().be.getMaxPower());
        if (isHovered(mouseX, mouseY, 7, 125, 18, 18)) {
            GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.recipeNameMap.get(this.getMenu().be.recipe);
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
            MachineAssemblyMachineBlockEntity be = this.getMenu().be;
            if (Minecraft.getInstance().level != null) {
                AssemblyMachineRecipes.rebuild(Minecraft.getInstance().level.getRecipeManager());
            }
            GUIScreenRecipeSelector.openSelector(
                    AssemblyMachineRecipes.INSTANCE,
                    be.getBlockPos(),
                    be.recipe,
                    0,
                    ItemBlueprints.grabPool(be.inventory.getStackInSlot(MachineAssemblyMachineBlockEntity.BLUEPRINT_SLOT)),
                    this);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
